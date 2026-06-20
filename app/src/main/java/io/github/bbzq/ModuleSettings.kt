package io.github.bbzq

import android.content.SharedPreferences

object ModuleSettings {
    const val PREFS_NAME = "bbzq_settings"
    const val KEY_MINI_PROGRAM_ENABLED = "mini_program"
    const val KEY_PURIFY_SHARE_ENABLED = "purify_share"
    const val KEY_SKIP_REWARD_AD_ENABLED = "skip_reward_ad"
    const val KEY_BLOCK_TEENAGERS_MODE_DIALOG_ENABLED = "block_teenagers_mode_dialog"
    const val KEY_SKIP_SPLASH_AD_ENABLED = "skip_splash_ad_enabled"
    const val KEY_SKIP_VIDEO_AD_ENABLED = "skip_video_ad_enabled"
    const val KEY_SKIP_VIDEO_AD_CATEGORIES = "skip_video_ad_categories"
    const val KEY_SKIP_VIDEO_AD_SETTINGS_VISIBLE = "skip_video_ad_settings_visible"
    const val KEY_ACCESS_KEY_SETTINGS_VISIBLE = "access_key_settings_visible"
    const val KEY_BLOCK_VIDEO_DETAIL_BANNER_AD_ENABLED = "block_video_detail_banner_ad_enabled"
    const val KEY_UNLOCK_VIDEO_FEATURES_ENABLED = "unlock_video_features_enabled"
    const val KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED = "auto_like_video_detail_enabled"
    const val KEY_FIX_LIVE_QUALITY_URL_ENABLED = "fix_live_quality_url_enabled"
    const val KEY_PURIFY_HOME_RECOMMEND_AD_ENABLED = "purify_home_recommend_ad_enabled"
    const val KEY_PURIFY_HOME_RECOMMEND_PICTURE_ENABLED = "purify_home_recommend_picture_enabled"
    const val KEY_PURIFY_HOME_RECOMMEND_GAME_PROMO_ENABLED = "purify_home_recommend_game_promo_enabled"
    const val KEY_BLOCK_HOME_RECOMMEND_AUTO_REFRESH_ENABLED = "block_home_recommend_auto_refresh_enabled"
    const val KEY_HIDE_ALL_HOME_COMPONENTS_ENABLED = "hide_all_home_components_enabled"
    const val KEY_CUSTOM_HOME_COMPONENT_HIDE_ENABLED = "custom_home_component_hide_enabled"
    const val KEY_HIDDEN_HOME_COMPONENTS = "hidden_home_components"
    const val KEY_KNOWN_HOME_COMPONENTS = "known_home_components"
    const val KEY_PURIFY_STORY_VIDEO_AD_ENABLED = "purify_story_video_ad_enabled"
    const val KEY_PURIFY_STORY_VIDEO_AD_TAGS = "purify_story_video_ad_tags"
    const val KEY_PURIFY_STORY_VIDEO_AD_BLOCKED_COUNT = "purify_story_video_ad_blocked_count"
    const val KEY_CUSTOM_DOWNLOAD_THREAD_ENABLED = "custom_download_thread_enabled"
    const val KEY_CUSTOM_DOWNLOAD_CONCURRENCY = "custom_download_concurrency"
    const val KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED = "skip_mini_game_reward_ad_enabled"
    const val KEY_BLOCK_LIVE_RESERVATION_ENABLED = "block_live_reservation_enabled"
    const val KEY_BLOCK_LIVE_ROOM_QOE_POPUP_ENABLED = "block_live_room_qoe_popup_enabled"
    const val KEY_DISABLE_LONG_PRESS_COPY_ENABLED = "disable_long_press_copy_enabled"
    const val KEY_ENHANCE_LONG_PRESS_COPY_ENABLED = "enhance_long_press_copy_enabled"
    const val KEY_CUSTOM_BOTTOM_BAR_ENABLED = "custom_bottom_bar_enabled"
    const val KEY_HIDDEN_BOTTOM_BAR_ITEMS = "hidden_bottom_bar_items"
    const val KEY_KNOWN_BOTTOM_BAR_ITEMS = "known_bottom_bar_items"
    const val KEY_FULL_NUMBER_FORMAT_ENABLED = "full_number_format_enabled"
    const val KEY_UNLOCK_COMMENT_GIF_ENABLED = "unlock_comment_gif_enabled"
    const val KEY_LAST_ACCESS_KEY = "last_access_key"

    const val KEY_TARGET_APP_VERSION = "target_app_version"
    const val CACHE_BILI_SETTINGS_ACTIVITY = "cache_settings_activity"
    const val KEY_RUNTIME_HOST_PACKAGE = "runtime_host_package"
    const val KEY_RUNTIME_HOST_VERSION_NAME = "runtime_host_version_name"
    const val KEY_RUNTIME_HOST_VERSION_CODE = "runtime_host_version_code"
    const val KEY_RUNTIME_HOST_SOURCE_KIND = "runtime_host_source_kind"
    const val KEY_RUNTIME_XPOSED_API_VERSION = "runtime_xposed_api_version"
    const val KEY_RUNTIME_XPOSED_FRAMEWORK_NAME = "runtime_xposed_framework_name"
    const val KEY_RUNTIME_XPOSED_FRAMEWORK_VERSION = "runtime_xposed_framework_version"
    const val KEY_RUNTIME_XPOSED_FRAMEWORK_VERSION_CODE = "runtime_xposed_framework_version_code"
    const val KEY_RUNTIME_XPOSED_FRAMEWORK_PROPERTIES = "runtime_xposed_framework_properties"
    const val KEY_RUNTIME_KIND = "runtime_kind"
    const val KEY_RUNTIME_PATCH_MODE = "runtime_patch_mode"
    const val KEY_RUNTIME_PROCESS_NAME = "runtime_process_name"
    const val KEY_RUNTIME_LAST_UPDATE_TIME = "runtime_last_update_time"

    val defaultStoryVideoAdTags = setOf("ad")
    val defaultSkipVideoAdCategories = setOf(
        "sponsor",
        "selfpromo",
        "intro",
        "outro",
        "interaction",
        "preview",
        "filler",
        "music_offtopic",
    )

    val storyVideoAdTags = listOf(
        StoryVideoAdTag("ad", "广告", null),
        StoryVideoAdTag("short", "短剧", "短剧"),
        StoryVideoAdTag("shopping", "购物", "购物"),
        StoryVideoAdTag("tv", "电视剧", "电视剧"),
        StoryVideoAdTag("doc", "纪录片", "纪录片"),
        StoryVideoAdTag("ent", "娱乐", "娱乐"),
        StoryVideoAdTag("movie", "电影", "电影"),
        StoryVideoAdTag("music", "音乐", "音乐"),
        StoryVideoAdTag("topic", "话题", "话题"),
    )

    val skipVideoAdCategories = listOf(
        SponsorBlockCategory("sponsor", "赞助商广告", "视频中的植入、口播和商业推广。"),
        SponsorBlockCategory("selfpromo", "自我推广", "UP 主引流、关注提醒、推广其他内容。"),
        SponsorBlockCategory("intro", "片头", "与正文关系不大的固定开场。"),
        SponsorBlockCategory("outro", "片尾", "结束卡、鸣谢和结尾引导。"),
        SponsorBlockCategory("interaction", "互动提醒", "点赞、投币、评论等互动号召。"),
        SponsorBlockCategory("preview", "预览 / 回顾", "下集预告、前情提要和重复回顾。"),
        SponsorBlockCategory("filler", "填充内容", "与主线关系较弱的灌水片段。"),
        SponsorBlockCategory("music_offtopic", "离题音乐", "与内容无关的纯音乐或演奏片段。"),
    )

    fun isSkipSplashAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SKIP_SPLASH_AD_ENABLED, true)

    fun isBlockTeenagersModeDialogEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_BLOCK_TEENAGERS_MODE_DIALOG_ENABLED, false)

    fun isUnlockVideoFeaturesEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_UNLOCK_VIDEO_FEATURES_ENABLED, true)

    fun isSkipVideoAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SKIP_VIDEO_AD_ENABLED, false)

    fun getSkipVideoAdCategories(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_SKIP_VIDEO_AD_CATEGORIES, defaultSkipVideoAdCategories)
            ?: defaultSkipVideoAdCategories

    fun isSkipVideoAdSettingsVisible(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SKIP_VIDEO_AD_SETTINGS_VISIBLE, false)

    fun isAccessKeySettingsVisible(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_ACCESS_KEY_SETTINGS_VISIBLE, false)

    fun isBlockVideoDetailBannerAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_BLOCK_VIDEO_DETAIL_BANNER_AD_ENABLED, false)

    fun isAutoLikeVideoDetailEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_AUTO_LIKE_VIDEO_DETAIL_ENABLED, false)

    fun isFixLiveQualityUrlEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_FIX_LIVE_QUALITY_URL_ENABLED, false)

    fun isPurifyHomeRecommendAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PURIFY_HOME_RECOMMEND_AD_ENABLED, false)

    fun isPurifyHomeRecommendPictureEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PURIFY_HOME_RECOMMEND_PICTURE_ENABLED, false)

    fun isPurifyHomeRecommendGamePromoEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PURIFY_HOME_RECOMMEND_GAME_PROMO_ENABLED, false)

    fun isBlockHomeRecommendAutoRefreshEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_BLOCK_HOME_RECOMMEND_AUTO_REFRESH_ENABLED, false)

    fun isHideAllHomeComponentsEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_HIDE_ALL_HOME_COMPONENTS_ENABLED, false)

    fun isCustomHomeComponentHideEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_CUSTOM_HOME_COMPONENT_HIDE_ENABLED, false)

    fun getHiddenHomeComponents(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_HIDDEN_HOME_COMPONENTS, emptySet()) ?: emptySet()

    fun getKnownHomeComponents(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_KNOWN_HOME_COMPONENTS, emptySet()) ?: emptySet()

    fun isPurifyStoryVideoAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PURIFY_STORY_VIDEO_AD_ENABLED, false)

    fun getPurifyStoryVideoAdTags(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_PURIFY_STORY_VIDEO_AD_TAGS, defaultStoryVideoAdTags)
            ?: defaultStoryVideoAdTags

    fun isCustomDownloadThreadEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_CUSTOM_DOWNLOAD_THREAD_ENABLED, false)

    fun getCustomDownloadConcurrency(prefs: SharedPreferences): Int =
        prefs.getInt(KEY_CUSTOM_DOWNLOAD_CONCURRENCY, 1).coerceIn(1, 12)

    fun isSkipMiniGameRewardAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SKIP_MINI_GAME_REWARD_AD_ENABLED, true)

    fun isSkipRewardAdEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SKIP_REWARD_AD_ENABLED, false)

    fun isBlockLiveReservationEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_BLOCK_LIVE_RESERVATION_ENABLED, false)

    fun isBlockLiveRoomQoePopupEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_BLOCK_LIVE_ROOM_QOE_POPUP_ENABLED, false)

    fun isDisableLongPressCopyEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_DISABLE_LONG_PRESS_COPY_ENABLED, false)

    fun isEnhanceLongPressCopyEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_ENHANCE_LONG_PRESS_COPY_ENABLED, false)

    fun isPurifyShareEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PURIFY_SHARE_ENABLED, false)

    fun isCustomBottomBarEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_CUSTOM_BOTTOM_BAR_ENABLED, false)

    fun getHiddenBottomBarItems(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_HIDDEN_BOTTOM_BAR_ITEMS, emptySet()) ?: emptySet()

    fun getKnownBottomBarItems(prefs: SharedPreferences): Set<String> =
        prefs.getStringSet(KEY_KNOWN_BOTTOM_BAR_ITEMS, emptySet()) ?: emptySet()

    fun isFullNumberFormatEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_FULL_NUMBER_FORMAT_ENABLED, false)

    fun isUnlockCommentGifEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_UNLOCK_COMMENT_GIF_ENABLED, false)
}

data class StoryVideoAdTag(
    val key: String,
    val label: String,
    val cartText: String?,
)

data class SponsorBlockCategory(
    val key: String,
    val label: String,
    val summary: String,
)
