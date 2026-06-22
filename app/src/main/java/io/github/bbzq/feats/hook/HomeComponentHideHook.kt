package io.github.bbzq.feats.hook

import android.view.View
import io.github.bbzq.ModuleSettings
import io.github.bbzq.feats.BaseRoamingHook
import io.github.bbzq.feats.callMethod
import io.github.bbzq.feats.allFields
import io.github.bbzq.feats.from
import io.github.bbzq.feats.hookAfter
import io.github.bbzq.feats.hookAfterMethod
import io.github.bbzq.feats.hookAfterAllMethods
import io.github.bbzq.feats.methodsNamed
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class HomeComponentHideHook(env: io.github.bbzq.feats.RoamingEnv) : BaseRoamingHook(env) {
    private val knownComponents = linkedMapOf<String, String>()
    private var homeRecommendFieldWriteFailedLogged = false

    override fun startHook() {
        if (env.processName != env.packageName) return
        ModuleSettings.refreshKnownHomeRecommendItemsCache(prefs)
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
        count += hookHomeComponentCatalog()
        count += hookHomeRecommendItems()

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

    private fun hookHomeComponentCatalog(): Int {
        var count = 0
        BASE_HOME_FRAGMENT_CLASSES.mapNotNull { it.from(classLoader) }.forEach { baseHomeFragmentClass ->
            listOf("onAttach", "onCreate", "onCreateView", "onViewCreated", "onResume").forEach { methodName ->
                count += env.hookAfterAllMethods(baseHomeFragmentClass, methodName) { param ->
                    runCatching {
                        collectHomeComponentCatalog(param.thisObject)
                    }.onFailure {
                        log("HomeComponentHide component catalog hook failed at ${baseHomeFragmentClass.name}.$methodName", it)
                    }
                }
            }
        }
        return count
    }

    private fun collectHomeComponentCatalog(fragment: Any?) {
        if (fragment == null) return
        val fragmentName = fragment.javaClass.name
        if (!BASE_HOME_FRAGMENT_NAMES.any { baseName ->
                fragmentName.equals(baseName, ignoreCase = true) ||
                    fragmentName.contains(baseName, ignoreCase = true)
            }) return

        val components = runCatching {
            fragment.callMethod("m169957gf") as? List<*>
        }.getOrNull().orEmpty()

        components.forEach { component ->
            val className = component?.javaClass?.name ?: return@forEach
            if (className.isBlank()) return@forEach
            saveKnownComponent(className)
        }
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

    private fun hookHomeRecommendItems(): Int {
        val responseClasses = PEGASUS_RESPONSE_CLASSES.mapNotNull { it.from(classLoader) }
        val holderDataClass = PEGASUS_HOLDER_DATA.from(classLoader) ?: return 0
        if (responseClasses.isEmpty()) return 0

        val getHolderType = holderDataClass.methodsNamed("getHolderType")
            .firstOrNull {
                it.parameterCount == 0 &&
                    it.returnType == String::class.java &&
                    !Modifier.isStatic(it.modifiers) &&
                    !Modifier.isAbstract(it.modifiers)
            }
            ?: return 0
        val getBizType = holderDataClass.methodsNamed("getBizType")
            .firstOrNull {
                it.parameterCount == 0 &&
                    !Modifier.isStatic(it.modifiers) &&
                    !Modifier.isAbstract(it.modifiers)
            }
        var count = 0
        responseClasses.forEach { responseClass ->
            val getItems = responseClass.methodsNamed("getItems")
                .firstOrNull {
                    it.parameterCount == 0 &&
                        List::class.java.isAssignableFrom(it.returnType) &&
                        !Modifier.isStatic(it.modifiers) &&
                        !Modifier.isAbstract(it.modifiers)
                }
                ?: return@forEach
            val itemsField = responseClass.allFields()
                .filter { List::class.java.isAssignableFrom(it.type) }
                .singleOrNull()

            env.hookAfter(getItems) { param ->
                val items = param.result as? List<*> ?: return@hookAfter
                val hiddenKeys = ModuleSettings.getHiddenHomeRecommendItems(prefs)
                val enabled = ModuleSettings.isCustomHomeRecommendFilterEnabled(prefs)
                val filtered = ArrayList<Any?>(items.size)
                val knownItems = linkedSetOf<String>()
                var removed = 0

                items.forEachIndexed { index, item ->
                    val entry = item?.extractRecommendItemEntry(index, getHolderType, getBizType)
                    if (entry == null) {
                        filtered += item
                        return@forEachIndexed
                    }

                    knownItems += encodeRecommendItem(entry.order, entry.key, entry.bizType, entry.className)
                    if (enabled && entry.key in hiddenKeys) {
                        removed += 1
                    } else {
                        filtered += item
                    }
                }

                saveKnownHomeRecommendItems(knownItems)
                if (removed == 0) return@hookAfter

                param.result = filtered
                writeBackFilteredItems(param.thisObject, itemsField, filtered)
                log(
                    "HomeComponentHide removed $removed home item(s) " +
                        "from ${getItems.declaringClass.name}.${getItems.name}",
                )
            }
            count += 1
        }
        return count
    }

    private fun Any.extractRecommendItemEntry(
        index: Int,
        getHolderType: Method,
        getBizType: Method?,
    ): HomeRecommendItem? {
        val className = javaClass.name
        val holderType = invokeString(getHolderType, this)
            ?.takeIf { it.isNotBlank() }
            ?: className.substringAfterLast('.').ifBlank { className }
        if (holderType.isBlank()) return null
        return HomeRecommendItem(
            order = index,
            key = holderType,
            bizType = invokeString(getBizType, this).orEmpty(),
            className = className,
        )
    }

    private fun invokeString(method: Method?, target: Any): String? =
        runCatching { method?.invoke(target)?.toString() }.getOrNull()

    private fun saveKnownHomeRecommendItems(items: Set<String>) {
        if (items.isEmpty()) return
        val snapshot = ModuleSettings.getKnownHomeRecommendItems(prefs)
        if (snapshot == items) return
        ModuleSettings.cacheKnownHomeRecommendItems(items)
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_KNOWN_HOME_RECOMMEND_ITEMS, items.toMutableSet())
            .apply()
    }

    private fun writeBackFilteredItems(target: Any?, field: Field?, items: List<Any?>) {
        if (target == null || field == null) return
        runCatching {
            field.set(target, items)
        }.onFailure { throwable ->
            if (!homeRecommendFieldWriteFailedLogged) {
                homeRecommendFieldWriteFailedLogged = true
                log("HomeComponentHide could not update PegasusResponse items field", throwable)
            }
        }
    }

    private fun encodeRecommendItem(order: Int, key: String, bizType: String, className: String): String =
        listOf(order.toString(), key, bizType, className)
            .joinToString("\t") { it.sanitizePart() }

    private fun String.sanitizePart(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

    private fun encodeComponent(order: Int, name: String, className: String): String =
        listOf(order.toString(), name.sanitizePart(), className.sanitizePart()).joinToString("\t")

    private fun decodeComponent(raw: String): HomeComponentItem? {
        val parts = raw.split('\t', limit = 3)
        if (parts.size != 3) return null
        val order = parts[0].toIntOrNull() ?: return null
        return HomeComponentItem(order, parts[1], parts[2])
    }

    private data class HomeComponentItem(
        val order: Int,
        val name: String,
        val className: String,
    )

    private data class HomeRecommendItem(
        val order: Int,
        val key: String,
        val bizType: String,
        val className: String,
    )

    private companion object {
        private const val ANDROIDX_FRAGMENT = "androidx.fragment.app.Fragment"
        private val BASE_HOME_FRAGMENT_CLASSES = arrayOf(
            "tv.danmaku.bili.home.tab.page.BaseHomeFragment",
            "tv.danmaku.p9138bili.p9170home.p9173tab.p9174page.BaseHomeFragment",
        )
        private val BASE_HOME_FRAGMENT_NAMES = BASE_HOME_FRAGMENT_CLASSES.map { it.lowercase() }
        private val PEGASUS_RESPONSE_CLASSES = arrayOf(
            "com.bilibili.pegasus.data.base.PegasusResponse",
            "com.bilibili.pegasus.p5730data.p5731base.PegasusResponse",
            "com.bilibili.pegasus.p5730data.request.PegasusResponseWrapper",
        )
        private const val PEGASUS_HOLDER_DATA = "com.bilibili.pegasus.PegasusHolderData"
        private const val LISTENER_TAG_KEY = 0x7F0B1120
        private val HOME_CONTAINER_KEYWORDS = listOf(
            "basehomefragment",
            "homefragment",
            "homeframe",
            "pagebuildcomponent",
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
