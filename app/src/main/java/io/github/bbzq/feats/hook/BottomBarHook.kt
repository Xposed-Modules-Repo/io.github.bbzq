package io.github.bbzq.feats.hook

import io.github.bbzq.ModuleSettings
import io.github.bbzq.feats.BaseRoamingHook
import io.github.bbzq.feats.RoamingEnv
import io.github.bbzq.feats.from
import io.github.bbzq.feats.getObjectField
import io.github.bbzq.feats.hookAfter
import io.github.bbzq.feats.methodOrNull
import java.lang.reflect.Method

class BottomBarHook(env: RoamingEnv) : BaseRoamingHook(env) {
    override fun startHook() {
        val parserMethods = findParserMethods()

        parserMethods.forEach { method ->
            env.hookAfter(method) { param ->
                runCatching { dispatch(param.result) }
                    .onFailure {
                        log("Bottom bar processor failed at ${method.declaringClass.name}.${method.name}", it)
                    }
            }
        }

        if (parserMethods.isEmpty()) {
            log("startHook: BottomBar, no parser found")
        } else {
            log("startHook: BottomBar, methods=${parserMethods.size}")
        }
    }

    private fun findParserMethods(): List<Method> = buildList {
        addFastJsonMethod(
            className = "com.alibaba.fastjson.JSON",
            methodName = "parseObject",
            parameterTypeNames = arrayOf(
                "java.lang.String",
                "java.lang.reflect.Type",
                "int",
                "[Lcom.alibaba.fastjson.parser.Feature;",
            ),
        )
        addFastJsonMethod(
            className = "com.alibaba.fastjson2.JSON",
            methodName = "parseObject",
            parameterTypeNames = arrayOf(
                "java.lang.String",
                "java.lang.reflect.Type",
                "[Lcom.alibaba.fastjson2.JSONReader\$Feature;",
            ),
        )
        addGsonMethod(
            methodName = "fromJson",
            parameterTypeNames = arrayOf(
                "java.lang.String",
                "java.lang.reflect.Type",
            ),
        )
        addGsonMethod(
            methodName = "fromJson",
            parameterTypeNames = arrayOf(
                "java.io.Reader",
                "java.lang.reflect.Type",
            ),
        )
    }.distinctBy(Method::toGenericString)

    private fun MutableList<Method>.addFastJsonMethod(
        className: String,
        methodName: String,
        parameterTypeNames: Array<String>,
    ) {
        val parameterTypes = resolveParameterTypes(parameterTypeNames) ?: return
        val method = className.from(classLoader)
            ?.methodOrNull(methodName, *parameterTypes)
            ?: return
        add(method)
    }

    private fun MutableList<Method>.addGsonMethod(
        methodName: String,
        parameterTypeNames: Array<String>,
    ) {
        val parameterTypes = resolveParameterTypes(parameterTypeNames) ?: return
        val method = "com.google.gson.Gson".from(classLoader)
            ?.methodOrNull(methodName, *parameterTypes)
            ?: return
        add(method)
    }

    private fun resolveParameterTypes(typeNames: Array<String>): Array<Class<*>>? =
        typeNames.map(::resolveParameterType).takeIf { it.none { type -> type == null } }
            ?.filterNotNull()
            ?.toTypedArray()

    private fun resolveParameterType(typeName: String): Class<*>? = when (typeName) {
        "int" -> Int::class.javaPrimitiveType
        else -> typeName.from(classLoader)
    }

    private fun dispatch(rawResult: Any?) {
        val result = unwrap(rawResult) ?: return
        if (!result.isTabResponse()) return

        val tabData = result.readAny("tabData") ?: return
        val bottom = tabData.readMutableList("bottom") ?: return
        val hiddenIds = ModuleSettings.getHiddenBottomBarItems(prefs)
        val enabled = ModuleSettings.isCustomBottomBarEnabled(prefs)
        val knownItems = linkedSetOf<String>()

        bottom.removeAll { item ->
            if (item == null) return@removeAll false
            val id = item.readString("tabId", "f498840tabId", "id")
                ?.takeIf { it.isNotBlank() }
                ?: return@removeAll false
            val name = item.readString("name", "f498841name", "title").orEmpty()
            val uri = item.readString("uri", "f498844uri", "jumpUrl").orEmpty()

            knownItems += encodeBottomItem(
                order = knownItems.size,
                id = id,
                name = name.ifBlank { id },
                uri = uri,
            )
            enabled && id in hiddenIds
        }

        saveKnownItems(knownItems)
    }

    private fun unwrap(result: Any?): Any? {
        if (result == null) return null
        val className = result.javaClass.name
        return if (className.endsWith("GeneralResponse") || className.endsWith("RxGeneralResponse")) {
            result.getObjectField("data")
        } else {
            result
        }
    }

    private fun Any.isTabResponse(): Boolean {
        val className = javaClass.name
        return className in TAB_RESPONSE_CLASSES || className.endsWith("MainResourceManager\$TabResponse")
    }

    private fun Any.readAny(vararg names: String): Any? =
        names.firstNotNullOfOrNull { name -> getObjectField(name) }

    private fun Any.readString(vararg names: String): String? =
        readAny(*names)?.toString()

    @Suppress("UNCHECKED_CAST")
    private fun Any.readMutableList(vararg names: String): MutableList<Any?>? {
        names.forEach { name ->
            val value = getObjectField(name) as? MutableList<Any?>
            if (value != null) return value
        }
        return null
    }

    private fun saveKnownItems(items: Set<String>) {
        if (items.isEmpty()) return
        val oldItems = ModuleSettings.getKnownBottomBarItems(prefs)
        if (oldItems == items) return
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_KNOWN_BOTTOM_BAR_ITEMS, items.toMutableSet())
            .apply()
    }

    private fun encodeBottomItem(order: Int, id: String, name: String, uri: String): String =
        listOf(order.toString(), id, name, uri)
            .joinToString(ITEM_SEPARATOR) { it.sanitizeItemPart() }

    private fun String.sanitizeItemPart(): String =
        replace('\t', ' ')
            .replace('\n', ' ')
            .replace('\r', ' ')

    private companion object {
        private const val ITEM_SEPARATOR = "\t"
        private val TAB_RESPONSE_CLASSES = setOf(
            "tv.danmaku.bili.ui.main2.resource.MainResourceManager\$TabResponse",
            "tv.danmaku.p9138bili.p9228ui.main2.resource.MainResourceManager\$TabResponse",
        )
    }
}

