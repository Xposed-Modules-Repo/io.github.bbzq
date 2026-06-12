package io.github.bzzq.hooks

import android.app.Activity
import io.github.bzzq.InAppSettingsDialog
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Proxy

class BiliEntryHook(
    targetPackageName: String,
) : BaseHook(targetPackageName) {
    override fun startHook() {
        val count =
            hookTopLevelSettingsFragment(
                "com.bilibili.p4439app.preferences.BiliPreferencesActivity\$BiliPreferencesFragment",
                "com.bilibili.app.preferences.BiliPreferencesActivity\$BiliPreferencesFragment",
            ) +
                hookTopLevelSettingsFragment(
                    "com.bilibili.p4439app.preferences.fragment.WideBiliPreferencesFragment",
                    "com.bilibili.app.preferences.fragment.WideBiliPreferencesFragment",
                )
        log("Installed $count bilibili settings entry hook(s)")
    }

    private fun hookTopLevelSettingsFragment(vararg classNames: String): Int {
        val fragmentClass = HostAccess.findClass(classLoader, *classNames) ?: return 0
        val method = HostAccess.method(fragmentClass, listOf("onCreatePreferences")) {
            it.parameterCount == 2
        } ?: return 0

        xposed.hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
            .intercept { chain ->
                val result = chain.proceed()
                val fragment = chain.thisObject ?: return@intercept result
                runCatching { injectSettingsEntry(fragment) }
                    .onFailure { log("Failed to inject bilibili settings entry", it) }
                result
            }
        return 1
    }

    private fun injectSettingsEntry(fragment: Any) {
        if (HostAccess.invoke(fragment, "findPreference", ENTRY_KEY) != null) return

        val activity = HostAccess.invoke(fragment, "getActivity") as? Activity ?: return
        val targetGroup = findTargetGroup(fragment) ?: return
        val entry = createEntryPreference(fragment, activity) ?: return

        val added = HostAccess.invoke(targetGroup, "addPreference", entry) as? Boolean ?: false
        if (!added) return
        log("Injected advanced settings entry into ${fragment.javaClass.name}")
    }

    private fun findTargetGroup(fragment: Any): Any? {
        TARGET_GROUP_KEYS.forEach { key ->
            HostAccess.invoke(fragment, "findPreference", key)?.let { return it }
        }
        return HostAccess.invoke(fragment, "getPreferenceScreen")
    }

    private fun createEntryPreference(fragment: Any, activity: Activity): Any? {
        val preference = newPreferenceInstance(fragment, activity) ?: return null

        HostAccess.invoke(preference, "setKey", ENTRY_KEY)
        HostAccess.invoke(preference, "setTitle", ENTRY_TITLE)
        HostAccess.invoke(preference, "setSummary", ENTRY_SUMMARY)
        HostAccess.invoke(preference, "setPersistent", false)
        HostAccess.invoke(preference, "setSelectable", true)
        resolveAnchorOrder(fragment)?.let { order ->
            HostAccess.invoke(preference, "setOrder", order + 1)
        }

        val listenerSetter = HostAccess.method(preference.javaClass, listOf("setOnPreferenceClickListener")) {
            it.parameterCount == 1 && it.parameterTypes.firstOrNull()?.isInterface == true
        } ?: return preference
        val listenerClass = listenerSetter.parameterTypes[0]
        val listener = Proxy.newProxyInstance(
            listenerClass.classLoader ?: classLoader,
            arrayOf(listenerClass),
        ) { _, method, _ ->
            if (method.name == "onPreferenceClick") {
                InAppSettingsDialog.show(activity)
                true
            } else {
                null
            }
        }
        runCatching { listenerSetter.invoke(preference, listener) }
            .onFailure { log("Failed to attach settings click listener", it) }
        return preference
    }

    private fun resolveAnchorOrder(fragment: Any): Int? {
        ANCHOR_KEYS.forEach { key ->
            val preference = HostAccess.invoke(fragment, "findPreference", key) ?: return@forEach
            val order = HostAccess.invoke(preference, "getOrder")
            if (order is Int) return order
        }
        return null
    }

    private fun newPreferenceInstance(fragment: Any, activity: Activity): Any? {
        val classNames = if (fragment.javaClass.name.contains("WideBiliPreferencesFragment")) {
            WIDE_PREFERENCE_CLASSES
        } else {
            NORMAL_PREFERENCE_CLASSES
        }
        val preferenceClass = HostAccess.findClass(classLoader, *classNames) ?: return null
        return runCatching {
            preferenceClass.getConstructor(android.content.Context::class.java).newInstance(activity)
        }.getOrNull()
    }

    private companion object {
        private const val ENTRY_KEY = "bzzq_advanced_settings"
        private const val ENTRY_TITLE = "\u9ad8\u7ea7\u8bbe\u7f6e"
        private const val ENTRY_SUMMARY = "BBZQ 模块设置"

        private val TARGET_GROUP_KEYS = arrayOf(
            "pref_key_tools_setting",
            "categoryAdvanced",
        )

        private val ANCHOR_KEYS = arrayOf(
            "pref_key_side_center",
            "pref_clear_storage",
        )

        private val NORMAL_PREFERENCE_CLASSES = arrayOf(
            "tv.danmaku.p9138bili.widget.preference.BLPreference",
            "androidx.preference.Preference",
        )

        private val WIDE_PREFERENCE_CLASSES = arrayOf(
            "com.bilibili.p4439app.preferences.settingWide.CornerPreference",
            "com.bilibili.app.preferences.settingWide.CornerPreference",
            "tv.danmaku.p9138bili.widget.preference.BLPreference",
            "androidx.preference.Preference",
        )
    }
}
