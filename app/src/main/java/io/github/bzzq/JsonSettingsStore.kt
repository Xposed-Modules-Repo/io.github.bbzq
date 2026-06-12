package io.github.bzzq

import android.content.Context
import org.json.JSONObject
import java.io.File

object JsonSettingsStore {
    private const val FILE_NAME = "module_settings.json"
    
    fun getSettingsFile(context: Context): File {
        // Use code_cache as requested, or fallback to cache
        val dir = context.codeCacheDir ?: context.cacheDir
        return File(dir, FILE_NAME)
    }

    fun save(context: Context, allSettings: Map<String, *>) {
        runCatching {
            val json = JSONObject()
            allSettings.forEach { (k, v) ->
                if (v is Set<*>) {
                    json.put(k, org.json.JSONArray(v))
                } else {
                    json.put(k, v)
                }
            }
            getSettingsFile(context).writeText(json.toString())
        }
    }

    fun load(context: Context): Map<String, Any> {
        return runCatching {
            val file = getSettingsFile(context)
            if (!file.exists()) return emptyMap()
            val json = JSONObject(file.readText())
            val map = mutableMapOf<String, Any>()
            json.keys().forEach { key ->
                val value = json.get(key)
                map[key] = if (value is org.json.JSONArray) {
                    List(value.length()) { i -> value.get(i) }
                } else {
                    value
                }
            }
            map
        }.getOrDefault(emptyMap())
    }
}
