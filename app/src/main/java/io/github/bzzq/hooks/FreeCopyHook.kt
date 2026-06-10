package io.github.bzzq.hooks

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import io.github.bzzq.ModuleSettings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.json.JSONObject

/**
 * BiliRoamingX-like copy behavior:
 * - disable original long-press copy
 * - optionally replace it with a selectable dialog
 */
class FreeCopyHook(
    override val targetPackageName: String,
) : AppHook {
    override fun install(
        xposed: XposedInterface,
        packageReady: PackageReadyParam,
        log: (String, Throwable?) -> Unit,
    ) {
        val prefs = xposed.getRemotePreferences(ModuleSettings.PREFS_NAME)
        val classLoader = packageReady.getClassLoader()

        hookCommentCopyMenus(xposed, classLoader, prefs, log)
        hookConversationCopy(xposed, classLoader, prefs, log)
        hookDescCopy(xposed, classLoader, prefs, log)
    }

    private fun hookCommentCopyMenus(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        COPY_MENU_HOLDER_CLASS_NAMES.forEach { className ->
            runCatching {
                val clazz = Class.forName(className, false, classLoader)
                val candidates = clazz.declaredMethods.filter { method ->
                    method.parameterTypes.size >= 3 &&
                        View::class.java.isAssignableFrom(method.parameterTypes.last())
                }
                if (candidates.isEmpty()) return@runCatching

                candidates.forEach { method ->
                    method.isAccessible = true
                    xposed.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                        .intercept { chain ->
                            val disableOriginalCopy = ModuleSettings.isDisableLongPressCopyEnabled(prefs)
                            if (!disableOriginalCopy) return@intercept chain.proceed()

                            val menu = chain.getArg(1)
                            val triggerView = chain.getArg(method.parameterTypes.size - 1) as? View
                            if (!looksLikeCopyAction(menu, triggerView)) return@intercept chain.proceed()

                            if (!ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)) {
                                log("Consumed default comment copy action", null)
                                return@intercept null
                            }

                            val result = chain.proceed()
                            val text = readClipboardText(triggerView?.context) ?: return@intercept result
                            showSelectableDialog(triggerView?.context, text, log)
                            result
                        }
                }
                log("Installed free-copy menu hook for $className", null)
            }.onFailure {
                log("Failed to install free-copy menu hook for $className", it)
            }
        }
    }

    private fun hookConversationCopy(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        runCatching {
            val clazz = Class.forName(CONVERSATION_ACTIVITY_CLASS_NAME, false, classLoader)
            val methods = clazz.declaredMethods.filter { it.parameterTypes.size == 8 }
            methods.forEach { method ->
                method.isAccessible = true
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                    .intercept { chain ->
                        if (!ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)) {
                            return@intercept chain.proceed()
                        }

                        val activity = chain.thisObject as? Activity ?: return@intercept chain.proceed()
                        val message = chain.getArg(1) ?: return@intercept chain.proceed()
                        val popupWindow = chain.getArg(6)
                        val action = chain.getArg(7)?.toString().orEmpty()
                        if (!action.contains("COPY", ignoreCase = true)) {
                            return@intercept chain.proceed()
                        }

                        val text = extractConversationText(message) ?: return@intercept chain.proceed()
                        showSelectableDialog(activity, text, log)
                        dismissPopupWindow(popupWindow)
                        null
                    }
            }
            log("Installed conversation free-copy hook", null)
        }.onFailure {
            log("Failed to install conversation free-copy hook", it)
        }
    }

    private fun hookDescCopy(
        xposed: XposedInterface,
        classLoader: ClassLoader,
        prefs: android.content.SharedPreferences,
        log: (String, Throwable?) -> Unit,
    ) {
        DESC_COPY_CLASS_NAMES.forEach { className ->
            runCatching {
                val clazz = Class.forName(className, false, classLoader)
                clazz.declaredMethods
                    .filter { method ->
                        method.parameterTypes.contentEquals(arrayOf(Boolean::class.javaPrimitiveType, String::class.java))
                    }
                    .forEach { method ->
                        method.isAccessible = true
                        xposed.hook(method)
                            .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                            .intercept { chain ->
                                if (!ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)) {
                                    return@intercept chain.proceed()
                                }
                                val context = findContext(chain.thisObject) ?: return@intercept chain.proceed()
                                val text = chain.getArg(1) as? String ?: return@intercept chain.proceed()
                                if (text.isBlank()) return@intercept chain.proceed()
                                showSelectableDialog(context, text, log)
                                null
                            }
                    }
                log("Installed desc free-copy hook for $className", null)
            }.onFailure {
                log("Failed to install desc free-copy hook for $className", it)
            }
        }
    }

    private fun findContext(target: Any?): Context? {
        when (target) {
            is Context -> return target
            null -> return null
        }
        return runCatching {
            target.javaClass.declaredFields.firstOrNull { Context::class.java.isAssignableFrom(it.type) }
                ?.apply { isAccessible = true }
                ?.get(target) as? Context
        }.getOrNull()
    }

    private fun dismissPopupWindow(popupWindow: Any?) {
        runCatching {
            popupWindow?.javaClass?.getMethod("dismiss")?.invoke(popupWindow)
        }
    }

    private fun extractConversationText(message: Any): String? {
        val contentString = runCatching {
            message.javaClass.methods.firstOrNull { it.name == "getContentString" && it.parameterCount == 0 }
                ?.invoke(message) as? String
        }.getOrNull() ?: return null

        return runCatching {
            val json = JSONObject(contentString)
            json.optString("content").ifEmpty {
                buildString {
                    appendLine(json.optString("title").trim())
                    appendLine(json.optString("text").trim())
                    json.optJSONArray("modules")?.let { modules ->
                        for (index in 0 until modules.length()) {
                            val item = modules.optJSONObject(index) ?: continue
                            appendLine("${item.optString("title")}：${item.optString("detail")}")
                        }
                    }
                }.trim()
            }
        }.getOrNull()
    }

    private fun looksLikeCopyAction(menu: Any?, view: View?): Boolean {
        val menuText = menu?.toString().orEmpty()
        if (menuText.contains("COPY", ignoreCase = true) || menuText.contains("复制")) return true

        return containsText(view) {
            it.contains("复制") || it.contains("拷贝")
        }
    }

    private fun containsText(view: View?, predicate: (String) -> Boolean): Boolean {
        if (view == null) return false
        if (view is TextView) {
            val text = view.text?.toString().orEmpty()
            if (predicate(text)) return true
        }
        val group = view as? android.view.ViewGroup ?: return false
        for (index in 0 until group.childCount) {
            if (containsText(group.getChildAt(index), predicate)) return true
        }
        return false
    }

    private fun readClipboardText(context: Context?): CharSequence? {
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clipData: ClipData = clipboard.primaryClip ?: return null
        if (clipData.itemCount == 0) return null
        return clipData.getItemAt(0).coerceToText(context)?.takeIf { it.isNotBlank() }
    }

    private fun showSelectableDialog(context: Context?, text: CharSequence, log: (String, Throwable?) -> Unit) {
        val activity = context as? Activity ?: return
        activity.runOnUiThread {
            runCatching {
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("自由复制内容")
                    .setMessage(text)
                    .setPositiveButton("分享") { _, _ ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text.toString())
                        }
                        activity.startActivity(Intent.createChooser(intent, "分享"))
                    }
                    .setNeutralButton("复制全部") { _, _ ->
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("bzzq_free_copy", text))
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                dialog.findViewById<TextView>(android.R.id.message)?.apply {
                    setTextIsSelectable(true)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }.onFailure {
                log("Failed to show free-copy dialog", it)
            }
        }
    }

    private companion object {
        private val COPY_MENU_HOLDER_CLASS_NAMES = listOf(
            "com.bilibili.app.comment3.ui.widget.menu.CommentMoreMenuItemHolder",
        )
        private const val CONVERSATION_ACTIVITY_CLASS_NAME =
            "com.bilibili.bplus.im.conversation.ConversationActivity"
        private val DESC_COPY_CLASS_NAMES = listOf(
            "tv.danmaku.bili.ui.video.section.info.VideoDescCopyHelper",
            "com.bilibili.video.story.StoryTextCopyHelper",
            "com.bilibili.app.comm.comment2.helper.TextCopyHelper",
        )
    }
}
