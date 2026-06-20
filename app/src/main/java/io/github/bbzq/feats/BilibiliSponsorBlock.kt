package io.github.bbzq.feats

import io.github.bbzq.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BilibiliSponsorBlock(
    private val bvid: String,
    private val cid: String,
    private val enabledCategories: Set<String>,
) {
    fun getSegments(): FetchResult {
        val trimmedBvid = bvid.trim()
        if (trimmedBvid.isEmpty()) {
            return FetchResult(FetchStatus.FAILED, emptyList())
        }

        val cached = cache[trimmedBvid]
            ?.takeIf { !it.isExpired() }
            ?.result
        if (cached != null) {
            return cached.filterByCategories(enabledCategories).filterByCid(cid)
        }

        val hashPrefix = trimmedBvid.sha256().take(HASH_PREFIX_LENGTH)
        val request = Request.Builder()
            .url("$BASE_URL$hashPrefix")
            .header("accept", "application/json")
            .header("origin", REQUEST_ORIGIN)
            .header("user-agent", USER_AGENT)
            .header("x-ext-version", BuildConfig.RELEASE_NAME)
            .build()

        val result = fetchSegments(request, trimmedBvid)
        if (result.status.shouldCache) {
            cache[trimmedBvid] = CacheEntry(result)
        }
        return result.filterByCategories(enabledCategories).filterByCid(cid)
    }

    private fun fetchSegments(request: Request, targetBvid: String): FetchResult {
        return try {
            httpClient.newCall(request).execute().use { response ->
                val statusCode = response.code
                when {
                    statusCode == 404 -> FetchResult(FetchStatus.NOT_FOUND, emptyList(), statusCode)
                    !response.isSuccessful -> FetchResult(FetchStatus.FAILED, emptyList(), statusCode)
                    else -> {
                        val body = response.body.string()
                        if (body.isBlank()) {
                            FetchResult(FetchStatus.EMPTY, emptyList(), statusCode)
                        } else {
                            parseSegments(body, targetBvid, statusCode)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            FetchResult(FetchStatus.FAILED, emptyList())
        }
    }

    private fun parseSegments(
        json: String,
        targetBvid: String,
        statusCode: Int,
    ): FetchResult {
        return try {
            val payload = JSONArray(json)
            if (payload.length() == 0) {
                return FetchResult(FetchStatus.EMPTY, emptyList(), statusCode)
            }

            for (index in 0 until payload.length()) {
                val videoEntry = payload.optJSONObject(index) ?: continue
                if (videoEntry.optString("videoID") != targetBvid) continue

                val segments = videoEntry.optJSONArray("segments")
                    ?.toSegmentList()
                    .orEmpty()
                    .filter(::isSkippableSegment)
                    .sortedBy { it.segment[0] }

                return if (segments.isEmpty()) {
                    FetchResult(FetchStatus.EMPTY, emptyList(), statusCode)
                } else {
                    FetchResult(FetchStatus.SUCCESS, segments, statusCode)
                }
            }

            FetchResult(FetchStatus.NOT_FOUND, emptyList(), statusCode)
        } catch (_: JSONException) {
            FetchResult(FetchStatus.FAILED, emptyList(), statusCode)
        }
    }

    private fun JSONArray.toSegmentList(): List<Segment> {
        val items = ArrayList<Segment>(length())
        for (index in 0 until length()) {
            val segment = optJSONObject(index)?.toSegment() ?: continue
            items += segment
        }
        return items
    }

    private fun JSONObject.toSegment(): Segment? {
        val segmentArray = optJSONArray("segment") ?: return null
        if (segmentArray.length() < 2) return null

        val start = segmentArray.optDouble(0, Double.NaN)
        val end = segmentArray.optDouble(1, Double.NaN)
        if (!start.isFinite() || !end.isFinite() || end <= start) return null

        return Segment(
            segment = floatArrayOf(start.toFloat(), end.toFloat()),
            cid = optString("cid"),
            uuid = optString("UUID"),
            category = optString("category"),
            actionType = optString("actionType"),
            videoDuration = optInt("videoDuration"),
            locked = optInt("locked"),
            votes = optInt("votes"),
        )
    }

    private fun isSkippableSegment(segment: Segment): Boolean =
        segment.actionType.equals(ACTION_SKIP, ignoreCase = true)

    private fun FetchResult.filterByCategories(categories: Set<String>): FetchResult {
        if (segments.isEmpty()) return this
        if (categories.isEmpty()) return copy(status = FetchStatus.EMPTY, segments = emptyList())

        val filtered = segments.filter { it.category in categories }
        return copy(
            status = if (filtered.isEmpty() && status == FetchStatus.SUCCESS) FetchStatus.EMPTY else status,
            segments = filtered,
        )
    }

    private fun FetchResult.filterByCid(targetCid: String): FetchResult {
        if (segments.isEmpty()) return this
        if (targetCid.isBlank()) return this

        val filtered = segments.filter { it.cid.isBlank() || it.cid == targetCid }
        return copy(
            status = if (filtered.isEmpty() && status == FetchStatus.SUCCESS) FetchStatus.EMPTY else status,
            segments = filtered,
        )
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }

    data class Segment(
        val segment: FloatArray,
        val cid: String,
        val uuid: String,
        val category: String,
        val actionType: String,
        val videoDuration: Int,
        val locked: Int,
        val votes: Int,
    )

    data class FetchResult(
        val status: FetchStatus,
        val segments: List<Segment>,
        val httpStatusCode: Int? = null,
    )

    enum class FetchStatus(val shouldCache: Boolean) {
        SUCCESS(true),
        EMPTY(true),
        NOT_FOUND(true),
        FAILED(false),
    }

    private data class CacheEntry(
        val result: FetchResult,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }

    private companion object {
        private const val ACTION_SKIP = "skip"
        private const val BASE_URL = "https://bsbsb.top/api/skipSegments/"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
        private const val HASH_PREFIX_LENGTH = 4
        private const val REQUEST_ORIGIN = "BBZQ"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android; Xposed; NkBe) BBZQ/1.0"

        private val cache = ConcurrentHashMap<String, CacheEntry>()

        private val httpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }
}

