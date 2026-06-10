package io.github.bzzq.hooks

import android.content.SharedPreferences
import io.github.bzzq.ModuleSettingsBridge
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.io.Closeable

class HookContext(
    val xposed: XposedInterface,
    val packageReady: PackageReadyParam,
    val log: (String, Throwable?) -> Unit,
) : Closeable {
    val packageName: String
        get() = packageReady.getPackageName()

    val classLoader: ClassLoader
        get() = packageReady.getClassLoader()

    val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        ModuleSettingsBridge.instance
    }

    private val dexKitCacheStore by lazy(LazyThreadSafetyMode.NONE) {
        DexKitCacheStore(
            packageName = packageName,
            versionCode = HostEnv.versionCode(packageReady.getApplicationInfo()),
            sourceDir = packageReady.getApplicationInfo().sourceDir,
            classLoader = classLoader,
            log = log,
        ).also { it.ensureVersionState() }
    }

    fun dexKitCache(): DexKitCacheStore = dexKitCacheStore

    fun dexKitOrNull() = runCatching {
        DexKitLoader.ensureLoaded(log)
        dexKitCacheStore.openBridge()
    }.onFailure {
        log("Failed to create DexKit bridge for $packageName", it)
    }.getOrNull()

    fun dexDesc(name: String, enabled: Boolean = true): DexDesc {
        return DexDesc(dexKitCacheStore, name, enabled, classLoader)
    }

    override fun close() {
        runCatching { dexKitCacheStore.close() }
    }
}

private object DexKitLoader {
    @Volatile
    private var loaded = false

    fun ensureLoaded(log: (String, Throwable?) -> Unit) {
        if (loaded) return

        synchronized(this) {
            if (loaded) return
            runCatching { System.loadLibrary("dexkit") }
                .onSuccess { loaded = true }
                .onFailure { log("Failed to load DexKit native library", it) }
        }
    }
}
