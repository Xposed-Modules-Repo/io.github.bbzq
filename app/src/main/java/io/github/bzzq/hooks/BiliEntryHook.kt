package io.github.bzzq.hooks

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.bzzq.InAppSettingsDialog

class BiliEntryHook(
    targetPackageName: String,
) : BaseHook(targetPackageName) {
    override fun startHook() {
        runCatching {
            val onResume = Activity::class.java.getDeclaredMethod("onResume")
            context.xposed.hook(onResume).intercept { chain ->
                val result = chain.proceed()
                scheduleAttach(chain.thisObject as? Activity, context.log)
                result
            }

            val onWindowFocusChanged =
                Activity::class.java.getDeclaredMethod("onWindowFocusChanged", Boolean::class.javaPrimitiveType)
            context.xposed.hook(onWindowFocusChanged).intercept { chain ->
                val result = chain.proceed()
                if (chain.getArg(0) == true) {
                    scheduleAttach(chain.thisObject as? Activity, context.log)
                }
                result
            }

            context.log("Installed settings-page entry hooks for ${context.packageName}", null)
        }.onFailure {
            context.log("Failed to install settings-page entry hooks", it)
        }
    }

    private fun scheduleAttach(activity: Activity?, log: (String, Throwable?) -> Unit) {
        val safeActivity = activity ?: return
        val decor = safeActivity.window?.decorView ?: return
        ATTACH_DELAYS_MS.forEach { delay ->
            decor.postDelayed({
                runCatching { attachEntry(safeActivity, log) }
                    .onFailure { log("Failed to attach advanced settings entry", it) }
            }, delay)
        }
    }

    private fun attachEntry(activity: Activity, log: (String, Throwable?) -> Unit) {
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: activity.window?.decorView as? ViewGroup
            ?: return
        if (!looksLikeSettingsPage(activity, contentRoot)) {
            removeEntry(contentRoot, SETTINGS_ENTRY_TAG)
            removeEntry(contentRoot, SETTINGS_OVERLAY_TAG)
            return
        }

        val installedInline = installSettingsRow(activity, contentRoot, log)
        if (installedInline) {
            removeEntry(contentRoot, SETTINGS_OVERLAY_TAG)
            return
        }

        installOverlayEntry(activity, contentRoot, log)
    }

    private fun installSettingsRow(activity: Activity, root: ViewGroup, log: (String, Throwable?) -> Unit): Boolean {
        if (root.findViewWithTag<View>(SETTINGS_ENTRY_TAG) != null) return true

        val anchor = findAnchorRow(root)
        val parent = anchor?.parent as? ViewGroup ?: findBestSettingsContainer(root) ?: return false
        val entry = createSettingsEntryView(activity).apply { tag = SETTINGS_ENTRY_TAG }

        if (anchor != null) {
            val index = parent.indexOfChild(anchor)
            parent.addView(entry, index.coerceAtLeast(0))
            log("Inserted inline advanced settings entry", null)
            return true
        }

        parent.addView(entry, 0)
        log("Inserted inline advanced settings entry at container top", null)
        return true
    }

    private fun installOverlayEntry(activity: Activity, root: ViewGroup, log: (String, Throwable?) -> Unit) {
        if (root.findViewWithTag<View>(SETTINGS_OVERLAY_TAG) != null) return

        val entry = createOverlayEntryView(activity).apply { tag = SETTINGS_OVERLAY_TAG }
        root.addView(entry, createOverlayLayoutParams(activity, root))
        entry.bringToFront()
        log("Inserted fallback advanced settings entry for settings page", null)
    }

    private fun removeEntry(root: ViewGroup, tag: String) {
        root.findViewWithTag<View>(tag)?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }

    private fun createSettingsEntryView(activity: Activity): View {
        val titleLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleLayout.addView(TextView(activity).apply {
            text = ENTRY_TITLE
            textSize = 16f
            setTextColor(Color.parseColor("#18191C"))
        })
        titleLayout.addView(TextView(activity).apply {
            text = ENTRY_SUMMARY
            textSize = 13f
            setTextColor(Color.parseColor("#9499A0"))
            setPadding(0, dp(activity, 4), 0, 0)
        })

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            setBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            val outValue = android.util.TypedValue()
            activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            addView(titleLayout)
            addView(TextView(activity).apply {
                text = "进入"
                textSize = 13f
                setTextColor(Color.parseColor("#FB7299"))
            })
            setOnClickListener { showSettingsDialog(activity) { _, _ -> } }
        }
    }

    private fun createOverlayEntryView(activity: Activity): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(activity, 14).toFloat()
            }
            elevation = dp(activity, 8).toFloat()
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            isClickable = true
            isFocusable = true
            addView(
                LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(activity).apply {
                        text = ENTRY_TITLE
                        textSize = 16f
                        setTextColor(Color.parseColor("#18191C"))
                    })
                    addView(TextView(activity).apply {
                        text = ENTRY_SUMMARY
                        textSize = 13f
                        setTextColor(Color.parseColor("#9499A0"))
                        setPadding(0, dp(activity, 4), 0, 0)
                    })
                },
            )
            addView(TextView(activity).apply {
                text = "进入"
                textSize = 13f
                setTextColor(Color.parseColor("#FB7299"))
            })
            setOnClickListener { showSettingsDialog(activity) { _, _ -> } }
        }
    }

    private fun createOverlayLayoutParams(activity: Activity, root: ViewGroup): ViewGroup.MarginLayoutParams {
        val params =
            if (root is FrameLayout) {
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.TOP
                }
            } else {
                ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        return params.apply {
            leftMargin = dp(activity, 12)
            topMargin = dp(activity, 12)
            rightMargin = dp(activity, 12)
        }
    }

    private fun showSettingsDialog(activity: Activity, log: (String, Throwable?) -> Unit) {
        runCatching {
            InAppSettingsDialog.show(activity)
        }.onFailure {
            log("Failed to show in-app settings dialog", it)
        }
    }

    private fun looksLikeSettingsPage(activity: Activity, root: View): Boolean {
        val className = activity.javaClass.name.lowercase()
        if ("setting" in className || "preference" in className) return true
        if (activity.javaClass.name.startsWith(SETTINGS_PACKAGE_PREFIX)) return true

        val matchedMarkers = SETTINGS_MARKERS.count { marker ->
            findTextView(root) { text -> text.contains(marker) } != null
        }
        if (matchedMarkers >= 2) return true
        if (matchedMarkers >= 1 && findTextView(root) { text -> text.contains("设置") } != null) return true

        return findAnchorRow(root) != null
    }

    private fun findAnchorRow(view: View): View? {
        val titleView = findTextView(view) { text ->
            ANCHOR_TEXTS.any { anchor -> text.contains(anchor) }
        } ?: return null

        var current: View? = titleView
        while (current != null) {
            val parent = current.parent as? ViewGroup ?: break
            if (parent.isVerticalContainer() && parent.indexOfChild(current) >= 0) {
                return current
            }
            current = parent
        }
        return null
    }

    private fun findBestSettingsContainer(view: View): ViewGroup? {
        if (view !is ViewGroup) return null
        if (view.isVerticalContainer() && view.childCount >= 2 && countTextDrivenChildren(view) >= 2) {
            return view
        }
        for (index in 0 until view.childCount) {
            findBestSettingsContainer(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun countTextDrivenChildren(group: ViewGroup): Int {
        var count = 0
        for (index in 0 until group.childCount) {
            if (containsAnyText(group.getChildAt(index))) count++
        }
        return count
    }

    private fun containsAnyText(view: View): Boolean {
        if (view is TextView && !view.text.isNullOrBlank()) return true
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                if (containsAnyText(view.getChildAt(index))) return true
            }
        }
        return false
    }

    private fun findTextView(view: View, predicate: (String) -> Boolean): TextView? {
        if (view is TextView) {
            val text = view.text?.toString().orEmpty()
            if (predicate(text)) return view
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findTextView(view.getChildAt(index), predicate)?.let { return it }
            }
        }
        return null
    }

    private fun ViewGroup.isVerticalContainer(): Boolean =
        this is LinearLayout && orientation == LinearLayout.VERTICAL

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    private companion object {
        private const val ENTRY_TITLE = "高级设置"
        private const val ENTRY_SUMMARY = "bzzq 模块设置"
        private const val SETTINGS_PACKAGE_PREFIX = "com.bilibili.app.comm.setting."

        private val ATTACH_DELAYS_MS = longArrayOf(0L, 120L, 360L, 720L, 1200L, 2000L)
        private val SETTINGS_MARKERS = listOf(
            "关于",
            "清理缓存",
            "推荐设置",
            "隐私权限设置",
            "青少年守护",
        )
        private val ANCHOR_TEXTS = listOf(
            "关于哔哩哔哩",
            "关于",
            "隐私权限设置",
            "青少年守护",
            "清理缓存",
            "推荐设置",
        )

        private const val SETTINGS_ENTRY_TAG = "bzzq_settings_entry"
        private const val SETTINGS_OVERLAY_TAG = "bzzq_settings_overlay_entry"
    }
}
