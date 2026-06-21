package com.shohan.cleanspace.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/**
 * Wraps all Shizuku-related calls in try/catch so the app never crashes
 * if Shizuku is not installed, not running, or permission was revoked.
 */
object ShizukuHelper {

    const val REQUEST_CODE = 9001

    var cacheService: ICacheService? = null
        private set

    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            cacheService = if (binder != null && binder.pingBinder()) {
                ICacheService.Stub.asInterface(binder)
            } else null
            isBound = cacheService != null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cacheService = null
            isBound = false
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Throwable) {
            // Shizuku not running, nothing we can do
        }
    }

    private fun serviceArgs(context: Context): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(
            ComponentName(context.packageName, CacheServiceImpl::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("cache_service")
            .debuggable(false)
            .version(1)
    }

    fun bindService(context: Context) {
        if (isBound) return
        try {
            Shizuku.bindUserService(serviceArgs(context), serviceConnection)
        } catch (e: Throwable) {
            cacheService = null
        }
    }

    fun unbindService(context: Context) {
        if (!isBound) return
        try {
            Shizuku.unbindUserService(serviceArgs(context), serviceConnection, true)
        } catch (e: Throwable) {
            // ignore
        }
        isBound = false
        cacheService = null
    }
}
