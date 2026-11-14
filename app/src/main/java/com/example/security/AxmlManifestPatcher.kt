package com.example.security

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AxmlManifestInfo(
    val packageName: String,
    val originalApplicationClass: String,
    val versionName: String,
    val isValidBinaryAxml: Boolean
)

/**
 * Pure-Kotlin Android Binary XML (AXML) Inspector & Manifest Patcher.
 * Reads packageName + original Application class from compiled AndroidManifest.xml
 * and patches <application android:name="..."> to launch BloodyStubApplication first.
 */
object AxmlManifestPatcher {

    private const val CHUNK_AXML_FILE = 0x00080003
    private const val TYPE_STRING_POOL = 0x0001
    private const val TYPE_RESOURCE_MAP = 0x0180
    private const val TYPE_START_NAMESPACE = 0x0100
    private const val TYPE_END_NAMESPACE = 0x0101
    private const val TYPE_START_ELEMENT = 0x0102
    private const val TYPE_END_ELEMENT = 0x0103
    private const val TYPE_CDATA = 0x0104

    private const val ATTR_ANDROID_NAME = 0x01010003
    private const val ATTR_ANDROID_VERSION_NAME = 0x0101021c
    private const val ATTR_APP_COMPONENT_FACTORY = 0x0101057a
    private const val ANDROID_NS_URI = "http://schemas.android.com/apk/res/android"
    private const val TYPE_STRING_VALUE = 0x03
    const val STUB_APPLICATION_CLASS = "com.bloodyteam.stub.BloodyStubApplication"

    fun inspectBinaryManifest(axmlBytes: ByteArray): AxmlManifestInfo {
        if (axmlBytes.size < 36) {
            return AxmlManifestInfo("", "", "", false)
        }
        val buf = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.getInt(0) != CHUNK_AXML_FILE) {
            return AxmlManifestInfo("", "", "", false)
        }

        val parsed = runCatching { parseAxml(axmlBytes) }.getOrNull()
            ?: return AxmlManifestInfo("", "", "", false)

        var pkg = ""
        var origApp = ""
        var verName = ""

        for (node in parsed.nodes) {
            if (node.type == TYPE_START_ELEMENT) {
                val elemName = parsed.strings.getOrNull(node.elemNameIdx).orEmpty()
                if (elemName == "manifest") {
                    for (attr in node.attributes) {
                        val aName = parsed.strings.getOrNull(attr.nameIdx).orEmpty()
                        val resId = parsed.resourceIds.getOrNull(attr.nameIdx) ?: 0
                        if (aName == "package" && resId == 0 && attr.dataType == TYPE_STRING_VALUE) {
                            pkg = parsed.strings.getOrNull(attr.data).orEmpty()
                        } else if (resId == ATTR_ANDROID_VERSION_NAME && attr.dataType == TYPE_STRING_VALUE) {
                            verName = parsed.strings.getOrNull(attr.data).orEmpty()
                        }
                    }
                } else if (elemName == "application") {
                    for (attr in node.attributes) {
                        val resId = parsed.resourceIds.getOrNull(attr.nameIdx) ?: 0
                        if (resId == ATTR_ANDROID_NAME && attr.dataType == TYPE_STRING_VALUE) {
                            val raw = parsed.strings.getOrNull(attr.data).orEmpty()
                            origApp = resolveClassName(pkg, raw)
                        }
                    }
                }
            }
        }

        return AxmlManifestInfo(
            packageName = pkg,
            originalApplicationClass = origApp,
            versionName = verName,
            isValidBinaryAxml = true
        )
    }

    data class PatchedManifestResult(
        val patchedBytes: ByteArray,
        val info: AxmlManifestInfo
    )

    fun patchApplicationStub(
        axmlBytes: ByteArray,
        stubClassName: String = STUB_APPLICATION_CLASS
    ): PatchedManifestResult {
        val info = inspectBinaryManifest(axmlBytes)
        if (!info.isValidBinaryAxml) {
            return PatchedManifestResult(axmlBytes, info)
        }

        val parsed = parseAxml(axmlBytes)
        val strings = parsed.strings.toMutableList()
        val resIds = parsed.resourceIds.toMutableList()
        val nodes = parsed.nodes.toMutableList()

        // Ensure "name" with ATTR_ANDROID_NAME exists inside ResourceMap portion [0 .. resIds.size)
        var nameAttrIdx = resIds.indexOf(ATTR_ANDROID_NAME)
        if (nameAttrIdx < 0) {
            val insertAt = resIds.size
            strings.add(insertAt, "name")
            resIds.add(ATTR_ANDROID_NAME)
            shiftNodeStringIndices(nodes, insertAt)
            nameAttrIdx = insertAt
        }

        // Ensure android namespace URI string exists
        var androidNsIdx = strings.indexOf(ANDROID_NS_URI)
        if (androidNsIdx < 0) {
            androidNsIdx = strings.size
            strings.add(ANDROID_NS_URI)
        }

        // Append stub application class name
        val stubStrIdx = strings.indexOf(stubClassName).let { existing ->
            if (existing >= 0) existing else {
                strings.add(stubClassName)
                strings.size - 1
            }
        }

        // Append fallback framework AppComponentFactory class name if needed
        var appFactoryFallbackIdx = -1

        // Patch <application> element attributes
        for (node in nodes) {
            if (node.type == TYPE_START_ELEMENT && strings.getOrNull(node.elemNameIdx) == "application") {
                val nameAttr = node.attributes.firstOrNull { resIds.getOrNull(it.nameIdx) == ATTR_ANDROID_NAME }
                if (nameAttr != null) {
                    nameAttr.rawValueIdx = stubStrIdx
                    nameAttr.dataType = TYPE_STRING_VALUE
                    nameAttr.data = stubStrIdx
                } else {
                    val newAttr = AxmlAttr(
                        nsIdx = androidNsIdx,
                        nameIdx = nameAttrIdx,
                        rawValueIdx = stubStrIdx,
                        typedSize = 8,
                        res0 = 0,
                        dataType = TYPE_STRING_VALUE,
                        data = stubStrIdx
                    )
                    node.attributes.add(newAttr)
                    node.attributes.sortWith(compareBy { a ->
                        val id = resIds.getOrNull(a.nameIdx) ?: Int.MAX_VALUE
                        if (id == 0) Int.MAX_VALUE else id
                    })
                }

                // Neutralize custom AppComponentFactory (e.g. CoreComponentFactory) since original classes.dex is packed
                val factoryAttr = node.attributes.firstOrNull { resIds.getOrNull(it.nameIdx) == ATTR_APP_COMPONENT_FACTORY }
                if (factoryAttr != null) {
                    if (appFactoryFallbackIdx < 0) {
                        appFactoryFallbackIdx = strings.indexOf("android.app.AppComponentFactory").let { idx ->
                            if (idx >= 0) idx else {
                                strings.add("android.app.AppComponentFactory")
                                strings.size - 1
                            }
                        }
                    }
                    factoryAttr.rawValueIdx = appFactoryFallbackIdx
                    factoryAttr.dataType = TYPE_STRING_VALUE
                    factoryAttr.data = appFactoryFallbackIdx
                }
            }
        }

        val rebuilt = encodeAxml(
            strings = strings,
            originalRawStyleBytes = parsed.rawStyleSectionBytes,
            originalStyleCount = parsed.styleCount,
            resourceIds = resIds,
            nodes = nodes
        )
        return PatchedManifestResult(rebuilt, info)
    }

    private fun resolveClassName(pkg: String, raw: String): String {
        if (raw.isBlank()) return ""
        return when {
            raw.startsWith(".") && pkg.isNotBlank() -> "$pkg$raw"
            !raw.contains(".") && pkg.isNotBlank() -> "$pkg.$raw"
            else -> raw
        }
    }

    private fun shiftNodeStringIndices(nodes: MutableList<AxmlNode>, insertedAt: Int) {
        fun remap(idx: Int): Int = if (idx >= insertedAt) idx + 1 else idx
        for (n in nodes) {
            if (n.commentIdx >= 0) n.commentIdx = remap(n.commentIdx)
            if (n.nsIdx >= 0) n.nsIdx = remap(n.nsIdx)
            if (n.elemNameIdx >= 0) n.elemNameIdx = remap(n.elemNameIdx)
            if (n.nsPrefixIdx >= 0) n.nsPrefixIdx = remap(n.nsPrefixIdx)
            if (n.nsUriIdx >= 0) n.nsUriIdx = remap(n.nsUriIdx)
            if (n.cdataIdx >= 0) n.cdataIdx = remap(n.cdataIdx)
            for (a in n.attributes) {
                if (a.nsIdx >= 0) a.nsIdx = remap(a.nsIdx)
                if (a.nameIdx >= 0) a.nameIdx = remap(a.nameIdx)
                if (a.rawValueIdx >= 0) a.rawValueIdx = remap(a.rawValueIdx)
                if (a.dataType == TYPE_STRING_VALUE && a.data >= 0) a.data = remap(a.data)
            }
        }
    }

    // --- Binary AXML Parser & Encoder ---

    private data class ParsedAxml(
        val strings: List<String>,
        val styleCount: Int,
        val rawStyleSectionBytes: ByteArray,
        val resourceIds: List<Int>,
        val nodes: List<AxmlNode>
    )

    private class AxmlNode(
        val type: Int,
        val headerSize: Int,
        val lineNumber: Int,
        var commentIdx: Int,
        var nsPrefixIdx: Int = -1,
        var nsUriIdx: Int = -1,
        var nsIdx: Int = -1,
        var elemNameIdx: Int = -1,
        var attrStart: Int = 20,
        var attrSize: Int = 20,
        var idIndex: Int = 0,
        var classIndex: Int = 0,
        var styleIndex: Int = 0,
        val attributes: MutableList<AxmlAttr> = mutableListOf(),
        var cdataIdx: Int = -1,
        val extraTailBytes: ByteArray = ByteArray(0)
    )

    private class AxmlAttr(
        var nsIdx: Int,
        var nameIdx: Int,
        var rawValueIdx: Int,
        val typedSize: Int,
        val res0: Int,
        var dataType: Int,
        var data: Int
    )

    private fun parseAxml(bytes: ByteArray): ParsedAxml {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.getInt(0)
        require(fileType == CHUNK_AXML_FILE) { "Not valid AXML header" }

        var strings = emptyList<String>()
        var styleCount = 0
        var rawStyleSectionBytes = ByteArray(0)
        var resIds = emptyList<Int>()
        val nodes = mutableListOf<AxmlNode>()

        var pos = 8
        while (pos + 8 <= bytes.size) {
            val type = buf.getShort(pos).toInt() and 0xFFFF
            val headerSize = buf.getShort(pos + 2).toInt() and 0xFFFF
            val chunkSize = buf.getInt(pos + 4)
            require(chunkSize >= 8 && pos + chunkSize <= bytes.size) { "Corrupt AXML chunk at $pos" }

            when (type) {
                TYPE_STRING_POOL -> {
                    val stringCount = buf.getInt(pos + 8)
                    styleCount = buf.getInt(pos + 12)
                    val flags = buf.getInt(pos + 16)
                    val isUtf8 = (flags and 0x100) != 0
                    val stringsStart = buf.getInt(pos + 20)
                    val stylesStart = buf.getInt(pos + 24)
                    val offsetsBase = pos + headerSize
                    val dataStart = pos + stringsStart
                    val parsedList = ArrayList<String>(stringCount)
                    for (i in 0 until stringCount) {
                        val strRelOffset = buf.getInt(offsetsBase + i * 4)
                        val absOff = dataStart + strRelOffset
                        parsedList.add(
                            if (isUtf8) readUtf8PoolString(bytes, absOff) else readUtf16PoolString(bytes, absOff)
                        )
                    }
                    strings = parsedList
                    if (styleCount > 0 && stylesStart > 0 && stylesStart < chunkSize) {
                        val styleOffsetsLen = styleCount * 4
                        val styleOffsetsAbs = offsetsBase + stringCount * 4
                        val styleDataAbs = pos + stylesStart
                        val baos = ByteArrayOutputStream()
                        baos.write(bytes, styleOffsetsAbs, styleOffsetsLen)
                        baos.write(bytes, styleDataAbs, (pos + chunkSize) - styleDataAbs)
                        rawStyleSectionBytes = baos.toByteArray()
                    }
                }
                TYPE_RESOURCE_MAP -> {
                    val count = (chunkSize - headerSize) / 4
                    val list = ArrayList<Int>(count)
                    val base = pos + headerSize
                    for (i in 0 until count) {
                        list.add(buf.getInt(base + i * 4))
                    }
                    resIds = list
                }
                TYPE_START_NAMESPACE, TYPE_END_NAMESPACE -> {
                    val line = buf.getInt(pos + 8)
                    val comment = buf.getInt(pos + 12)
                    val prefix = buf.getInt(pos + 16)
                    val uri = buf.getInt(pos + 20)
                    nodes.add(
                        AxmlNode(
                            type = type,
                            headerSize = headerSize,
                            lineNumber = line,
                            commentIdx = comment,
                            nsPrefixIdx = prefix,
                            nsUriIdx = uri
                        )
                    )
                }
                TYPE_START_ELEMENT -> {
                    val line = buf.getInt(pos + 8)
                    val comment = buf.getInt(pos + 12)
                    val ns = buf.getInt(pos + 16)
                    val name = buf.getInt(pos + 20)
                    val attrStart = buf.getShort(pos + 24).toInt() and 0xFFFF
                    val attrSize = buf.getShort(pos + 26).toInt() and 0xFFFF
                    val attrCount = buf.getShort(pos + 28).toInt() and 0xFFFF
                    val idIdx = buf.getShort(pos + 30).toInt() and 0xFFFF
                    val clsIdx = buf.getShort(pos + 32).toInt() and 0xFFFF
                    val styIdx = buf.getShort(pos + 34).toInt() and 0xFFFF
                    val attrs = ArrayList<AxmlAttr>(attrCount)
                    val attrBase = pos + 16 + attrStart
                    for (i in 0 until attrCount) {
                        val p = attrBase + i * attrSize
                        attrs.add(
                            AxmlAttr(
                                nsIdx = buf.getInt(p),
                                nameIdx = buf.getInt(p + 4),
                                rawValueIdx = buf.getInt(p + 8),
                                typedSize = buf.getShort(p + 12).toInt() and 0xFFFF,
                                res0 = bytes[p + 14].toInt() and 0xFF,
                                dataType = bytes[p + 15].toInt() and 0xFF,
                                data = buf.getInt(p + 16)
                            )
                        )
                    }
                    nodes.add(
                        AxmlNode(
                            type = type,
                            headerSize = headerSize,
                            lineNumber = line,
                            commentIdx = comment,
                            nsIdx = ns,
                            elemNameIdx = name,
                            attrStart = attrStart,
                            attrSize = attrSize,
                            idIndex = idIdx,
                            classIndex = clsIdx,
                            styleIndex = styIdx,
                            attributes = attrs
                        )
                    )
                }
                TYPE_END_ELEMENT -> {
                    val line = buf.getInt(pos + 8)
                    val comment = buf.getInt(pos + 12)
                    val ns = buf.getInt(pos + 16)
                    val name = buf.getInt(pos + 20)
                    nodes.add(
                        AxmlNode(
                            type = type,
                            headerSize = headerSize,
                            lineNumber = line,
                            commentIdx = comment,
                            nsIdx = ns,
                            elemNameIdx = name
                        )
                    )
                }
                TYPE_CDATA -> {
                    val line = buf.getInt(pos + 8)
                    val comment = buf.getInt(pos + 12)
                    val data = buf.getInt(pos + 16)
                    val tail = bytes.copyOfRange(pos + 20, pos + chunkSize)
                    nodes.add(
                        AxmlNode(
                            type = type,
                            headerSize = headerSize,
                            lineNumber = line,
                            commentIdx = comment,
                            cdataIdx = data,
                            extraTailBytes = tail
                        )
                    )
                }
            }
            pos += chunkSize
        }

        return ParsedAxml(strings, styleCount, rawStyleSectionBytes, resIds, nodes)
    }

    private fun encodeAxml(
        strings: List<String>,
        originalRawStyleBytes: ByteArray,
        originalStyleCount: Int,
        resourceIds: List<Int>,
        nodes: List<AxmlNode>
    ): ByteArray {
        val spBytes = encodeUtf16StringPool(strings, originalStyleCount, originalRawStyleBytes)
        val resMapBytes = if (resourceIds.isEmpty()) ByteArray(0) else encodeResourceMap(resourceIds)
        val nodesBaos = ByteArrayOutputStream()
        for (n in nodes) {
            nodesBaos.write(encodeNode(n))
        }
        val nodesBytes = nodesBaos.toByteArray()

        val totalSize = 8 + spBytes.size + resMapBytes.size + nodesBytes.size
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(CHUNK_AXML_FILE)
        out.putInt(totalSize)
        out.put(spBytes)
        out.put(resMapBytes)
        out.put(nodesBytes)
        return out.array()
    }

    private fun encodeUtf16StringPool(
        strings: List<String>,
        styleCount: Int,
        rawStyleBytes: ByteArray
    ): ByteArray {
        val stringDataBaos = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for (i in strings.indices) {
            offsets[i] = stringDataBaos.size()
            val s = strings[i]
            val charLen = s.length
            if (charLen >= 0x8000) {
                val hi = ((charLen shr 16) and 0x7FFF) or 0x8000
                val lo = charLen and 0xFFFF
                stringDataBaos.write(hi and 0xFF)
                stringDataBaos.write((hi shr 8) and 0xFF)
                stringDataBaos.write(lo and 0xFF)
                stringDataBaos.write((lo shr 8) and 0xFF)
            } else {
                stringDataBaos.write(charLen and 0xFF)
                stringDataBaos.write((charLen shr 8) and 0xFF)
            }
            for (c in s) {
                val code = c.code
                stringDataBaos.write(code and 0xFF)
                stringDataBaos.write((code shr 8) and 0xFF)
            }
            stringDataBaos.write(0)
            stringDataBaos.write(0)
        }
        while (stringDataBaos.size() % 4 != 0) {
            stringDataBaos.write(0)
        }
        val strData = stringDataBaos.toByteArray()

        val headerSize = 28
        val effectiveStyleCount = if (rawStyleBytes.isEmpty()) 0 else styleCount
        val strOffsetsBytes = strings.size * 4
        val styleOffsetsBytes = effectiveStyleCount * 4
        val stringsStart = headerSize + strOffsetsBytes + styleOffsetsBytes
        val styleDataBytesLen = if (effectiveStyleCount > 0) (rawStyleBytes.size - styleOffsetsBytes).coerceAtLeast(0) else 0
        val stylesStart = if (effectiveStyleCount > 0) stringsStart + strData.size else 0
        val chunkSize = stringsStart + strData.size + styleDataBytesLen
        val paddedChunkSize = (chunkSize + 3) and 3.inv()

        val buf = ByteBuffer.allocate(paddedChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(TYPE_STRING_POOL.toShort())
        buf.putShort(headerSize.toShort())
        buf.putInt(paddedChunkSize)
        buf.putInt(strings.size)
        buf.putInt(effectiveStyleCount)
        buf.putInt(0) // UTF-16LE
        buf.putInt(stringsStart)
        buf.putInt(stylesStart)
        for (off in offsets) buf.putInt(off)
        if (effectiveStyleCount > 0) {
            buf.put(rawStyleBytes, 0, styleOffsetsBytes)
        }
        buf.put(strData)
        if (effectiveStyleCount > 0 && styleDataBytesLen > 0) {
            buf.put(rawStyleBytes, styleOffsetsBytes, styleDataBytesLen)
        }
        return buf.array()
    }

    private fun encodeResourceMap(resIds: List<Int>): ByteArray {
        val size = 8 + resIds.size * 4
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(TYPE_RESOURCE_MAP.toShort())
        buf.putShort(8.toShort())
        buf.putInt(size)
        for (id in resIds) buf.putInt(id)
        return buf.array()
    }

    private fun encodeNode(n: AxmlNode): ByteArray {
        return when (n.type) {
            TYPE_START_NAMESPACE, TYPE_END_NAMESPACE -> {
                ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(n.type.toShort())
                    .putShort(16.toShort())
                    .putInt(24)
                    .putInt(n.lineNumber)
                    .putInt(n.commentIdx)
                    .putInt(n.nsPrefixIdx)
                    .putInt(n.nsUriIdx)
                    .array()
            }
            TYPE_END_ELEMENT -> {
                ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(TYPE_END_ELEMENT.toShort())
                    .putShort(16.toShort())
                    .putInt(24)
                    .putInt(n.lineNumber)
                    .putInt(n.commentIdx)
                    .putInt(n.nsIdx)
                    .putInt(n.elemNameIdx)
                    .array()
            }
            TYPE_START_ELEMENT -> {
                val attrSize = 20
                val chunkSize = 36 + n.attributes.size * attrSize
                val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
                buf.putShort(TYPE_START_ELEMENT.toShort())
                buf.putShort(16.toShort())
                buf.putInt(chunkSize)
                buf.putInt(n.lineNumber)
                buf.putInt(n.commentIdx)
                buf.putInt(n.nsIdx)
                buf.putInt(n.elemNameIdx)
                buf.putShort(20.toShort())
                buf.putShort(attrSize.toShort())
                buf.putShort(n.attributes.size.toShort())
                buf.putShort(n.idIndex.toShort())
                buf.putShort(n.classIndex.toShort())
                buf.putShort(n.styleIndex.toShort())
                for (a in n.attributes) {
                    buf.putInt(a.nsIdx)
                    buf.putInt(a.nameIdx)
                    buf.putInt(a.rawValueIdx)
                    buf.putShort(a.typedSize.toShort())
                    buf.put(a.res0.toByte())
                    buf.put(a.dataType.toByte())
                    buf.putInt(a.data)
                }
                buf.array()
            }
            TYPE_CDATA -> {
                val chunkSize = 20 + n.extraTailBytes.size
                ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(TYPE_CDATA.toShort())
                    .putShort(16.toShort())
                    .putInt(chunkSize)
                    .putInt(n.lineNumber)
                    .putInt(n.commentIdx)
                    .putInt(n.cdataIdx)
                    .put(n.extraTailBytes)
                    .array()
            }
            else -> ByteArray(0)
        }
    }

    private fun readUtf8PoolString(bytes: ByteArray, offset: Int): String {
        var pos = offset
        // char length varint
        val b0 = bytes[pos++].toInt() and 0xFF
        if ((b0 and 0x80) != 0) pos++
        // byte length varint
        val l0 = bytes[pos++].toInt() and 0xFF
        val byteLen = if ((l0 and 0x80) != 0) {
            val l1 = bytes[pos++].toInt() and 0xFF
            ((l0 and 0x7F) shl 8) or l1
        } else {
            l0
        }
        return String(bytes, pos, byteLen.coerceIn(0, bytes.size - pos), Charsets.UTF_8)
    }

    private fun readUtf16PoolString(bytes: ByteArray, offset: Int): String {
        var pos = offset
        val u0 = ((bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8))
        pos += 2
        val charLen = if ((u0 and 0x8000) != 0) {
            val u1 = ((bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8))
            pos += 2
            ((u0 and 0x7FFF) shl 16) or u1
        } else {
            u0
        }
        val byteLen = (charLen * 2).coerceIn(0, bytes.size - pos)
        return String(bytes, pos, byteLen, Charsets.UTF_16LE)
    }
}
