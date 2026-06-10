package io.github.bzzq.hooks

import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Method

class DexDesc(
    private val cacheStore: DexKitCacheStore,
    private val descName: String,
    private val enabled: Boolean,
    private val classLoader: ClassLoader,
) {
    fun toMethodOrNull(): Method? {
        if (!enabled) return null

        val cached = cacheStore.readMethodCache(descName)
        if (cached.isEmpty()) {
            runCatching { cacheStore.openBridge() }
            return null
        }

        return runCatching {
            DexMethod(cached).getMethodInstance(classLoader).apply { isAccessible = true }
        }.getOrElse {
            runCatching { cacheStore.openBridge() }
            null
        }
    }
}
