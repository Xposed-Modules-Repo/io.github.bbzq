package io.github.bbzq

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.bbzq.feats.RoamingRuntime
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method

class BbzqModule : XposedModule() {
    private var packageName: String = ""
    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.getProcessName()
        log(
            Log.INFO,
            LOG_TAG,
            "Loaded in $processName on $frameworkName($frameworkVersionCode), api=$apiVersion",
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val packageName = param.getPackageName()
        if (packageName !in TARGET_PACKAGES || !param.isFirstPackage()) return
        this.packageName = packageName
        val application = resolveCurrentApplication()
        if (application == null) {
            log(Log.WARN, LOG_TAG, "Package ready but current application is unavailable: $packageName")
            return
        }
        startRuntime(
            packageName = packageName,
            processName = processName,
            application = application,
            classLoader = param.getClassLoader(),
        )
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        param.setSavedInstanceState(packageName.takeIf { it.isNotBlank() })
        log(Log.INFO, LOG_TAG, "Hot reloading requested for ${packageName.ifBlank { processName }}")
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        processName = param.getProcessName()
        val application = resolveCurrentApplication()
        val resolvedPackageName = application?.packageName
            ?: (param.getSavedInstanceState() as? String)
            ?: packageName

        if (resolvedPackageName !in TARGET_PACKAGES) {
            log(Log.WARN, LOG_TAG, "Skip hot reload outside target packages: $resolvedPackageName")
            super.onHotReloaded(param)
            return
        }

        val classLoader = application?.javaClass?.classLoader
        if (application == null || classLoader == null) {
            log(Log.WARN, LOG_TAG, "Hot reload skipped because current application is unavailable")
            super.onHotReloaded(param)
            return
        }

        packageName = resolvedPackageName
        startRuntime(
            packageName = resolvedPackageName,
            processName = processName,
            application = application,
            classLoader = classLoader,
        )
        super.onHotReloaded(param)
    }

    private fun startRuntime(
        packageName: String,
        processName: String,
        application: Context,
        classLoader: ClassLoader,
    ) {
        RoamingRuntime.start(
            xposed = this,
            packageName = packageName,
            processName = processName,
            application = application,
            classLoader = classLoader,
        ) { message, throwable ->
            if (throwable == null) {
                log(Log.INFO, LOG_TAG, message)
            } else {
                log(Log.WARN, LOG_TAG, message, throwable)
            }
        }
    }

    private fun resolveCurrentApplication(): Application? {
        return runCatching {
            currentApplicationMethod.invoke(null) as? Application
        }.getOrNull()
    }

    private companion object {
        private const val LOG_TAG = "BBZQ"

        private val TARGET_PACKAGES = setOf(
            "tv.danmaku.bili",
            "com.bilibili.app.in",
            "tv.danmaku.bilibilihd",
            "com.bilibili.app.blue",
        )

        private val currentApplicationMethod: Method by lazy(LazyThreadSafetyMode.NONE) {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .apply { isAccessible = true }
        }
    }
}
