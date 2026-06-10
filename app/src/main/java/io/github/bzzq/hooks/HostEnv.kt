package io.github.bzzq.hooks

import android.app.Application
import android.content.pm.ApplicationInfo
import java.lang.ref.WeakReference
import java.lang.reflect.Method

object HostEnv {
    private var cachedApplication = WeakReference<Application>(null)
    private val currentApplicationMethod: Method by lazy(LazyThreadSafetyMode.NONE) {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
            .apply { isAccessible = true }
    }

    fun currentApplication(): Application? {
        cachedApplication.get()?.let { return it }
        val application = runCatching {
            currentApplicationMethod.invoke(null) as? Application
        }.getOrNull() ?: return null
        cachedApplication = WeakReference(application)
        return application
    }

    fun versionCode(appInfo: ApplicationInfo): Long {
        return runCatching {
            ApplicationInfo::class.java.getMethod("getLongVersionCode").invoke(appInfo) as Long
        }.getOrElse {
            runCatching {
                ApplicationInfo::class.java.getField("versionCode").getInt(appInfo).toLong()
            }.getOrDefault(0L)
        }
    }
}
