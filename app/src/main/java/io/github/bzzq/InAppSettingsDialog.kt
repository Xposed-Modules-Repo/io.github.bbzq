package io.github.bzzq

import android.app.Activity
import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

object InAppSettingsDialog {
    fun show(
        activity: Activity,
        prefs: SharedPreferences = ModuleSettingsBridge.instance,
    ) {
        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F6F7F8"))
            addView(createToolbar(activity) { dialog.dismiss() })
            addView(
                SettingsContentFactory(activity, prefs).createScrollView(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
        dialog.show()
    }

    private fun createToolbar(activity: Activity, onClose: () -> Unit): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            elevation = dp(activity, 2).toFloat()

            addView(TextView(activity).apply {
                text = "高级设置"
                textSize = 20f
                setTextColor(Color.parseColor("#18191C"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(activity).apply {
                text = "关闭"
                textSize = 15f
                setTextColor(Color.parseColor("#FB7299"))
                isClickable = true
                isFocusable = true
                setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4))
                setOnClickListener { onClose() }
            })
        }
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
