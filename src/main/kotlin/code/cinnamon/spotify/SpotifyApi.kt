package code.cinnamon.spotify

import kotlinx.serialization.json.*
import okhttp3.*

object SpotifyApi {
    private val httpClient = OkHttpClient()

    fun getCurrentSong(token: String): Pair<String, String>? {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/currently-playing")
            .addHeader("Authorization", "Bearer $token")
            .build()

        httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null

            val body = res.body?.string() ?: return null
            val json = Json.parseToJsonElement(body).jsonObject

            val title = json["item"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "Unknown"
            val imageUrl = json["item"]?.jsonObject?.get("album")?.jsonObject
                ?.get("images")?.jsonArray?.get(0)?.jsonObject?.get("url")?.jsonPrimitive?.content

            return title to (imageUrl ?: "")
        }
    }
}
