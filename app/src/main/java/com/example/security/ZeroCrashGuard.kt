package com.example.security

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class GuardedFault(
    val id: Long,
    val timestamp: String,
    val threadName: String,
    val exceptionType: String,
    val message: String,
    val source: String
)

/**
 * Layer 6: Zero-Crash Exception Guard & Main-Thread Looper Watchdog.
 * Installs a process-wide UncaughtExceptionHandler for background threads,
 * provides a safe CoroutineExceptionHandler, and executes guarded blocks without ANRs.
 */
object ZeroCrashGuard {
    private val installed = AtomicBoolean(false)
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private val _faultLog = MutableStateFlow<List<GuardedFault>>(emptyList())
    val faultLog: StateFlow<List<GuardedFault>> = _faultLog.asStateFlow()

    private val _isGuardActive = MutableStateFlow(false)
    val isGuardActive: StateFlow<Boolean> = _isGuardActive.asStateFlow()

    private val _mainThreadLatencyMs = MutableStateFlow(0L)
    val mainThreadLatencyMs: StateFlow<Long> = _mainThreadLatencyMs.asStateFlow()

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        recordFault("CoroutineScope", Thread.currentThread().name, throwable)
    }

    fun install() {
        if (installed.compareAndSet(false, true)) {
            previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                recordFault("UncaughtThreadHandler", thread.name, throwable)
                // For background threads, absorb safely so the app stays responsive.
                // Only delegate if the main looper itself is fatally broken beyond recovery.
                val mainLooper = try { Looper.getMainLooper() } catch (_: Throwable) { null }
                if (mainLooper != null && thread.id == mainLooper.thread.id && Looper.myLooper() == null) {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            }
            _isGuardActive.value = true
        }
    }

    inline fun <T> runGuarded(source: String, fallback: T, block: () -> T): T {
        return try {
            block()
        } catch (t: Throwable) {
            recordFault(source, Thread.currentThread().name, t)
            fallback
        }
    }

    fun recordFault(source: String, threadName: String, throwable: Throwable) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val fault = GuardedFault(
            id = System.nanoTime(),
            timestamp = formatter.format(Date()),
            threadName = threadName,
            exceptionType = throwable::class.java.simpleName.ifEmpty { "Throwable" },
            message = throwable.message ?: "No detail message",
            source = source
        )
        _faultLog.update { current -> (listOf(fault) + current).take(25) }
    }

    fun probeMainThreadLatency(onMeasured: (Long) -> Unit = {}) {
        val start = System.nanoTime()
        Handler(Looper.getMainLooper()).post {
            val elapsedMs = ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1L)
            _mainThreadLatencyMs.value = elapsedMs
            onMeasured(elapsedMs)
        }
    }

    fun simulateGuardedWorkerFault() {
        val worker = Thread({
            throw IllegalStateException("Simulated hook injection fault intercepted on worker thread")
        }, "DexShield-GuardTest")
        worker.start()
        worker.join(500)
    }

    fun clearLogs() {
        _faultLog.value = emptyList()
    }
}
