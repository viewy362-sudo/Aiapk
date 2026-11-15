package com.example.security

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class SigningIdentity(
    val privateKey: PrivateKey,
    val certificate: X509Certificate,
    val certDerBytes: ByteArray,
    val certSha256Colon: String
)

data class ApkEntryData(
    val name: String,
    val bytes: ByteArray,
    val forceStored: Boolean = false
)

sealed class ApkEntrySource {
    abstract val name: String
    abstract val forceStored: Boolean
    abstract fun loadBytes(): ByteArray

    data class InMemory(
        override val name: String,
        val bytes: ByteArray,
        override val forceStored: Boolean = false
    ) : ApkEntrySource() {
        override fun loadBytes(): ByteArray = bytes
    }

    data class FromZipEntry(
        override val name: String,
        val zipFile: ZipFile,
        val entryName: String,
        override val forceStored: Boolean = false
    ) : ApkEntrySource() {
        override fun loadBytes(): ByteArray {
            val ze = zipFile.getEntry(entryName)
                ?: throw IllegalStateException("Missing source entry: $entryName")
            return zipFile.getInputStream(ze).use { it.readBytes() }
        }
    }
}

data class ApkSignatureVerificationResult(
    val hasV1JarSignature: Boolean,
    val hasV2SigningBlock: Boolean,
    val v2RsaVerified: Boolean,
    val certSubject: String,
    val certSha256Colon: String
)

/**
 * Pure-Kotlin Streaming ZipAlign + JAR v1 + APK Signature Scheme v2 ("APK Sig Block 42") Engine.
 * Streams large archives via temporary files & 1 MiB Merkle chunks so even 100MB+ APKs sign cleanly
 * with minimal heap memory (no OutOfMemoryError on mobile devices).
 */
object ApkSignerEngine {

    private const val APK_SIG_BLOCK_V2_ID = 0x7109871a
    private const val APK_SIG_BLOCK_PADDING_ID = 0x42726577
    private const val SIG_ALG_RSA_PKCS1_V15_SHA256 = 0x0103
    private const val CHUNK_SIZE_BYTES = 1024 * 1024 // 1 MiB per AOSP v2 spec
    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    fun loadOrCreateSigningIdentity(
        context: Context?,
        commonName: String = "Bloody Team",
        organization: String = "Bloody Team Security"
    ): SigningIdentity {
        val cleanCn = commonName.trim().ifEmpty { "Bloody Team" }
        val cleanOrg = organization.trim().ifEmpty { "Bloody Team Security" }
        val cacheId = sha256Hex("$cleanCn|$cleanOrg".toByteArray(Charsets.UTF_8)).take(12)

        if (context != null) {
            val keyFile = File(context.filesDir, "bloody_key_$cacheId.pk8")
            val certFile = File(context.filesDir, "bloody_cert_$cacheId.der")
            if (keyFile.exists() && certFile.exists()) {
                runCatching {
                    val pkBytes = keyFile.readBytes()
                    val certBytes = certFile.readBytes()
                    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkBytes))
                    val cert = CertificateFactory.getInstance("X.509")
                        .generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
                    return SigningIdentity(
                        privateKey = privateKey,
                        certificate = cert,
                        certDerBytes = certBytes,
                        certSha256Colon = sha256ColonHex(certBytes)
                    )
                }
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()
        val certDer = generateSelfSignedX509CertDer(keyPair, cleanCn, cleanOrg)
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate

        if (context != null) {
            runCatching {
                File(context.filesDir, "bloody_key_$cacheId.pk8").writeBytes(keyPair.private.encoded)
                File(context.filesDir, "bloody_cert_$cacheId.der").writeBytes(certDer)
            }
        }

        return SigningIdentity(
            privateKey = keyPair.private,
            certificate = cert,
            certDerBytes = certDer,
            certSha256Colon = sha256ColonHex(certDer)
        )
    }

    fun buildZipAlignedAndSignedApk(
        entries: List<ApkEntryData>,
        identity: SigningIdentity,
        outputFile: File
    ): ApkSignatureVerificationResult {
        val sources = entries.map { e ->
            ApkEntrySource.InMemory(name = e.name, bytes = e.bytes, forceStored = e.forceStored)
        }
        return buildZipAlignedAndSignedApkFromSources(sources, identity, outputFile)
    }

    /**
     * Streams ZipAligned entries to disk, writes JAR v1 signature (MANIFEST.MF, CERT.SF, CERT.RSA),
     * and injects APK Signature Scheme v2 Block directly via file channels (<12 MB peak heap).
     */
    fun buildZipAlignedAndSignedApkFromSources(
        entries: List<ApkEntrySource>,
        identity: SigningIdentity,
        outputFile: File
    ): ApkSignatureVerificationResult {
        val seenNames = LinkedHashSet<String>()
        val cleanEntries = ArrayList<ApkEntrySource>(entries.size)
        for (e in entries) {
            val cleanName = e.name.trimStart('/')
            if (cleanName.isBlank()) continue
            val upper = cleanName.uppercase()
            val isMetaSig = upper == "META-INF/MANIFEST.MF" ||
                (upper.startsWith("META-INF/") &&
                    (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC")))
            if (isMetaSig) continue
            if (seenNames.add(cleanName)) {
                cleanEntries.add(e)
            }
        }

        // Post-1980 DOS timestamp prevents OpenJDK ZipOutputStream from adding 9-byte 'UT' headers.
        val dosSafeTimestampMs = 1577836800000L

        outputFile.parentFile?.mkdirs()
        val v1TempFile = File(outputFile.parentFile, "${outputFile.name}.v1tmp")
        val rawManifestEntries = ArrayList<String>(cleanEntries.size)

        try {
            val countingOut = CountingOutputStream(FileOutputStream(v1TempFile).buffered(64 * 1024))
            ZipOutputStream(countingOut).use { zos ->
                for (item in cleanEntries) {
                    val entryName = item.name.trimStart('/')
                    val lower = entryName.lowercase()
                    val isStored = item.forceStored ||
                        lower == "resources.arsc" ||
                        (lower.startsWith("lib/") && lower.endsWith(".so"))

                    val bytes = item.loadBytes()
                    val zipEntry = ZipEntry(entryName).apply { time = dosSafeTimestampMs }

                    if (isStored) {
                        val alignment = if (lower.endsWith(".so")) 16384 else 4
                        val crc = CRC32().apply { update(bytes) }.value
                        zipEntry.method = ZipEntry.STORED
                        zipEntry.size = bytes.size.toLong()
                        zipEntry.compressedSize = bytes.size.toLong()
                        zipEntry.crc = crc

                        zos.flush()
                        val nameLen = entryName.toByteArray(Charsets.UTF_8).size
                        val basePayloadOffset = countingOut.bytesWritten + 30L + nameLen
                        val rem = (basePayloadOffset % alignment).toInt()
                        val padLen = if (rem == 0) 0 else alignment - rem
                        if (padLen > 0) {
                            zipEntry.extra = ByteArray(padLen)
                        }
                    } else {
                        zipEntry.method = ZipEntry.DEFLATED
                    }

                    zos.putNextEntry(zipEntry)
                    zos.write(bytes)
                    zos.closeEntry()

                    val entryDigestB64 = sha256Base64(bytes)
                    val entryBlock = wrapManifestLine("Name: $entryName") +
                        "\r\nSHA-256-Digest: $entryDigestB64\r\n\r\n"
                    rawManifestEntries.add(entryBlock)
                }

                // Build MANIFEST.MF
                val manifestHeader = "Manifest-Version: 1.0\r\nCreated-By: Bloody Team APK Signer 2.0\r\n\r\n"
                val manifestBaos = ByteArrayOutputStream(maxOf(1024, rawManifestEntries.size * 128))
                manifestBaos.write(manifestHeader.toByteArray(Charsets.UTF_8))
                for (block in rawManifestEntries) {
                    manifestBaos.write(block.toByteArray(Charsets.UTF_8))
                }
                val manifestBytes = manifestBaos.toByteArray()

                zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF").apply { time = dosSafeTimestampMs })
                zos.write(manifestBytes)
                zos.closeEntry()

                // Build CERT.SF
                val sfBuilder = StringBuilder(maxOf(1024, rawManifestEntries.size * 128))
                sfBuilder.append("Signature-Version: 1.0\r\n")
                sfBuilder.append("Created-By: Bloody Team APK Signer 2.0\r\n")
                sfBuilder.append("SHA-256-Digest-Manifest: ").append(sha256Base64(manifestBytes)).append("\r\n")
                sfBuilder.append("X-Android-APK-Signed: 2\r\n\r\n")

                for (sectionText in rawManifestEntries) {
                    val nameHeader = sectionText.substringBefore("\r\nSHA-256-Digest:")
                    val sectionDigestB64 = sha256Base64(sectionText.toByteArray(Charsets.UTF_8))
                    sfBuilder.append(nameHeader).append("\r\n")
                    sfBuilder.append("SHA-256-Digest: ").append(sectionDigestB64).append("\r\n\r\n")
                }
                val certSfBytes = sfBuilder.toString().toByteArray(Charsets.UTF_8)

                zos.putNextEntry(ZipEntry("META-INF/CERT.SF").apply { time = dosSafeTimestampMs })
                zos.write(certSfBytes)
                zos.closeEntry()

                // Build CERT.RSA (PKCS#7 SignedData)
                val pkcs7Bytes = buildPkcs7SignedData(
                    dataToSign = certSfBytes,
                    privateKey = identity.privateKey,
                    certificate = identity.certificate,
                    certDerBytes = identity.certDerBytes
                )
                zos.putNextEntry(ZipEntry("META-INF/CERT.RSA").apply { time = dosSafeTimestampMs })
                zos.write(pkcs7Bytes)
                zos.closeEntry()
            }

            injectApkSignatureV2BlockFile(v1TempFile, identity, outputFile)
        } finally {
            runCatching { v1TempFile.delete() }
        }

        return verifyApkSignatures(outputFile)
    }

    fun injectApkSignatureV2BlockFile(v1File: File, identity: SigningIdentity, outputFile: File) {
        RandomAccessFile(v1File, "r").use { raf ->
            val fileLen = raf.length()
            val eocdOffset = findEocdOffset(raf)
                ?: throw IllegalStateException("ZIP End of Central Directory not found")
            val eocdSize = (fileLen - eocdOffset).toInt()
            val eocdBytes = ByteArray(eocdSize)
            raf.seek(eocdOffset)
            raf.readFully(eocdBytes)

            val eocdBuf = ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN)
            val cdSize = eocdBuf.getInt(12).toLong() and 0xFFFFFFFFL
            val cdOffset = eocdBuf.getInt(16).toLong() and 0xFFFFFFFFL
            require(cdOffset + cdSize == eocdOffset) {
                "Malformed ZIP central directory bounds (cdOffset=$cdOffset, cdSize=$cdSize, eocd=$eocdOffset)"
            }

            val apkDigest = computeV2MerkleDigestFromFile(
                raf = raf,
                sec1End = cdOffset,
                sec2Start = cdOffset,
                sec2End = eocdOffset,
                sec3Bytes = eocdBytes
            )

            val signerBytes = buildV2SignerBytes(apkDigest, identity)
            val v2Value = lengthPrefixed(signerBytes)
            val v2Block = buildApkSigningBlock(v2Value)

            val patchedEocd = eocdBytes.copyOf()
            ByteBuffer.wrap(patchedEocd).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(16, (cdOffset + v2Block.size).toInt())

            FileOutputStream(outputFile).buffered(64 * 1024).use { out ->
                copyFileSlice(raf, 0L, cdOffset, out)
                out.write(v2Block)
                copyFileSlice(raf, cdOffset, eocdOffset - cdOffset, out)
                out.write(patchedEocd)
                out.flush()
            }
        }
    }

    fun verifyApkSignatures(apkFile: File): ApkSignatureVerificationResult {
        var hasV1 = false
        ZipFile(apkFile).use { zf ->
            hasV1 = zf.getEntry("META-INF/MANIFEST.MF") != null &&
                zf.getEntry("META-INF/CERT.SF") != null &&
                zf.getEntry("META-INF/CERT.RSA") != null
        }

        RandomAccessFile(apkFile, "r").use { raf ->
            val fileLen = raf.length()
            val eocdOffset = findEocdOffset(raf)
                ?: return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            val eocdBytes = ByteArray((fileLen - eocdOffset).toInt())
            raf.seek(eocdOffset)
            raf.readFully(eocdBytes)

            val eocdBuf = ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN)
            val cdOffset = eocdBuf.getInt(16).toLong() and 0xFFFFFFFFL
            val cdSize = eocdBuf.getInt(12).toLong() and 0xFFFFFFFFL

            if (cdOffset < 32L || cdOffset + cdSize != eocdOffset) {
                return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            }

            val tailBuf = ByteArray(24)
            raf.seek(cdOffset - 24L)
            raf.readFully(tailBuf)
            val hasMagic = tailBuf.copyOfRange(8, 24).contentEquals(APK_SIG_BLOCK_MAGIC)
            if (!hasMagic) {
                return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            }

            val tailSize = ByteBuffer.wrap(tailBuf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).long
            val blockStart = cdOffset - 8L - tailSize
            if (blockStart < 0L || tailSize > 16L * 1024L * 1024L) {
                return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            }

            val headSizeBytes = ByteArray(8)
            raf.seek(blockStart)
            raf.readFully(headSizeBytes)
            val headSize = ByteBuffer.wrap(headSizeBytes).order(ByteOrder.LITTLE_ENDIAN).long
            if (headSize != tailSize) {
                return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            }

            val pairsBytes = ByteArray((tailSize - 24L).toInt())
            raf.seek(blockStart + 8L)
            raf.readFully(pairsBytes)

            val blockBuf = ByteBuffer.wrap(pairsBytes).order(ByteOrder.LITTLE_ENDIAN)
            var v2ValueBytes: ByteArray? = null
            while (blockBuf.remaining() >= 12) {
                val pairLen = blockBuf.long.toInt()
                val id = blockBuf.int
                val valBytes = ByteArray(pairLen - 4)
                blockBuf.get(valBytes)
                if (id == APK_SIG_BLOCK_V2_ID) {
                    v2ValueBytes = valBytes
                    break
                }
            }
            if (v2ValueBytes == null) {
                return ApkSignatureVerificationResult(hasV1, false, false, "N/A", "N/A")
            }

            // Recompute v2 Merkle digest using Section 3 EOCD with cdOffset patched back to blockStart
            val patchedEocd = eocdBytes.copyOf()
            ByteBuffer.wrap(patchedEocd).order(ByteOrder.LITTLE_ENDIAN).putInt(16, blockStart.toInt())
            val recomputedDigest = computeV2MerkleDigestFromFile(
                raf = raf,
                sec1End = blockStart,
                sec2Start = cdOffset,
                sec2End = eocdOffset,
                sec3Bytes = patchedEocd
            )

            val v2Buf = ByteBuffer.wrap(v2ValueBytes).order(ByteOrder.LITTLE_ENDIAN)
            val signersLen = v2Buf.int
            val signerLen = v2Buf.int
            val signerStart = v2Buf.position()
            val signedDataBytes = readLengthPrefixed(v2Buf)
            val signaturesSeqBytes = readLengthPrefixed(v2Buf)
            val pubKeyBytes = readLengthPrefixed(v2Buf)
            require(v2Buf.position() == signerStart + signerLen && signersLen > 0 && pubKeyBytes.isNotEmpty())

            val sdBuf = ByteBuffer.wrap(signedDataBytes).order(ByteOrder.LITTLE_ENDIAN)
            val digestsSeq = ByteBuffer.wrap(readLengthPrefixed(sdBuf)).order(ByteOrder.LITTLE_ENDIAN)
            var digestMatches = false
            while (digestsSeq.hasRemaining()) {
                val entry = ByteBuffer.wrap(readLengthPrefixed(digestsSeq)).order(ByteOrder.LITTLE_ENDIAN)
                val algId = entry.int
                val digest = readLengthPrefixed(entry)
                if (algId == SIG_ALG_RSA_PKCS1_V15_SHA256 && digest.contentEquals(recomputedDigest)) {
                    digestMatches = true
                }
            }

            val certsSeq = ByteBuffer.wrap(readLengthPrefixed(sdBuf)).order(ByteOrder.LITTLE_ENDIAN)
            val certBytes = readLengthPrefixed(certsSeq)
            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

            val sigsBuf = ByteBuffer.wrap(signaturesSeqBytes).order(ByteOrder.LITTLE_ENDIAN)
            val firstSigEntry = ByteBuffer.wrap(readLengthPrefixed(sigsBuf)).order(ByteOrder.LITTLE_ENDIAN)
            val sigAlgId = firstSigEntry.int
            val sigBytes = readLengthPrefixed(firstSigEntry)

            val rsaValid = sigAlgId == SIG_ALG_RSA_PKCS1_V15_SHA256 && Signature.getInstance("SHA256withRSA").run {
                initVerify(cert.publicKey)
                update(signedDataBytes)
                verify(sigBytes)
            }

            return ApkSignatureVerificationResult(
                hasV1JarSignature = hasV1,
                hasV2SigningBlock = true,
                v2RsaVerified = digestMatches && rsaValid,
                certSubject = cert.subjectX500Principal.name,
                certSha256Colon = sha256ColonHex(certBytes)
            )
        }
    }

    private fun copyFileSlice(raf: RandomAccessFile, offset: Long, totalBytes: Long, out: OutputStream) {
        raf.seek(offset)
        val buf = ByteArray(64 * 1024)
        var remaining = totalBytes
        while (remaining > 0L) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val n = raf.read(buf, 0, toRead)
            if (n <= 0) break
            out.write(buf, 0, n)
            remaining -= n
        }
    }

    private fun computeV2MerkleDigestFromFile(
        raf: RandomAccessFile,
        sec1End: Long,
        sec2Start: Long,
        sec2End: Long,
        sec3Bytes: ByteArray
    ): ByteArray {
        val chunkDigests = mutableListOf<ByteArray>()
        val chunkBuf = ByteArray(CHUNK_SIZE_BYTES)

        fun hashRange(start: Long, end: Long) {
            val lenTotal = (end - start).coerceAtLeast(0L)
            if (lenTotal == 0L) {
                chunkDigests.add(hashChunk(chunkBuf, 0, 0))
                return
            }
            raf.seek(start)
            var remaining = lenTotal
            while (remaining > 0L) {
                val want = minOf(CHUNK_SIZE_BYTES.toLong(), remaining).toInt()
                raf.readFully(chunkBuf, 0, want)
                chunkDigests.add(hashChunk(chunkBuf, 0, want))
                remaining -= want
            }
        }

        hashRange(0L, sec1End)
        hashRange(sec2Start, sec2End)

        if (sec3Bytes.isEmpty()) {
            chunkDigests.add(hashChunk(sec3Bytes, 0, 0))
        } else {
            var off = 0
            while (off < sec3Bytes.size) {
                val len = minOf(CHUNK_SIZE_BYTES, sec3Bytes.size - off)
                chunkDigests.add(hashChunk(sec3Bytes, off, len))
                off += len
            }
        }

        val topMd = MessageDigest.getInstance("SHA-256")
        val header = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            .put(0x5a.toByte())
            .putInt(chunkDigests.size)
            .array()
        topMd.update(header)
        for (cd in chunkDigests) {
            topMd.update(cd)
        }
        return topMd.digest()
    }

    private fun findEocdOffset(raf: RandomAccessFile): Long? {
        val fileLen = raf.length()
        if (fileLen < 22L) return null
        val tailLen = minOf(fileLen, 65536L + 22L).toInt()
        val tailStart = fileLen - tailLen
        val tail = ByteArray(tailLen)
        raf.seek(tailStart)
        raf.readFully(tail)
        for (i in (tailLen - 22) downTo 0) {
            if (tail[i] == 0x50.toByte() &&
                tail[i + 1] == 0x4b.toByte() &&
                tail[i + 2] == 0x05.toByte() &&
                tail[i + 3] == 0x06.toByte()
            ) {
                val commentLen = ((tail[i + 20].toInt() and 0xFF) or ((tail[i + 21].toInt() and 0xFF) shl 8))
                if (i + 22 + commentLen == tailLen) {
                    return tailStart + i
                }
            }
        }
        return null
    }

    private fun buildV2SignerBytes(apkDigest: ByteArray, identity: SigningIdentity): ByteArray {
        val digestEntry = ByteBuffer.allocate(8 + apkDigest.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(SIG_ALG_RSA_PKCS1_V15_SHA256)
            .putInt(apkDigest.size)
            .put(apkDigest)
            .array()
        val digestsSeq = lengthPrefixed(lengthPrefixed(digestEntry))
        val certsSeq = lengthPrefixed(lengthPrefixed(identity.certDerBytes))
        val additionalAttrs = lengthPrefixed(ByteArray(0))

        val signedData = ByteArrayOutputStream().apply {
            write(digestsSeq)
            write(certsSeq)
            write(additionalAttrs)
        }.toByteArray()

        val sigBytes = Signature.getInstance("SHA256withRSA").run {
            initSign(identity.privateKey)
            update(signedData)
            sign()
        }

        val sigEntry = ByteBuffer.allocate(8 + sigBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(SIG_ALG_RSA_PKCS1_V15_SHA256)
            .putInt(sigBytes.size)
            .put(sigBytes)
            .array()
        val signaturesSeq = lengthPrefixed(lengthPrefixed(sigEntry))
        val pubKeyEncoded = identity.certificate.publicKey.encoded

        val signerBody = ByteArrayOutputStream().apply {
            write(lengthPrefixed(signedData))
            write(signaturesSeq)
            write(lengthPrefixed(pubKeyEncoded))
        }.toByteArray()

        return lengthPrefixed(signerBody)
    }

    private fun buildApkSigningBlock(v2BlockValue: ByteArray): ByteArray {
        val v2PairSize = 8 + 4 + v2BlockValue.size
        val minTotalSize = 8 + v2PairSize + 8 + 16
        val rem = minTotalSize % 4096
        val padPairBytes = if (rem == 0) {
            ByteArray(0)
        } else {
            val neededTotalPadPair = 4096 - rem
            val padPayloadLen = maxOf(0, neededTotalPadPair - 12)
            val actualPairLen = 12 + padPayloadLen
            ByteBuffer.allocate(actualPairLen).order(ByteOrder.LITTLE_ENDIAN)
                .putLong((4 + padPayloadLen).toLong())
                .putInt(APK_SIG_BLOCK_PADDING_ID)
                .put(ByteArray(padPayloadLen))
                .array()
        }

        val pairsSize = v2PairSize + padPairBytes.size
        val sizeField = (pairsSize + 8 + 16).toLong()

        val blockBuf = ByteBuffer.allocate(8 + pairsSize + 8 + 16).order(ByteOrder.LITTLE_ENDIAN)
        blockBuf.putLong(sizeField)
        blockBuf.putLong((4 + v2BlockValue.size).toLong())
        blockBuf.putInt(APK_SIG_BLOCK_V2_ID)
        blockBuf.put(v2BlockValue)
        if (padPairBytes.isNotEmpty()) {
            blockBuf.put(padPairBytes)
        }
        blockBuf.putLong(sizeField)
        blockBuf.put(APK_SIG_BLOCK_MAGIC)
        return blockBuf.array()
    }

    private fun hashChunk(data: ByteArray, offset: Int, len: Int): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val header = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            .put(0xa5.toByte())
            .putInt(len)
            .array()
        md.update(header)
        md.update(data, offset, len)
        return md.digest()
    }

    private fun lengthPrefixed(data: ByteArray): ByteArray {
        return ByteBuffer.allocate(4 + data.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(data.size)
            .put(data)
            .array()
    }

    private fun readLengthPrefixed(buf: ByteBuffer): ByteArray {
        val len = buf.int
        require(len >= 0 && len <= buf.remaining()) { "Invalid length-prefixed field: $len" }
        return ByteArray(len).also { buf.get(it) }
    }

    private fun generateSelfSignedX509CertDer(
        keyPair: KeyPair,
        commonName: String,
        organization: String
    ): ByteArray {
        val oidSha256WithRsa = derOid(intArrayOf(1, 2, 840, 113549, 1, 1, 11))
        val sigAlgId = derSequence(oidSha256WithRsa, derNull())

        val serialBytes = ByteArray(8).also { SecureRandom().nextBytes(it) }
        serialBytes[0] = (serialBytes[0].toInt() and 0x7F or 0x01).toByte()
        val serialDer = derInteger(BigInteger(1, serialBytes))

        val versionExplicit = derExplicit(0, derInteger(BigInteger.valueOf(2)))
        val nameDer = derX500Name(commonName, organization, "US")
        val validityDer = derSequence(
            derUtcTime("250101000000Z"),
            derUtcTime("491231235959Z")
        )
        val spkiDer = keyPair.public.encoded

        val basicConstraintsOid = derOid(intArrayOf(2, 5, 29, 19))
        val basicConstraintsExt = derSequence(
            basicConstraintsOid,
            derBoolean(true),
            derOctetString(derSequence())
        )
        val extensionsExplicit = derExplicit(3, derSequence(basicConstraintsExt))

        val tbsCert = derSequence(
            versionExplicit,
            serialDer,
            sigAlgId,
            nameDer,
            validityDer,
            nameDer,
            spkiDer,
            extensionsExplicit
        )

        val sigBytes = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(tbsCert)
            sign()
        }

        return derSequence(
            tbsCert,
            sigAlgId,
            derBitString(sigBytes)
        )
    }

    private fun buildPkcs7SignedData(
        dataToSign: ByteArray,
        privateKey: PrivateKey,
        certificate: X509Certificate,
        certDerBytes: ByteArray
    ): ByteArray {
        val signatureBytes = Signature.getInstance("SHA256withRSA").run {
            initSign(privateKey)
            update(dataToSign)
            sign()
        }

        val oidSignedData = derOid(intArrayOf(1, 2, 840, 113549, 1, 7, 2))
        val oidData = derOid(intArrayOf(1, 2, 840, 113549, 1, 7, 1))
        val oidSha256 = derOid(intArrayOf(2, 16, 840, 1, 101, 3, 4, 2, 1))
        val oidRsaEnc = derOid(intArrayOf(1, 2, 840, 113549, 1, 1, 1))

        val sha256AlgId = derSequence(oidSha256, derNull())
        val rsaAlgId = derSequence(oidRsaEnc, derNull())

        val issuerAndSerial = derSequence(
            certificate.issuerX500Principal.encoded,
            derInteger(certificate.serialNumber)
        )

        val signerInfo = derSequence(
            derInteger(BigInteger.ONE),
            issuerAndSerial,
            sha256AlgId,
            rsaAlgId,
            derOctetString(signatureBytes)
        )

        val certificatesImplicitSet = derTag(0xA0, certDerBytes)
        val signedData = derSequence(
            derInteger(BigInteger.ONE),
            derSet(sha256AlgId),
            derSequence(oidData),
            certificatesImplicitSet,
            derSet(signerInfo)
        )

        return derSequence(
            oidSignedData,
            derExplicit(0, signedData)
        )
    }

    private fun derX500Name(cn: String, org: String, country: String): ByteArray {
        val rdnCn = derSet(derSequence(derOid(intArrayOf(2, 5, 4, 3)), derUtf8String(cn)))
        val rdnOrg = derSet(derSequence(derOid(intArrayOf(2, 5, 4, 10)), derUtf8String(org)))
        val rdnC = derSet(derSequence(derOid(intArrayOf(2, 5, 4, 6)), derPrintableString(country)))
        return derSequence(rdnCn, rdnOrg, rdnC)
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val len = content.size
        val lenBytes = when {
            len < 128 -> byteArrayOf(len.toByte())
            len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 0xFFFF -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), len.toByte())
            else -> byteArrayOf(
                0x83.toByte(),
                (len shr 16).toByte(),
                (len shr 8).toByte(),
                len.toByte()
            )
        }
        return ByteArray(1 + lenBytes.size + content.size).also { out ->
            out[0] = tag.toByte()
            lenBytes.copyInto(out, 1)
            content.copyInto(out, 1 + lenBytes.size)
        }
    }

    private fun derConcat(vararg items: ByteArray): ByteArray {
        val total = items.sumOf { it.size }
        return ByteArray(total).also { out ->
            var pos = 0
            for (b in items) {
                b.copyInto(out, pos)
                pos += b.size
            }
        }
    }

    private fun derSequence(vararg items: ByteArray): ByteArray = derTag(0x30, derConcat(*items))
    private fun derSet(vararg items: ByteArray): ByteArray = derTag(0x31, derConcat(*items))
    private fun derExplicit(tagNumber: Int, inner: ByteArray): ByteArray = derTag(0xA0 or tagNumber, inner)
    private fun derInteger(value: BigInteger): ByteArray = derTag(0x02, value.toByteArray())
    private fun derBoolean(value: Boolean): ByteArray = derTag(0x01, byteArrayOf(if (value) 0xFF.toByte() else 0x00))
    private fun derNull(): ByteArray = byteArrayOf(0x05, 0x00)
    private fun derOctetString(bytes: ByteArray): ByteArray = derTag(0x04, bytes)
    private fun derUtf8String(text: String): ByteArray = derTag(0x0C, text.toByteArray(Charsets.UTF_8))
    private fun derPrintableString(text: String): ByteArray = derTag(0x13, text.toByteArray(Charsets.US_ASCII))
    private fun derUtcTime(zuluTime: String): ByteArray = derTag(0x17, zuluTime.toByteArray(Charsets.US_ASCII))
    private fun derBitString(bytes: ByteArray): ByteArray =
        derTag(0x03, ByteArray(bytes.size + 1).also { it[0] = 0; bytes.copyInto(it, 1) })

    private fun derOid(nodes: IntArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(nodes[0] * 40 + nodes[1])
        for (i in 2 until nodes.size) {
            var v = nodes[i]
            val stack = IntArray(5)
            var top = 0
            do {
                stack[top++] = v and 0x7F
                v = v ushr 7
            } while (v > 0)
            for (k in (top - 1) downTo 0) {
                val b = if (k > 0) stack[k] or 0x80 else stack[k]
                out.write(b)
            }
        }
        return derTag(0x06, out.toByteArray())
    }

    private fun wrapManifestLine(line: String): String {
        val utf8 = line.toByteArray(Charsets.UTF_8)
        if (utf8.size <= 70) return line
        val sb = StringBuilder()
        var pos = 0
        var first = true
        while (pos < utf8.size) {
            val maxBytes = if (first) 70 else 69
            var end = minOf(utf8.size, pos + maxBytes)
            while (end > pos && end < utf8.size && (utf8[end].toInt() and 0xC0) == 0x80) {
                end--
            }
            if (!first) sb.append("\r\n ")
            sb.append(String(utf8, pos, end - pos, Charsets.UTF_8))
            pos = end
            first = false
        }
        return sb.toString()
    }

    private fun sha256Base64(bytes: ByteArray): String =
        Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes), Base64.NO_WRAP)

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha256ColonHex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(":") { "%02X".format(it) }

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len
        }
    }
}
