package io.github.bzzq

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(ModuleSettings.PREFS_NAME, MODE_PRIVATE) }
    private val tagCheckBoxes = mutableMapOf<String, CheckBox>()
    private lateinit var storyVideoAdSwitch: Switch
    private lateinit var blockedCountView: TextView
    private var refreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
        })

        root.addView(createFeatureSwitch(
            R.string.skip_splash_ad_title,
            ModuleSettings.KEY_SKIP_SPLASH_AD_ENABLED,
            true,
        ))
        root.addView(createFeatureSwitch(
            R.string.unlock_video_features_title,
            ModuleSettings.KEY_UNLOCK_VIDEO_FEATURES_ENABLED,
            true,
        ))
        root.addView(createFeatureSwitch(
            R.string.auto_like_video_detail_title,
            ModuleSettings.KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED,
            false,
        ))
        root.addView(createFeatureSwitch(
            R.string.fix_live_quality_url_title,
            ModuleSettings.KEY_FIX_LIVE_QUALITY_URL_ENABLED,
            false,
        ))
        root.addView(createFeatureSwitch(
            R.string.skip_mini_game_reward_ad_title,
            ModuleSettings.KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED,
            true,
        ))
        root.addView(createFeatureSwitch(
            R.string.block_live_reservation_title,
            ModuleSettings.KEY_BLOCK_LIVE_RESERVATION_ENABLED,
            false,
        ))

        storyVideoAdSwitch = Switch(this).apply {
            text = getString(R.string.purify_story_video_ad_title)
            textSize = 18f
            setPadding(0, dp(16), 0, dp(8))
            setOnCheckedChangeListener { _, isChecked ->
                if (refreshing) return@setOnCheckedChangeListener

                prefs.edit().putBoolean(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_ENABLED, isChecked).apply()
                if (isChecked && selectedTagKeys().isEmpty()) {
                    prefs.edit()
                        .putStringSet(
                            ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS,
                            ModuleSettings.defaultStoryVideoAdTags,
                        )
                        .apply()
                }
                refresh()
            }
        }
        root.addView(storyVideoAdSwitch)

        ModuleSettings.storyVideoAdTags.forEach { tag ->
            val checkBox = CheckBox(this).apply {
                text = tag.label
                setOnCheckedChangeListener { _, _ ->
                    if (!refreshing) saveSelectedTags()
                }
            }
            tagCheckBoxes[tag.key] = checkBox
            root.addView(checkBox)
        }

        blockedCountView = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
        }
        root.addView(blockedCountView)

        setContentView(ScrollView(this).apply {
            addView(
                root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        })
        refresh()
    }

    private fun createFeatureSwitch(titleRes: Int, key: String, defaultValue: Boolean): Switch {
        return Switch(this).apply {
            text = getString(titleRes)
            textSize = 18f
            setPadding(0, dp(16), 0, dp(8))
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, isChecked ->
                if (!refreshing) prefs.edit().putBoolean(key, isChecked).apply()
            }
        }
    }

    private fun refresh() {
        refreshing = true
        val enabled = ModuleSettings.isPurifyStoryVideoAdEnabled(prefs)
        val selectedTags = ModuleSettings.getPurifyStoryVideoAdTags(prefs)

        storyVideoAdSwitch.isChecked = enabled
        tagCheckBoxes.forEach { (key, checkBox) ->
            checkBox.isEnabled = enabled
            checkBox.isChecked = key in selectedTags
        }
        blockedCountView.text = getString(
            R.string.purify_story_video_ad_blocked_count,
            prefs.getInt(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT, 0),
        )
        refreshing = false
    }

    private fun saveSelectedTags() {
        prefs.edit()
            .putStringSet(ModuleSettings.KEY_PURIFY_STORY_VIDEO_AD_TAGS, selectedTagKeys())
            .apply()
    }

    private fun selectedTagKeys(): Set<String> {
        return tagCheckBoxes
            .filterValues { it.isChecked }
            .keys
            .toSet()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
