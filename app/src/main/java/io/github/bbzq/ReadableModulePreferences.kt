package io.github.bbzq

import android.content.Context
import android.content.SharedPreferences
import java.io.File

class ReadableModulePreferences(
    private val context: Context,
    private val delegate: SharedPreferences,
) : SharedPreferences by delegate {

    init {
        ensureReadable()
        ModuleRemotePreferences.attach(context, delegate)
    }

    override fun edit(): SharedPreferences.Editor =
        ReadableEditor(delegate.edit())

    fun ensureReadable() {
        val dataDir = File(context.applicationInfo.dataDir)
        val prefsDir = File(dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "${ModuleSettings.PREFS_NAME}.xml")
        dataDir.setExecutable(true, false)
        prefsDir.setExecutable(true, false)
        prefsDir.setReadable(true, false)
        prefsFile.setReadable(true, false)
    }

    private inner class ReadableEditor(
        private val editor: SharedPreferences.Editor,
    ) : SharedPreferences.Editor by editor {
        private val operations = mutableListOf<PreferenceOperation>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, value)
            editor.putString(key, value)
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, values?.toSet())
            editor.putStringSet(key, values)
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, value)
            editor.putInt(key, value)
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, value)
            editor.putLong(key, value)
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, value)
            editor.putFloat(key, value)
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Put(key, value)
            editor.putBoolean(key, value)
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) operations += PreferenceOperation.Remove(key)
            editor.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            operations += PreferenceOperation.Clear
            editor.clear()
            return this
        }

        override fun apply() {
            editor.apply()
            ensureReadable()
            syncRemote()
        }

        override fun commit(): Boolean {
            val result = editor.commit()
            ensureReadable()
            syncRemote()
            return result
        }

        private fun syncRemote() {
            ModuleRemotePreferences.applyOperations(operations)
            operations.clear()
        }
    }
}
