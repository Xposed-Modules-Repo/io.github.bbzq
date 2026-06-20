package io.github.bbzq.feats.hook

import android.view.View
import io.github.bbzq.ModuleSettings
import io.github.bbzq.feats.BaseRoamingHook
import io.github.bbzq.feats.callMethod
import io.github.bbzq.feats.from
import io.github.bbzq.feats.hookAfterMethod
import io.github.bbzq.feats.hookAfterAllMethods

class HomeComponentHideHook(env: io.github.bbzq.feats.RoamingEnv) : BaseRoamingHook(env) {
    private val knownComponents = linkedMapOf<String, String>()

    override fun startHook() {
        if (env.processName != env.packageName) return
        val fragmentClass = ANDROIDX_FRAGMENT.from(classLoader)
        if (fragmentClass == null) {
            log("startHook: HomeComponentHide missing androidx.fragment.app.Fragment")
            return
        }

        var count = 0
        count += env.hookAfterAllMethods(fragmentClass, "onViewCreated") { param ->
            processFragment(param.thisObject)
        }
        count += env.hookAfterMethod(fragmentClass, "onHiddenChanged", Boolean::class.javaPrimitiveType!!) { param ->
            processFragment(param.thisObject)
        }

        if (count == 0) {
            log("startHook: HomeComponentHide no hook point found")
        } else {
            log("startHook: HomeComponentHide methods=$count")
        }
    }

    private fun processFragment(fragment: Any?) {
        if (fragment == null) return
        val component = resolveHomeComponent(fragment) ?: return
        val root = component.callMethod("getView") as? View ?: return
        val className = component.javaClass.name
        if (!isCandidateComponent(className)) return

        saveKnownComponent(className)
        attachPersistentHider(root, className)
        applyVisibility(root, className)
    }

    private fun resolveHomeComponent(fragment: Any): Any? {
        var current: Any? = fragment
        var parent = fragment.callMethod("getParentFragment")
        var guard = 0
        while (current != null && parent != null && guard < 20) {
            guard += 1
            if (isHomeContainer(parent)) return current
            current = parent
            parent = current.callMethod("getParentFragment")
        }
        return null
    }

    private fun isCandidateComponent(className: String): Boolean {
        if (!className.startsWith("com.bilibili") && !className.startsWith("tv.danmaku")) return false
        val classNameLower = className.lowercase()
        if (EXCLUDED_KEYWORDS.any(classNameLower::contains)) return false
        return true
    }

    private fun isHomeContainer(fragment: Any): Boolean {
        val name = fragment.javaClass.name.lowercase()
        return HOME_CONTAINER_KEYWORDS.any(name::contains)
    }

    private fun shouldHide(className: String): Boolean {
        if (ModuleSettings.isHideAllHomeComponentsEnabled(prefs)) return true
        if (!ModuleSettings.isCustomHomeComponentHideEnabled(prefs)) return false
        return className in ModuleSettings.getHiddenHomeComponents(prefs)
    }

    private fun saveKnownComponent(className: String) {
        if (knownComponents.containsKey(className)) return
        val snapshot = ModuleSettings.getKnownHomeComponents(prefs)
            .mapNotNull(::decodeComponent)
            .associateByTo(linkedMapOf(), { it.className }, { it.name })
        if (className in snapshot) {
            knownComponents.putAll(snapshot)
            return
        }

        val name = className.substringAfterLast('.').ifBlank { className }
        knownComponents.clear()
        knownComponents.putAll(snapshot)
        knownComponents[className] = name

        val encoded = knownComponents.entries.mapIndexed { index, entry ->
            encodeComponent(index, entry.value, entry.key)
        }.toMutableSet()
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_KNOWN_HOME_COMPONENTS, encoded)
            .apply()
    }

    private fun attachPersistentHider(root: View, className: String) {
        if (root.getTag(LISTENER_TAG_KEY) != null) return
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            applyVisibility(root, className)
        }
        runCatching {
            root.viewTreeObserver?.addOnGlobalLayoutListener(listener)
            root.setTag(LISTENER_TAG_KEY, listener)
        }.onFailure {
            log("HomeComponentHide failed to attach listener for $className", it)
        }
    }

    private fun applyVisibility(root: View, className: String) {
        root.visibility = if (shouldHide(className)) View.GONE else View.VISIBLE
    }

    private fun encodeComponent(order: Int, name: String, className: String): String =
        listOf(order.toString(), name.sanitizePart(), className.sanitizePart()).joinToString("\t")

    private fun decodeComponent(raw: String): HomeComponentItem? {
        val parts = raw.split('\t', limit = 3)
        if (parts.size != 3) return null
        val order = parts[0].toIntOrNull() ?: return null
        return HomeComponentItem(order, parts[1], parts[2])
    }

    private fun String.sanitizePart(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    private data class HomeComponentItem(
        val order: Int,
        val name: String,
        val className: String,
    )

    private companion object {
        private const val ANDROIDX_FRAGMENT = "androidx.fragment.app.Fragment"
        private const val LISTENER_TAG_KEY = 0x7F0B1120
        private val HOME_CONTAINER_KEYWORDS = listOf(
            "main2.homefragment",
            "homefragmentv2",
        )
        private val EXCLUDED_KEYWORDS = listOf(
            "search",
            "dynamic",
            "history",
            "favorite",
            "space",
            "reply",
            "detail",
        )
    }
}
