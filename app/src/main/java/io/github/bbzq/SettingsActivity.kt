package io.github.bbzq

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {
    private val prefs by lazy {
        val base = getSharedPreferences(ModuleSettings.PREFS_NAME, MODE_PRIVATE)
        ReadableModulePreferences(this, base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        RuntimeEnvironmentInfo.applyRuntimeSnapshotFromIntent(intent, prefs)

        val page = intent.getStringExtra(EXTRA_PAGE) ?: PAGE_ROOT
        val toolbar = createToolbar(page)
        val content = SettingsContentFactory(
            context = this,
            prefs = prefs,
            page = page,
            openPage = { targetPage ->
                ModuleSettingsNavigator.open(
                    context = this,
                    runtimeValues = intent.getBundleExtra(RuntimeEnvironmentInfo.EXTRA_RUNTIME_VALUES),
                    page = targetPage,
                )
            },
        ).createScrollView()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F6F7F8"))
            addView(toolbar)
            addView(
                content,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        setContentView(root)
        applyWindowInsets(root, toolbar, content)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        RuntimeEnvironmentInfo.applyRuntimeSnapshotFromIntent(intent, prefs)
        recreate()
    }

    private fun createToolbar(page: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            elevation = dp(2).toFloat()

            addView(TextView(this@SettingsActivity).apply {
                text = toolbarTitle(page)
                textSize = 20f
                setTextColor(Color.parseColor("#18191C"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(this@SettingsActivity).apply {
                text = "完成"
                textSize = 15f
                setTextColor(Color.parseColor("#FB7299"))
                isClickable = true
                isFocusable = true
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { finish() }
            })
        }
    }

    private fun toolbarTitle(page: String): String = when (page) {
        PAGE_SKIP_VIDEO_AD_SWITCH -> "空降助手功能开关"
        PAGE_SKIP_VIDEO_AD_CATEGORY -> "空降助手分类设定"
        else -> "BBZQ 设置"
    }

    private fun applyWindowInsets(
        root: LinearLayout,
        toolbar: LinearLayout,
        content: ScrollView,
    ) {
        val toolbarLeft = toolbar.paddingLeft
        val toolbarTop = toolbar.paddingTop
        val toolbarRight = toolbar.paddingRight
        val toolbarBottom = toolbar.paddingBottom
        val contentLeft = content.paddingLeft
        val contentTop = content.paddingTop
        val contentRight = content.paddingRight
        val contentBottom = content.paddingBottom

        root.setOnApplyWindowInsetsListener { _, insets ->
            val safeInsets =
                insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            toolbar.setPadding(
                toolbarLeft,
                toolbarTop + safeInsets.top,
                toolbarRight,
                toolbarBottom,
            )
            content.setPadding(
                contentLeft,
                contentTop,
                contentRight,
                contentBottom + safeInsets.bottom,
            )
            insets
        }
        root.requestApplyInsets()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PAGE = "settings_page"
        const val PAGE_ROOT = "root"
        const val PAGE_SKIP_VIDEO_AD_SWITCH = "skip_video_ad_switch"
        const val PAGE_SKIP_VIDEO_AD_CATEGORY = "skip_video_ad_category"
    }
}
