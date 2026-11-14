package com.example

import android.app.Application
import com.example.security.ZeroCrashGuard

class DexShieldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ZeroCrashGuard.install()
        ZeroCrashGuard.probeMainThreadLatency()
    }
}
