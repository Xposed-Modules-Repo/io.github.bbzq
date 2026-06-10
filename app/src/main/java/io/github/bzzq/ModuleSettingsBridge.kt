package io.github.bzzq

import android.app.Application
import android.content.ContentResolver
import android.content.SharedPreferences
import android.os.Bundle
import java.lang.ref.WeakReference
import java.lang.reflect.Method

class ModuleSettingsBridge private constructor() : SharedPreferences {
    override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any?>(
        ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED to getBoolean(ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED, true),
        ModuleSettings.KEY_UNLOCK_VIDEO_FEATURES_ENABLED to getBoolean(ModuleSettings.KEY_UNLOCK_VIDEO_FEATURES_ENABLED, true),
        ModuleSettings.KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED to getBoolean(ModuleSettings.KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED, false),
        ModuleSettings.KEY_FIX_LIVE_QUALITY_URL_ENABLED to getBoolean(ModuleSettings.KEY_FIX_LIVE_QUALITY_URL_ENABLED, false),
        ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED to getBoolean(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED, false),
        ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS to getStringSet(
            ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS,
            ModuleSettings.defaultStoryVideoAdTags.toMutableSet(),
        ),
        ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT to getInt(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT, 0),
        ModuleSettings.KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED to getBoolean(ModuleSettings.KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED, true),
        ModuleSettings.KEY_BLOCK_LIVE_RESERVATION_ENABLED to getBoolean(ModuleSettings.KEY_BLOCK_LIVE_RESERVATION_ENABLED, false),
        ModuleSettings.KEY_BLOCK_LIVE_ROOM_QOE_POPUP_ENABLED to getBoolean(ModuleSettings.KEY_BLOCK_LIVE_ROOM_QOE_POPUP_ENABLED, false),
        ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED to getBoolean(ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED, false),
        ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED to getBoolean(ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false),
        ModuleSettings.KEY_PURIFY_SHARE_ENABLED to getBoolean(ModuleSettings.KEY_PURIFY_SHARE_ENABLED, false),
        ModuleSettings.KEY_FULL_NUMBER_FORMAT_ENABLED to getBoolean(ModuleSettings.KEY_FULL_NUMBER_FORMAT_ENABLED, false),
        ModuleSettings.KEY_UNLOCK_COMMENT_GIF_ENABLED to getBoolean(ModuleSettings.KEY_UNLOCK_COMMENT_GIF_ENABLED, false),
        ModuleSettings.KEY_LAST_ACCESS_KEY to getString(ModuleSettings.KEY_LAST_ACCESS_KEY, null),
    )

    override fun getString(key: String?, defValue: String?): String? {
        val result = call(ModuleSettingsProvider.METHOD_GET_STRING, key, Bundle().apply {
            putString(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
        })
        return result?.getString(ModuleSettingsProvider.EXTRA_VALUE, defValue)
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val result = call(ModuleSettingsProvider.METHOD_GET_STRING_SET, key, Bundle().apply {
            putStringArrayList(ModuleSettingsProvider.EXTRA_DEFAULT, ArrayList(defValues.orEmpty()))
        })
        return result?.getStringArrayList(ModuleSettingsProvider.EXTRA_VALUE)?.toMutableSet()
            ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val result = call(ModuleSettingsProvider.METHOD_GET_INT, key, Bundle().apply {
            putInt(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
        })
        return result?.getInt(ModuleSettingsProvider.EXTRA_VALUE, defValue) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long = defValue

    override fun getFloat(key: String?, defValue: Float): Float = defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val result = call(ModuleSettingsProvider.METHOD_GET_BOOLEAN, key, Bundle().apply {
            putBoolean(ModuleSettingsProvider.EXTRA_DEFAULT, defValue)
        })
        return result?.getBoolean(ModuleSettingsProvider.EXTRA_VALUE, defValue) ?: defValue
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
        }
    }

    companion object {
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
