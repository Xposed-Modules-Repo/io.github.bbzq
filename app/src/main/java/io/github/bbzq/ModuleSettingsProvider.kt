package io.github.bbzq

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle

class ModuleSettingsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        enforceCaller()
        val prefs = contextOrThrow().moduleSettingsPrefs()

        return when (method) {
            METHOD_GET_BOOLEAN -> bundleOfBoolean(prefs.getBoolean(requireKey(arg), extras?.getBoolean(EXTRA_DEFAULT) ?: false))
            METHOD_GET_INT -> bundleOfInt(prefs.getInt(requireKey(arg), extras?.getInt(EXTRA_DEFAULT) ?: 0))
            METHOD_GET_STRING -> bundleOfString(prefs.getString(requireKey(arg), extras?.getString(EXTRA_DEFAULT)))
            METHOD_GET_STRING_SET -> bundleOfStringSet(
                prefs.getStringSet(requireKey(arg), extras?.getStringArrayList(EXTRA_DEFAULT)?.toSet())
                    ?.toList()
                    .orEmpty(),
            )
            METHOD_PUT_BOOLEAN -> {
                prefs.edit().putBoolean(requireKey(arg), extras?.getBoolean(EXTRA_VALUE) ?: false).apply()
                Bundle.EMPTY
            }
            METHOD_PUT_INT -> {
                prefs.edit().putInt(requireKey(arg), extras?.getInt(EXTRA_VALUE) ?: 0).apply()
                Bundle.EMPTY
            }
            METHOD_PUT_STRING -> {
                prefs.edit().putString(requireKey(arg), extras?.getString(EXTRA_VALUE)).apply()
                Bundle.EMPTY
            }
            METHOD_PUT_STRING_SET -> {
                prefs.edit()
                    .putStringSet(requireKey(arg), extras?.getStringArrayList(EXTRA_VALUE)?.toSet())
                    .apply()
                Bundle.EMPTY
            }
            METHOD_REMOVE -> {
                prefs.edit().remove(requireKey(arg)).apply()
                Bundle.EMPTY
            }
            METHOD_CONTAINS -> bundleOfBoolean(prefs.contains(requireKey(arg)))
            METHOD_GET_ALL -> Bundle().apply {
                prefs.all.forEach { (k, v) ->
                    when (v) {
                        is Boolean -> putBoolean(k, v)
                        is Int -> putInt(k, v)
                        is Long -> putLong(k, v)
                        is Float -> putFloat(k, v)
                        is String -> putString(k, v)
                        is Set<*> -> putStringArrayList(k, ArrayList(v.map { it.toString() }))
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun contextOrThrow(): Context = checkNotNull(context)

    private fun Context.moduleSettingsPrefs(): SharedPreferences =
        ReadableModulePreferences(
            this,
            getSharedPreferences(ModuleSettings.PREFS_NAME, Context.MODE_PRIVATE),
        )

    private fun requireKey(key: String?): String = requireNotNull(key) { "Preference key is required" }

    private fun enforceCaller() {
        val context = contextOrThrow()
        val pm = context.packageManager
        val callerUid = Binder.getCallingUid()
        val packages = pm.getPackagesForUid(callerUid).orEmpty()
        if (packages.any { it == context.packageName || it in ALLOWED_CLIENT_PACKAGES }) return
        throw SecurityException("Caller is not allowed to access module settings")
    }

    private fun bundleOfBoolean(value: Boolean) = Bundle().apply { putBoolean(EXTRA_VALUE, value) }

    private fun bundleOfInt(value: Int) = Bundle().apply { putInt(EXTRA_VALUE, value) }

    private fun bundleOfString(value: String?) = Bundle().apply { putString(EXTRA_VALUE, value) }

    private fun bundleOfStringSet(value: List<String>) = Bundle().apply {
        putStringArrayList(EXTRA_VALUE, ArrayList(value))
    }

    companion object {
        @JvmField val AUTHORITY = "io.github.bbzq.settings"
        @JvmField val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        @JvmField val METHOD_GET_BOOLEAN = "getBoolean"
        @JvmField val METHOD_GET_INT = "getInt"
        @JvmField val METHOD_GET_STRING = "getString"
        @JvmField val METHOD_GET_STRING_SET = "getStringSet"
        @JvmField val METHOD_PUT_BOOLEAN = "putBoolean"
        @JvmField val METHOD_PUT_INT = "putInt"
        @JvmField val METHOD_PUT_STRING = "putString"
        @JvmField val METHOD_PUT_STRING_SET = "putStringSet"
        @JvmField val METHOD_REMOVE = "remove"
        @JvmField val METHOD_CONTAINS = "contains"
        @JvmField val METHOD_GET_ALL = "getAll"

        @JvmField val EXTRA_DEFAULT = "default"
        @JvmField val EXTRA_VALUE = "value"

        private val ALLOWED_CLIENT_PACKAGES = setOf(
            "tv.danmaku.bili",
            "com.bilibili.app.in",
            "tv.danmaku.bilibilihd",
            "com.bilibili.app.blue",
        )
    }
}
