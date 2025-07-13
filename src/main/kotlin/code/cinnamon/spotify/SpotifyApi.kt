package code.cinnamon.spotify

import kotlinx.serialization.json.*
import okhttp3.*

data class SpotifyTrackData(
    val title: String,
    val artist: String,
    val progress: Float, // 0.0 to 1.0
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
            println("[Spotify API] getCurrentSong response: ${res.code}")
            if (!res.isSuccessful) {
                println("[Spotify API] Error: ${res.message}")
                return null
            }

            val body = res.body?.string() ?: return null
            if (body.isEmpty()) {
                println("[Spotify API] Empty response - no track playing")
                return null
            }

            println("[Spotify API] Response body: $body")
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
            println("[Spotify API] getCurrentTrackData response: ${res.code}")

            if (!res.isSuccessful) {
                println("[Spotify API] Error response: ${res.message}")
                if (res.code == 401) {
                    println("[Spotify API] Token expired or invalid")
                }
                return null
            }

            val body = res.body?.string() ?: return null
            if (body.isEmpty()) {
                println("[Spotify API] Empty response - no track playing")
                return null
            }

            try {
                println("[Spotify API] Raw response: $body")
                val json = Json.parseToJsonElement(body).jsonObject

                val item = json["item"]?.jsonObject
                if (item == null) {
                    println("[Spotify API] No item in response")
                    return null
                }

                val title = item["name"]?.jsonPrimitive?.content ?: "Unknown"
                println("[Spotify API] Track title: $title")

                val artists = item["artists"]?.jsonArray
                val artist = if (artists != null) {
                    artists.joinToString(", ") { artistObj ->
                        artistObj.jsonObject["name"]?.jsonPrimitive?.content ?: "Unknown"
                    }
                } else {
                    "Unknown"
                }
                println("[Spotify API] Artists: $artist")

                val album = item["album"]?.jsonObject
                val albumImageUrl = album?.get("images")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                println("[Spotify API] Album image URL: $albumImageUrl")

                // Get timing information
                val progressMs = json["progress_ms"]?.jsonPrimitive?.long ?: 0L
                val durationMs = item["duration_ms"]?.jsonPrimitive?.long ?: 1L
                val progress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()) else 0f

                println("[Spotify API] Progress: ${progressMs}ms / ${durationMs}ms (${progress * 100}%)")

                // Check if track is playing
                val isPlaying = json["is_playing"]?.jsonPrimitive?.boolean ?: false
                println("[Spotify API] Is playing: $isPlaying")

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

    // Additional method to get player state for debugging
    fun getPlayerState(token: String): String? {
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/player")
            .addHeader("Authorization", "Bearer $token")
            .build()

        httpClient.newCall(req).execute().use { res ->
            println("[Spotify API] getPlayerState response: ${res.code}")

            if (!res.isSuccessful) {
                println("[Spotify API] Player state error: ${res.message}")
                return null
            }

            val body = res.body?.string()
            if (body.isNullOrEmpty()) {
                println("[Spotify API] No active device found")
                return null
            }

            println("[Spotify API] Player state: $body")
            return body
        }
    }
}