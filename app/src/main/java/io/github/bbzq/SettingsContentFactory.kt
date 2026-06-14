package io.github.bbzq

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

        page.addView(createSectionLabel("分享与链接"))
        page.addView(createSectionCard(shareRows()))

        page.addView(createSectionLabel("复制增强"))
        page.addView(createSectionCard(copyRows()))

        page.addView(createSectionLabel("启动净化"))
        page.addView(createSectionCard(startupRows()))

        page.addView(createSectionLabel("播放净化"))
        page.addView(createSectionCard(playbackRows()))

        page.addView(createSectionLabel("竖屏视频净化"))
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

    private fun shareRows(): List<View> {
        return listOf(
            createSwitchRow(
                "净化分享",
                "将 b23.tv / bili2233.cn 短链还原为普通链接，并保留必要定位参数。",
                ModuleSettings.KEY_PURIFY_SHARE_ENABLED,
                false,
            ),
            createSwitchRow(
                "普通链接分享",
                "不再以小程序方式分享到 QQ 或微信，同时复制分享链接时尽量转换为 av 号。",
                ModuleSettings.KEY_MINI_PROGRAM_ENABLED,
                false,
            ),
        )
    }

    private fun copyRows(): List<View> {
        return listOf(
            createSwitchRow(
                "去除长按复制",
                "禁用评论、动态、视频简介等场景里长按后直接复制到剪贴板的行为，减少误触。",
                ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED,
                false,
            ) {
                disableLongPressCopySwitch = it
            },
            createSwitchRow(
                "长按自由复制",
                "需先开启“去除长按复制”，长按文本时才会弹出可自由选择的文本窗口。",
                ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED,
                false,
            ) {
                enhanceLongPressCopySwitch = it
            },
        )
    }

    private fun startupRows(): List<View> {
        return listOf(
            createSwitchRow(
                "跳过开屏广告",
                "清理启动时的开屏广告响应，减少进入 BBZQ 作用目标时的等待。",
                ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED,
                true,
            ),
        )
    }

    private fun playbackRows(): List<View> {
        return listOf(
            createSwitchRow(
                "跳过视频激励广告",
                "参考 BBZQ 的奖励广告处理逻辑，自动尝试完成视频激励页。",
                ModuleSettings.KEY_SKIP_REWARD_AD_ENABLED,
                false,
            ),
        )
    }

    private fun storyRows(): List<View> {
        val rows = mutableListOf<View>()
        rows += createSwitchRow(
            "净化竖屏视频广告",
            "按标签过滤竖屏视频流中的广告、购物和推广内容。",
            ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED,
            false,
        ) {
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
                    if (key == ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED ||
                        key == ModuleSettings.KEY_DISABLE_LONG_PRESS_COPY_ENABLED
                    ) {
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
        val copyBaseEnabled = ModuleSettings.isDisableLongPressCopyEnabled(prefs)
        val copyEnhanceEnabled = copyBaseEnabled && ModuleSettings.isEnhanceLongPressCopyEnabled(prefs)

        if (!copyBaseEnabled && prefs.getBoolean(ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false)) {
            prefs.edit().putBoolean(ModuleSettings.KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false).apply()
        }

        disableLongPressCopySwitch.isChecked = copyBaseEnabled
        enhanceLongPressCopySwitch.isEnabled = copyBaseEnabled
        enhanceLongPressCopySwitch.isChecked = copyEnhanceEnabled

        storyVideoAdSwitch.isChecked = storyEnabled
        tagCheckBoxes.forEach { (key, checkBox) ->
            checkBox.isEnabled = storyEnabled
            checkBox.isChecked = key in selectedTags
        }

        blockedCountView.text =
            "累计拦截 ${prefs.getInt(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT, 0)} 条内容"
        refreshing = false
    }

    private fun saveSelectedTags() {
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS, selectedTagKeys().toMutableSet())
            .apply()
    }

    private fun selectedTagKeys(): Set<String> =
        tagCheckBoxes.filterValues { it.isChecked }.keys.toSet()

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        private val PAGE_BACKGROUND = Color.parseColor("#F6F7F8")
        private val TITLE_COLOR = Color.parseColor("#18191C")
        private val SUMMARY_COLOR = Color.parseColor("#9499A0")
    }
}
