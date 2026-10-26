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

object MyInstantsClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<MyInstantsResult> = withContext(Dispatchers.IO) {
        try {
            val url = if (query.isBlank()) {
                "https://www.myinstants.com/api/v1/instants/?format=json"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://www.myinstants.com/api/v1/instants/?name=$encoded&format=json"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GjallarAudio-App")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val resultsArray = json.optJSONArray("results") ?: return@withContext emptyList()

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
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
