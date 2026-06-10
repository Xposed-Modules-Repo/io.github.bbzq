package io.github.bzzq.hooks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.bzzq.ModuleSettings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Runtime fallback for BRX-like "make text selectable" behavior.
 */
class SelectableTextHook(
    override val targetPackageName: String,
) : AppHook {
    override fun install(
        xposed: XposedInterface,
        packageReady: PackageReadyParam,
        log: (String, Throwable?) -> Unit,
    ) {
        val prefs = xposed.getRemotePreferences(ModuleSettings.PREFS_NAME)
        runCatching {
            val activityClass = Activity::class.java
            val onResume = activityClass.getDeclaredMethod("onResume")
            xposed.hook(onResume)
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .intercept { chain ->
                    val result = chain.proceed()
                    if (!ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)) {
                        return@intercept result
                    }

                    val activity = chain.thisObject as? Activity ?: return@intercept result
                    activity.window?.decorView?.post {
                        runCatching { enableSelectableText(activity.window?.decorView, activity) }
                            .onFailure { log("Failed to mark views selectable", it) }
                    }
                    result
                }
            log("Installed selectable-text runtime hook", null)
        }.onFailure {
            log("Failed to install selectable-text runtime hook", it)
        }
    }

    private fun enableSelectableText(root: View?, activity: Activity) {
        if (root == null) return
        if (root is TextView && shouldMakeSelectable(root, activity)) {
            root.setTextIsSelectable(true)
            root.isLongClickable = true
        }
        val group = root as? ViewGroup ?: return
        for (index in 0 until group.childCount) {
            enableSelectableText(group.getChildAt(index), activity)
        }
    }

    private fun shouldMakeSelectable(textView: TextView, activity: Activity): Boolean {
        val idName = resolveIdName(textView, activity) ?: ""
        if (idName in COPYABLE_TEXT_VIEW_ID_NAMES) return true
        return activity.javaClass.name == CONVERSATION_ACTIVITY_CLASS_NAME
    }

    private fun resolveIdName(view: View, activity: Activity): String? {
        val id = view.id
        if (id == View.NO_ID) return null
        return runCatching { activity.resources.getResourceEntryName(id) }.getOrNull()
    }

    private companion object {
        private const val CONVERSATION_ACTIVITY_CLASS_NAME =
            "com.bilibili.bplus.im.conversation.ConversationActivity"
        private val COPYABLE_TEXT_VIEW_ID_NAMES = setOf(
            "message",
            "comment_message",
            "dy_card_text",
            "dy_opus_paragraph_desc",
            "dy_opus_paragraph_title",
            "dy_opus_copy_right_id",
            "dy_opus_paragraph_text",
            "season_title",
            "tv_origin_name",
            "tv_intro",
            "tv_actor_name",
            "tv_actor_name_eng",
        )
    }
}
