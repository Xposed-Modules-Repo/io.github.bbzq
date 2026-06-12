package io.github.bzzq

import android.app.Application
import android.content.ContentResolver
import android.content.SharedPreferences
import android.os.Bundle
import java.lang.ref.WeakReference
import java.lang.reflect.Method

class ModuleSettingsBridge private constructor() : SharedPreferences {
    private var localCache: Map<String, Any> = emptyMap()
    private var lastLoadTime: Long = 0

    private fun ensureLoaded() {
        val now = System.currentTimeMillis()
        if (now - lastLoadTime < CACHE_EXPIRATION && localCache.isNotEmpty()) return
        
        resolveContentResolver()?.let { resolver ->
            val context = resolver.resolveContext()
            val loaded = JsonSettingsStore.load(context)
            if (loaded.isNotEmpty()) {
                localCache = loaded
            } else {
                // First run or missing JSON, sync from provider
                val all = mutableMapOf<String, Any>()
                getAllFromProvider().forEach { (k, v) ->
                    if (v != null) all[k] = v
                }
                localCache = all
                Thread { JsonSettingsStore.save(context, all) }.start()
            }
            lastLoadTime = now
        }
    }

    private fun getAllFromProvider(): Map<String, Any?> {
        val result = call(ModuleSettingsProvider.METHOD_GET_ALL, null, null)
        val map = mutableMapOf<String, Any?>()
        result?.keySet()?.forEach { key ->
            map[key] = result.get(key)
        }
        return if (map.isNotEmpty()) map else fallbackDefaults()
    }

    private fun fallbackDefaults(): Map<String, Any?> = mapOf(
        ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED to true,
        ModuleSettings.KEY_UNLOCK_VIDEO_FEATURES_ENABLED to true,
        ModuleSettings.KEY_FULL_NUMBER_FORMAT_ENABLED to false
    )

    private fun ContentResolver.resolveContext(): android.content.Context {
        return cachedApplication.get() ?: (currentApplicationMethod.invoke(null) as android.app.Application)
    }

    override fun getAll(): MutableMap<String, *> {
        ensureLoaded()
        return localCache.toMutableMap()
    }

    override fun getString(key: String?, defValue: String?): String? {
        ensureLoaded()
        return (localCache[key] as? String) ?: run {
            val result = call(ModuleSettingsProvider.METHOD_GET_STRING, key, Bundle().apply {
                putString(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
            })
            result?.getString(ModuleSettingsProvider.EXTRA_VALUE, defValue)
        }
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        ensureLoaded()
        // JSON doesn't support Set directly, it's stored as JSONArray/List
        return (localCache[key] as? List<*>)?.map { it.toString() }?.toMutableSet() ?: run {
            val result = call(ModuleSettingsProvider.METHOD_GET_STRING_SET, key, Bundle().apply {
                putStringArrayList(ModuleSettingsProvider.EXTRA_DEFAULT, ArrayList(defValues.orEmpty()))
            })
            result?.getStringArrayList(ModuleSettingsProvider.EXTRA_VALUE)?.toMutableSet()
                ?: defValues
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        ensureLoaded()
        return (localCache[key] as? Number)?.toInt() ?: run {
            val result = call(ModuleSettingsProvider.METHOD_GET_INT, key, Bundle().apply {
                putInt(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
            })
            result?.getInt(ModuleSettingsProvider.EXTRA_VALUE, defValue) ?: defValue
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        ensureLoaded()
        return (localCache[key] as? Number)?.toLong() ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        ensureLoaded()
        return (localCache[key] as? Number)?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        ensureLoaded()
        return (localCache[key] as? Boolean) ?: run {
            val result = call(ModuleSettingsProvider.METHOD_GET_BOOLEAN, key, Bundle().apply {
                putBoolean(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
            })
            result?.getBoolean(ModuleSettingsProvider.EXTRA_VALUE, defValue) ?: defValue
        }
    }

    override fun contains(key: String?): Boolean {
        val result = call(ModuleSettingsProvider.METHOD_CONTAINS, key, null)
        return result?.getBoolean(ModuleSettingsProvider.EXTRA_VALUE, false) ?: false
    }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return resolveContentResolver()?.call(ModuleSettingsProvider.CONTENT_URI, method, arg, extras)
    }

    private fun resolveContentResolver(): ContentResolver? {
        cachedApplication.get()?.let { return it.contentResolver }
        val application = runCatching {
            currentApplicationMethod.invoke(null) as? Application
        }.getOrNull() ?: return null
        cachedApplication = WeakReference(application)
        return application.contentResolver
    }

    private inner class Editor : SharedPreferences.Editor {
        private val operations = mutableListOf<() -> Unit>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            operations += {
                call(ModuleSettingsProvider.METHOD_PUT_STRING, key, Bundle().apply {
                    putString(ModuleSettingsProvider.EXTRA_VALUE, value)
                })
            }
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            operations += {
                call(ModuleSettingsProvider.METHOD_PUT_STRING_SET, key, Bundle().apply {
                    putStringArrayList(ModuleSettingsProvider.EXTRA_VALUE, ArrayList(values.orEmpty()))
                })
            }
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            operations += {
                call(ModuleSettingsProvider.METHOD_PUT_INT, key, Bundle().apply {
                    putInt(ModuleSettingsProvider.EXTRA_VALUE, value)
                })
            }
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            operations += {
                call(ModuleSettingsProvider.METHOD_PUT_BOOLEAN, key, Bundle().apply {
                    putBoolean(ModuleSettingsProvider.EXTRA_VALUE, value)
                })
            }
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            operations += { call(ModuleSettingsProvider.METHOD_REMOVE, key, null) }
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                getAll().keys.forEach { key ->
                    call(ModuleSettingsProvider.METHOD_REMOVE, key, null)
                }
            }
            operations.forEach { it.invoke() }
            operations.clear()
            clearRequested = false

            // Save to JSON for robust cross-process settings sync
            val resolver = resolveContentResolver()
            if (resolver != null) {
                val context = resolver.resolveContext()
                val all = getAll()
                @Suppress("UNCHECKED_CAST")
                localCache = all as Map<String, Any>
                lastLoadTime = System.currentTimeMillis()
                
                // Perform disk write in background
                Thread {
                    JsonSettingsStore.save(context, localCache)
                }.start()
            }
        }
    }

    companion object {
        private const val CACHE_EXPIRATION = 5000L // 5 seconds
        private var cachedApplication = WeakReference<Application>(null)
        private val currentApplicationMethod: Method by lazy(LazyThreadSafetyMode.NONE) {
            Class.forName("android.app.ActivityThread")
                .getDeclaredMethod("currentApplication")
                .apply { isAccessible = true }
        }

        val instance: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
            ModuleSettingsBridge()
        }
    }
}
