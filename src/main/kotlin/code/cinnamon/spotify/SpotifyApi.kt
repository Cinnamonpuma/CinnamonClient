package code.cinnamon.spotify

import kotlinx.serialization.json.*
import okhttp3.*

data class SpotifyTrackData(
    val title: String,
    val artist: String,
    val progress: Float,
    val albumImageUrl: String?,
    val currentTimeMs: Long,
    val durationMs: Long
)

object SpotifyApi {
    private val httpClient = OkHttpClient()

    fun getCurrentSong(token: String): Pair<String, String>? {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/currently-playing")
            .addHeader("Authorization", "Bearer $token")
            .build()

        httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                return null
            }

            val body = res.body?.string() ?: return null
            if (body.isEmpty()) {
                return null
            }

            val json = Json.parseToJsonElement(body).jsonObject

            val title = json["item"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "Unknown"
            val imageUrl = json["item"]?.jsonObject?.get("album")?.jsonObject
                ?.get("images")?.jsonArray?.get(0)?.jsonObject?.get("url")?.jsonPrimitive?.content

            return title to (imageUrl ?: "")
        }
    }

    fun getCurrentTrackData(token: String): SpotifyTrackData? {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/currently-playing")
            .addHeader("Authorization", "Bearer $token")
            .build()

        httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                if (res.code == 401) {
                }
                return null
            }

            val body = res.body?.string() ?: return null
            if (body.isEmpty()) {
                return null
            }

            try {
                val json = Json.parseToJsonElement(body).jsonObject

                val item = json["item"]?.jsonObject
                if (item == null) {
                    return null
                }

                val title = item["name"]?.jsonPrimitive?.content ?: "Unknown"

                val artists = item["artists"]?.jsonArray
                val artist = if (artists != null) {
                    artists.joinToString(", ") { artistObj ->
                        artistObj.jsonObject["name"]?.jsonPrimitive?.content ?: "Unknown"
                    }
                } else {
                    "Unknown"
                }

                val album = item["album"]?.jsonObject
                val albumImageUrl = album?.get("images")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content


                val progressMs = json["progress_ms"]?.jsonPrimitive?.long ?: 0L
                val durationMs = item["duration_ms"]?.jsonPrimitive?.long ?: 1L
                val progress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()) else 0f



                val isPlaying = json["is_playing"]?.jsonPrimitive?.boolean ?: false

                return SpotifyTrackData(
                    title = title,
                    artist = artist,
                    progress = progress.coerceIn(0f, 1f),
                    albumImageUrl = albumImageUrl,
                    currentTimeMs = progressMs,
                    durationMs = durationMs
                )
            } catch (e: Exception) {
                println("[Spotify API] Error parsing JSON: ${e.message}")
                e.printStackTrace()
                return null
            }
        }
    }

    fun getPlayerState(token: String): String? {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/player")
            .addHeader("Authorization", "Bearer $token")
            .build()

        httpClient.newCall(req).execute().use { res ->

            if (!res.isSuccessful) {
                return null
            }

            val body = res.body?.string()
            if (body.isNullOrEmpty()) {
                return null
            }

            return body
        }
    }
}