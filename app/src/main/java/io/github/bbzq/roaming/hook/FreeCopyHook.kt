package io.github.bbzq.roaming.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.bbzq.ModuleSettings
import io.github.bbzq.roaming.BaseRoamingHook
import io.github.bbzq.roaming.RoamingEnv
import io.github.bbzq.roaming.allFields
import io.github.bbzq.roaming.allMethods
import io.github.bbzq.roaming.callMethod
import io.github.bbzq.roaming.from
import io.github.bbzq.roaming.hookBefore
import org.json.JSONObject

class FreeCopyHook(env: RoamingEnv) : BaseRoamingHook(env) {
    override fun startHook() {
        var count = hookLongClickListener()
        count += hookConversation()
        log("startHook: FreeCopy, methods=$count")
    }

    private fun hookLongClickListener(): Int {
        val method = View::class.java.getDeclaredMethod(
            "setOnLongClickListener",
            View.OnLongClickListener::class.java,
        )
        env.hookBefore(method) { param ->
            val listener = param.args.firstOrNull() as? View.OnLongClickListener ?: return@hookBefore
            if (listener is CopyLongClickWrapper) return@hookBefore
            param.args[0] = CopyLongClickWrapper(listener)
        }
        return 1
    }

    private fun hookConversation(): Int {
        val activityClass = CONVERSATION_ACTIVITIES.firstNotNullOfOrNull { it.from(classLoader) }
            ?: return 0
        val methods = activityClass.allMethods()
            .filter { it.parameterCount == 8 }
            .toList()
        methods.forEach { method ->
            env.hookBefore(method) { param ->
                if (!isFreeCopyEnabled()) return@hookBefore
                val action = param.args.getOrNull(7)
                if (action?.toString()?.contains("COPY", ignoreCase = true) != true &&
                    action != param.args.firstOrNull()
                ) {
                    return@hookBefore
                }

                val activity = param.thisObject as? Activity ?: return@hookBefore
                val message = param.args.getOrNull(1) ?: return@hookBefore
                val text = extractMessageText(message) ?: return@hookBefore
                showCopyDialog(activity, text)
                param.args.getOrNull(6)?.callMethod("dismiss")
                param.result = null
            }
        }
        return methods.size
    }

    private inner class CopyLongClickWrapper(
        private val original: View.OnLongClickListener,
    ) : View.OnLongClickListener {
        override fun onLongClick(view: View): Boolean {
            val removeDirectCopy = ModuleSettings.isDisableLongPressCopyEnabled(prefs)
            val enhance = removeDirectCopy && ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)
            if (!enhance && !removeDirectCopy) return original.onLongClick(view)

            val text = findText(view) ?: findTextOnTarget(original)
            if (text.isNullOrBlank()) return original.onLongClick(view)
            if (enhance) showCopyDialog(view.context, text)
            return true
        }
    }

    private fun isFreeCopyEnabled(): Boolean =
        ModuleSettings.isDisableLongPressCopyEnabled(prefs) &&
            ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)

    private fun findText(view: View?): CharSequence? {
        if (view is TextView && !view.text.isNullOrBlank() && isCopyCandidate(view)) {
            return view.text
        }
        val group = view as? ViewGroup ?: return null
        COPYABLE_VIEW_IDS.forEach { name ->
            val id = group.resources.getIdentifier(name, "id", group.context.packageName)
            if (id != 0) {
                (group.findViewById<View>(id) as? TextView)
                    ?.text
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }
        for (index in 0 until group.childCount) {
            findText(group.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun isCopyCandidate(view: TextView): Boolean {
        val idName = runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()
        if (idName in COPYABLE_VIEW_IDS) return true
        val className = view.javaClass.name
        if (COPYABLE_CLASS_TOKENS.any { className.contains(it, ignoreCase = true) }) return true
        return view.text.length >= MIN_TEXT_LENGTH
    }

    private fun findTextOnTarget(target: Any?): CharSequence? {
        if (target == null) return null
        return target.javaClass.allFields()
            .mapNotNull { field -> runCatching { field.get(target) }.getOrNull() }
            .firstNotNullOfOrNull { value ->
                when (value) {
                    is SpannableStringBuilder -> value
                    is CharSequence -> value.takeIf { it.isNotBlank() }
                    else -> null
                }
            }
    }

    private fun extractMessageText(message: Any): String? {
        val raw = message.callMethod("getContentString") as? String ?: return null
        return runCatching {
            val json = JSONObject(raw)
            json.optString("content").ifBlank {
                buildList {
                    json.optString("title").takeIf(String::isNotBlank)?.let(::add)
                    json.optString("text").takeIf(String::isNotBlank)?.let(::add)
                    json.optJSONArray("modules")?.let { modules ->
                        for (index in 0 until modules.length()) {
                            val item = modules.optJSONObject(index) ?: continue
                            listOf(item.optString("title"), item.optString("detail"))
                                .filter(String::isNotBlank)
                                .joinToString("：")
                                .takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }
                }.joinToString("\n")
            }
        }.getOrNull()
    }

    private fun showCopyDialog(context: Context?, text: CharSequence) {
        val activity = context as? Activity ?: return
        activity.runOnUiThread {
            val themeId = activity.resources.getIdentifier("AppTheme.Dialog.Alert", "style", activity.packageName)
            val builder = if (themeId != 0) {
                AlertDialog.Builder(activity, themeId)
            } else {
                AlertDialog.Builder(activity)
            }
            val dialog = builder
                .setTitle("自由复制内容")
                .setMessage(text)
                .setPositiveButton("分享") { _, _ ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text.toString())
                    }
                    activity.startActivity(Intent.createChooser(intent, "分享文本"))
                }
                .setNeutralButton("复制全部") { _, _ ->
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("bbzq_copy", text))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            dialog.findViewById<TextView>(android.R.id.message)?.apply {
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private companion object {
        private const val MIN_TEXT_LENGTH = 12
        private val CONVERSATION_ACTIVITIES = listOf(
            "com.bilibili.bplus.p5162im.conversation.ConversationActivity",
            "com.bilibili.bplus.im.conversation.ConversationActivity",
        )
        private val COPYABLE_CLASS_TOKENS = listOf(
            "ExpandableTextView",
            "RichText",
            "EllipsizingTextView",
            "Comment",
        )
        private val COPYABLE_VIEW_IDS = listOf(
            "message",
            "comment_message",
            "dy_card_text",
            "dy_opus_paragraph_desc",
            "dy_opus_paragraph_title",
            "dy_opus_copy_right_id",
            "dy_opus_paragraph_text",
        )
    }
}
