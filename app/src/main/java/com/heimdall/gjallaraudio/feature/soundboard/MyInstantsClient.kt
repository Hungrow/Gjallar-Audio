package com.heimdall.gjallaraudio.feature.soundboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MyInstantsResult(
    val name: String,
    val soundUrl: String,
    val slug: String,
    val color: String
)

data class MyInstantsPage(
    val results: List<MyInstantsResult>,
    val hasNext: Boolean,
    val nextPage: Int?,
    val totalCount: Int
)

object MyInstantsClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchPage(query: String = "", page: Int = 1): MyInstantsPage = withContext(Dispatchers.IO) {
        try {
            val url = if (query.isBlank()) {
                "https://www.myinstants.com/api/v1/instants/?page=$page&format=json"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://www.myinstants.com/api/v1/instants/?name=$encoded&page=$page&format=json"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GjallarAudio-App")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext MyInstantsPage(emptyList(), false, null, 0)

            val body = response.body?.string() ?: return@withContext MyInstantsPage(emptyList(), false, null, 0)
            val json = JSONObject(body)
            val resultsArray = json.optJSONArray("results") ?: return@withContext MyInstantsPage(emptyList(), false, null, 0)
            val nextUrl = json.optString("next", "")
            val count = json.optInt("count", 0)

            val list = mutableListOf<MyInstantsResult>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val sound = item.optString("sound", "")
                val name = item.optString("name", "")
                val slug = item.optString("slug", "")
                val color = item.optString("color", "38bdf8")

                if (sound.isNotBlank() && name.isNotBlank()) {
                    list.add(
                        MyInstantsResult(
                            name = name,
                            soundUrl = sound,
                            slug = slug,
                            color = color
                        )
                    )
                }
            }

            val hasNext = nextUrl.isNotBlank()
            MyInstantsPage(
                results = list,
                hasNext = hasNext,
                nextPage = if (hasNext) page + 1 else null,
                totalCount = count
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MyInstantsPage(emptyList(), false, null, 0)
        }
    }

    suspend fun fetchInitialBatch(query: String = "", pagesCount: Int = 2): MyInstantsPage = withContext(Dispatchers.IO) {
        val aggregated = mutableListOf<MyInstantsResult>()
        var lastNextPage: Int? = null
        var lastHasNext = false
        var count = 0

        for (p in 1..pagesCount) {
            val page = fetchPage(query, p)
            aggregated.addAll(page.results)
            lastNextPage = page.nextPage
            lastHasNext = page.hasNext
            count = page.totalCount
            if (!page.hasNext) break
        }

        MyInstantsPage(
            results = aggregated.distinctBy { it.slug + it.soundUrl },
            hasNext = lastHasNext,
            nextPage = lastNextPage,
            totalCount = count
        )
    }

    suspend fun search(query: String): List<MyInstantsResult> {
        return fetchInitialBatch(query, 2).results
    }
}
