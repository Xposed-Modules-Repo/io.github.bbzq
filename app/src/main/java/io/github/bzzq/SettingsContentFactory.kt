package io.github.bzzq

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsContentFactory(
    private val context: Context,
    private val prefs: SharedPreferences,
) {
    private val tagCheckBoxes = mutableMapOf<String, CheckBox>()
    private lateinit var disableLongPressCopySwitch: Switch
    private lateinit var enhanceLongPressCopySwitch: Switch
    private lateinit var storyVideoAdSwitch: Switch
    private lateinit var blockedCountView: TextView
    private var refreshing = false

    fun createScrollView(): ScrollView {
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAGE_BACKGROUND)
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        page.addView(createSectionLabel("账号工具"))
        page.addView(createSectionCard(accountToolRows()))

        page.addView(createSectionLabel("通用功能"))
        page.addView(createSectionCard(generalRows()))

        page.addView(createSectionLabel("竖屏视频"))
        page.addView(createSectionCard(storyRows()))

        return ScrollView(context).apply {
            setBackgroundColor(PAGE_BACKGROUND)
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(
                page,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }.also { refresh() }
    }

    private fun accountToolRows(): List<View> {
        return listOf(
            createActionRow(
                title = "复制 access_key",
                summary = "复制当前登录账号最近一次抓到的 access_key。",
                actionText = "复制",
            ) { copyAccessKey() },
        )
    }

    private fun generalRows(): List<View> {
        return listOf(
            createSwitchRow("跳过开屏广告", "移除启动广告，减少等待时间。", ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED, true),
            createSwitchRow("解锁视频功能", "尝试放开部分试看限制和画质能力。", ModuleSettings.KEY_UNLOCK_VIDEO_FEATURES_ENABLED, true),
            createSwitchRow("视频详情页自动点赞", "进入视频详情页后自动点击点赞按钮。", ModuleSettings.KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED, false),
            createSwitchRow("直播画质修复", "修复直播画质链接异常或切换失败的问题。", ModuleSettings.KEY_FIX_LIVE_QUALITY_URL_ENABLED, false),
            createSwitchRow("跳过小游戏奖励广告", "自动快进或关闭小游戏里的奖励广告。", ModuleSettings.KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED, true),
            createSwitchRow("屏蔽直播预约", "移除视频详情页里的直播预约卡片。", ModuleSettings.KEY_BLOCK_LIVE_RESERVATION_ENABLED, false),
            createSwitchRow("屏蔽直播间效果弹窗", "移除直播间里的评分或调研弹窗。", ModuleSettings.KEY_BLOCK_LIVE_ROOM_QOE_POPUP_ENABLED, false),
            createSwitchRow("去除长按复制", "禁止长按后直接复制到剪贴板，减少误触。", ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED, false) {
                disableLongPressCopySwitch = it
            },
            createSwitchRow("长按自由复制", "需要先开启“去除长按复制”，再弹出可选择文本的窗口。", ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false) {
                enhanceLongPressCopySwitch = it
            },
            createSwitchRow("分享净化", "清理分享链接中的追踪参数。", ModuleSettings.KEY_PURIFY_SHARE_ENABLED, false),
            createSwitchRow("完整数字显示", "在“我的”和空间页显示完整数字，不再缩写成“万”“亿”。", ModuleSettings.KEY_FULL_NUMBER_FORMAT_ENABLED, false),
            createSwitchRow("评论区 GIF 解锁", "恢复评论区 GIF 缩略图播放并去掉角标。", ModuleSettings.KEY_UNLOCK_COMMENT_GIF_ENABLED, false),
        )
    }

    private fun storyRows(): List<View> {
        val rows = mutableListOf<View>()
        rows += createSwitchRow("净化竖屏视频广告", "按标签过滤广告、购物和推广内容。", ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED, false) {
            storyVideoAdSwitch = it
        }
        rows += createInfoRow("已选标签", "勾选后会一起参与过滤。")
        rows += createTagGroup()
        rows += createBlockedCountRow()
        return rows
    }

    private fun createSectionLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#8C8C91"))
            setPadding(dp(4), dp(14), dp(4), dp(8))
        }
    }

    private fun createSectionCard(rows: List<View>): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.WHITE)
            }
            clipToOutline = true
            rows.forEachIndexed { index, row ->
                addView(row)
                if (index != rows.lastIndex) {
                    addView(createDivider())
                }
            }
        }
    }

    private fun createDivider(): View {
        return View(context).apply {
            setBackgroundColor(Color.parseColor("#F1F2F3"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1),
            ).apply {
                marginStart = dp(16)
            }
        }
    }

    private fun createInfoRow(title: String, summary: String): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(TITLE_COLOR)
            })
            addView(TextView(context).apply {
                text = summary
                textSize = 12f
                setTextColor(SUMMARY_COLOR)
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createBlockedCountRow(): View {
        blockedCountView = TextView(context).apply {
            textSize = 12f
            setTextColor(SUMMARY_COLOR)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(TextView(context).apply {
                text = "拦截统计"
                textSize = 15f
                setTextColor(TITLE_COLOR)
            })
            addView(blockedCountView.apply {
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createTagGroup(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            ModuleSettings.storyVideoAdTags.forEach { tag ->
                addView(CheckBox(context).apply {
                    text = tag.label
                    textSize = 14f
                    setTextColor(TITLE_COLOR)
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                    setOnCheckedChangeListener { _, _ ->
                        if (!refreshing) saveSelectedTags()
                    }
                    tagCheckBoxes[tag.key] = this
                })
            }
        }
    }

    private fun createActionRow(
        title: String,
        summary: String,
        actionText: String,
        onClick: () -> Unit,
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            setBackgroundResource(selectableBackground())
            setOnClickListener { onClick() }
            addView(createTextColumn(title, summary), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(context).apply {
                text = actionText
                textSize = 14f
                setTextColor(ACCENT_COLOR)
            })
        }
    }

    private fun createSwitchRow(
        title: String,
        summary: String,
        key: String,
        defaultValue: Boolean,
        onSwitchReady: ((Switch) -> Unit)? = null,
    ): View {
        val switchView = Switch(context).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!refreshing) {
                    prefs.edit().putBoolean(key, isChecked).apply()
                    if (key == ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED || key == ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED) {
                        refresh()
                    }
                }
            }
        }
        onSwitchReady?.invoke(switchView)

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(createTextColumn(title, summary), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(switchView)
        }
    }

    private fun createTextColumn(title: String, summary: String): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, dp(14), 0)
            addView(TextView(context).apply {
                text = title
                textSize = 15f
                setTextColor(TITLE_COLOR)
            })
            addView(TextView(context).apply {
                text = summary
                textSize = 12f
                setTextColor(SUMMARY_COLOR)
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun refresh() {
        refreshing = true

        val storyEnabled = ModuleSettings.isPurifyStoryVideoAdEnabled(prefs)
        val selectedTags = ModuleSettings.getPurifyStoryVideoAdTags(prefs)
        val disableLongPressCopy = ModuleSettings.isDisableLongPressCopyEnabled(prefs)

        storyVideoAdSwitch.isChecked = storyEnabled
        disableLongPressCopySwitch.isChecked = disableLongPressCopy
        enhanceLongPressCopySwitch.isEnabled = disableLongPressCopy

        if (!disableLongPressCopy && prefs.getBoolean(ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false)) {
            prefs.edit().putBoolean(ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false).apply()
        }
        enhanceLongPressCopySwitch.isChecked =
            disableLongPressCopy && ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)

        tagCheckBoxes.forEach { (key, checkBox) ->
            checkBox.isEnabled = storyEnabled
            checkBox.isChecked = key in selectedTags
        }

        blockedCountView.text = "累计拦截 ${prefs.getInt(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT, 0)} 条内容"
        refreshing = false
    }

    private fun saveSelectedTags() {
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS, selectedTagKeys().toMutableSet())
            .apply()
    }

    private fun selectedTagKeys(): Set<String> =
        tagCheckBoxes.filterValues { it.isChecked }.keys.toSet()

    private fun copyAccessKey() {
        val token = prefs.getString(ModuleSettings.KEY_LAST_ACCESS_KEY, null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "未找到 access_key，请先在 Bilibili 内完成一次登录相关请求。", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("access_key", token))
        Toast.makeText(context, "已复制 access_key。", Toast.LENGTH_SHORT).show()
    }

    private fun selectableBackground(): Int {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        private val PAGE_BACKGROUND = Color.parseColor("#F6F7F8")
        private val TITLE_COLOR = Color.parseColor("#18191C")
        private val SUMMARY_COLOR = Color.parseColor("#9499A0")
        private val ACCENT_COLOR = Color.parseColor("#FB7299")
    }
}
