package io.github.bzzq.hooks

import io.github.bzzq.ModuleSettings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method

/**
 * BRX-like unlock for comment GIF thumbnails in reply main/detail responses.
 */
class UnlockCommentGifHook(
    override val targetPackageName: String,
) : AppHook {
    override fun install(
        xposed: XposedInterface,
        packageReady: PackageReadyParam,
        log: (String, Throwable?) -> Unit,
    ) {
        val prefs = xposed.getRemotePreferences(ModuleSettings.PREFS_NAME)
        val classLoader = packageReady.getClassLoader()

        hookMainListReply(xposed, classLoader, prefs, log)
        hookDetailListReply(xposed, classLoader, prefs, log)
        hookReplyAddResponse(xposed, classLoader, prefs, log)
    }

    private fun hookMainListReply(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        runCatching {
            val clazz = Class.forName(MAIN_LIST_REPLY_CLASS_NAME, false, classLoader)
            val methods = clazz.declaredMethods.filter { method ->
                method.parameterCount == 0 && method.name in MAIN_LIST_METHOD_NAMES
            }
            methods.forEach { method ->
                hookMethod(xposed, method, prefs, log) { result ->
                    when (method.name) {
                        "getUpTop", "getAdminTop", "getVoteTop" -> unlockReplyInfo(result, scale = true)
                        "getTopRepliesList", "getRepliesList" -> asIterable(result).forEach {
                            unlockReplyInfo(it, scale = true)
                        }
                    }
                }
            }
            if (methods.isNotEmpty()) {
                log("Installed comment GIF unlock hook for main reply list", null)
            }
        }.onFailure {
            log("Failed to install comment GIF unlock hook for main reply list", it)
        }
    }

    private fun hookDetailListReply(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        runCatching {
            val clazz = Class.forName(DETAIL_LIST_REPLY_CLASS_NAME, false, classLoader)
            val method = clazz.getDeclaredMethod("getRoot")
            hookMethod(xposed, method, prefs, log) { result ->
                unlockReplyInfo(result, scale = false)
            }
            log("Installed comment GIF unlock hook for detail reply list", null)
        }.onFailure {
            log("Failed to install comment GIF unlock hook for detail reply list", it)
        }
    }

    private fun hookReplyAddResponse(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        runCatching {
            val gsonClass = Class.forName(GSON_CLASS_NAME, false, classLoader)
            val methods = gsonClass.declaredMethods.filter { it.name == "fromJson" }
            methods.forEach { method ->
                hookMethod(xposed, method, prefs, log) { result ->
                    unlockPostedReply(result)
                }
            }
            if (methods.isNotEmpty()) {
                log("Installed comment GIF unlock hook for posted-reply response", null)
            }
        }.onFailure {
            log("Failed to install comment GIF unlock hook for posted-reply response", it)
        }
    }

    private fun hookMethod(
        xposed: XposedInterface,
        method: Method,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
        unlock: (Any?) -> Unit,
    ) {
        method.isAccessible = true
        xposed.hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
            .intercept { chain ->
                val result = chain.proceed()
                if (!ModuleSettings.isUnlockCommentGifEnabled(prefs)) {
                    return@intercept result
                }

                runCatching { unlock(result) }
                    .onFailure { log("Failed to unlock comment GIF payload", it) }
                result
            }
    }

    private fun unlockReplyInfo(replyInfo: Any?, scale: Boolean) {
        if (replyInfo == null) return
        val content = callNoArg(replyInfo, "getContent") ?: return
        setPictureScale(content, scale)
        asIterable(callNoArg(content, "getPicturesList")).forEach { picture ->
            if (picture == null) return@forEach
            callBooleanArg(picture, "setPlayGifThumbnail", true)
            callNoArg(picture, "clearTopRightIcon")
        }
    }

    private fun unlockPostedReply(result: Any?) {
        if (result == null) return
        val data = getMemberValue(result, "data") ?: return
        val successAction = getIntValue(data, "success_action", "successAction")
        if (successAction != 0) return

        val reply = getMemberValue(data, "reply") ?: return
        val content = getMemberValue(reply, "content") ?: return
        val pictures = getMemberValue(content, "pictures", "picturesList")
        if (asIterable(pictures).none()) return

        setNumericValue(content, 1.5f, "picture_scale", "pictureScale")
        asIterable(pictures).forEach { picture ->
            if (picture == null) return@forEach
            setBooleanValue(picture, true, "play_gif_thumbnail", "playGifThumbnail")
            clearOrNullify(picture, "top_right_icon", "topRightIcon")
        }
    }

    private fun setPictureScale(content: Any, scale: Boolean) {
        val value = if (scale) 1.5f else 1.0f
        if (callFloatArg(content, "setPictureScale", value)) return
        callDoubleArg(content, "setPictureScale", value.toDouble())
    }

    private fun callNoArg(target: Any, methodName: String): Any? {
        return runCatching {
            target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterCount == 0
            }?.invoke(target)
        }.getOrNull()
    }

    private fun callBooleanArg(target: Any, methodName: String, value: Boolean): Boolean {
        return runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0] == Boolean::class.javaPrimitiveType
            } ?: return false
            method.invoke(target, value)
            true
        }.getOrDefault(false)
    }

    private fun callFloatArg(target: Any, methodName: String, value: Float): Boolean {
        return runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0] == Float::class.javaPrimitiveType
            } ?: return false
            method.invoke(target, value)
            true
        }.getOrDefault(false)
    }

    private fun callDoubleArg(target: Any, methodName: String, value: Double): Boolean {
        return runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0] == Double::class.javaPrimitiveType
            } ?: return false
            method.invoke(target, value)
            true
        }.getOrDefault(false)
    }

    private fun getMemberValue(target: Any, vararg names: String): Any? {
        for (name in names) {
            callNoArg(target, getterName(name))?.let { return it }
            findField(target.javaClass, name)?.let { field ->
                field.isAccessible = true
                return field.get(target)
            }
        }
        return null
    }

    private fun getIntValue(target: Any, vararg names: String): Int? {
        val value = getMemberValue(target, *names) ?: return null
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun setNumericValue(target: Any, value: Float, vararg names: String) {
        for (name in names) {
            if (callFloatArg(target, setterName(name), value)) return
            if (callDoubleArg(target, setterName(name), value.toDouble())) return
            val field = findField(target.javaClass, name) ?: continue
            field.isAccessible = true
            when (field.type) {
                Float::class.javaPrimitiveType, Float::class.javaObjectType -> {
                    field.set(target, value)
                    return
                }
                Double::class.javaPrimitiveType, Double::class.javaObjectType -> {
                    field.set(target, value.toDouble())
                    return
                }
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> {
                    field.set(target, value.toInt())
                    return
                }
            }
        }
    }

    private fun setBooleanValue(target: Any, value: Boolean, vararg names: String) {
        for (name in names) {
            if (callBooleanArg(target, setterName(name), value)) return
            val field = findField(target.javaClass, name) ?: continue
            field.isAccessible = true
            if (field.type == Boolean::class.javaPrimitiveType || field.type == Boolean::class.javaObjectType) {
                field.set(target, value)
                return
            }
        }
    }

    private fun clearOrNullify(target: Any, vararg names: String) {
        for (name in names) {
            if (invokeNoArg(target, clearerName(name))) return
            val field = findField(target.javaClass, name) ?: continue
            field.isAccessible = true
            runCatching {
                when (field.type) {
                    Int::class.javaPrimitiveType -> field.setInt(target, 0)
                    Long::class.javaPrimitiveType -> field.setLong(target, 0L)
                    Float::class.javaPrimitiveType -> field.setFloat(target, 0f)
                    Double::class.javaPrimitiveType -> field.setDouble(target, 0.0)
                    Boolean::class.javaPrimitiveType -> field.setBoolean(target, false)
                    else -> field.set(target, null)
                }
            }.getOrNull() ?: continue
            return
        }
    }

    private fun invokeNoArg(target: Any, methodName: String): Boolean {
        return runCatching {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterCount == 0
            } ?: return false
            method.invoke(target)
            true
        }.getOrDefault(false)
    }

    private fun getterName(fieldName: String): String =
        "get" + fieldName.replaceFirstChar { it.uppercaseChar() }

    private fun setterName(fieldName: String): String =
        "set" + fieldName.replaceFirstChar { it.uppercaseChar() }

    private fun clearerName(fieldName: String): String =
        "clear" + fieldName.replaceFirstChar { it.uppercaseChar() }

    private fun findField(startClass: Class<*>, fieldName: String): java.lang.reflect.Field? {
        var currentClass: Class<*>? = startClass
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    private fun asIterable(value: Any?): Iterable<*> {
        return when (value) {
            is Iterable<*> -> value
            is Array<*> -> value.asIterable()
            else -> emptyList<Any?>()
        }
    }

    private companion object {
        private const val GSON_CLASS_NAME = "com.google.gson.Gson"
        private const val MAIN_LIST_REPLY_CLASS_NAME =
            "com.bapis.bilibili.main.community.reply.v1.MainListReply"
        private const val DETAIL_LIST_REPLY_CLASS_NAME =
            "com.bapis.bilibili.main.community.reply.v1.DetailListReply"
        private val MAIN_LIST_METHOD_NAMES = setOf(
            "getUpTop",
            "getAdminTop",
            "getVoteTop",
            "getTopRepliesList",
            "getRepliesList",
        )
    }
}
