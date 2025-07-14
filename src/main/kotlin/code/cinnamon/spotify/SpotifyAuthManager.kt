package code.cinnamon.spotify

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class StoredTokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
)

object SpotifyAuthManager {
    private val clientId = "56654fae013745cebd3ccdff860c5d5b"
    private val clientSecret = "62aad2d60b584ad5b52bdcdbf3ad437d"
    private val redirectUri = "http://127.0.0.1:21852/spotify/token"

    private val httpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt: Long = 0L

    private val tokenFile = File("spotify_tokens.json")

    init {
        loadStoredTokens()
    }

    fun getAuthorizationUrl(): String {
        return buildSpotifyAuthUrl()
    }

    fun requestAccessToken(code: String) {
        println("[Spotify] Requesting access token with code: $code")
        try {
            val requestBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build()

            val credential = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic $credential")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                println("[Spotify] Token response: $responseBody")
                if (response.isSuccessful && responseBody != null) {
                    val tokenResponse = json.decodeFromString<TokenResponse>(responseBody)
                    updateTokens(tokenResponse)
                    println("[Spotify] Access token received successfully")

                    MinecraftClient.getInstance().execute {
                        MinecraftClient.getInstance().inGameHud.chatHud.addMessage(
                            Text.literal("§a[Spotify] Successfully connected to Spotify!")
                        )
                    }
                } else {
                    println("[Spotify] Failed to get access token: ${response.code}")
                }
            }
        } catch (e: Exception) {
            println("[Spotify] Failed to request token: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getAccessToken(): String? {
        if (accessToken != null && isTokenExpired()) {
            println("[Spotify] Token expired, attempting to refresh...")
            refreshAccessToken()
        }
        return accessToken
    }

    fun disconnect() {
        accessToken = null
        refreshToken = null
        expiresAt = 0L

        if (tokenFile.exists()) {
            tokenFile.delete()
        }

        println("[Spotify] Disconnected from Spotify")
    }

    private fun updateTokens(tokenResponse: TokenResponse) {
        accessToken = tokenResponse.accessToken

        if (tokenResponse.refreshToken != null) {
            refreshToken = tokenResponse.refreshToken
        }

        expiresAt = System.currentTimeMillis() + ((tokenResponse.expiresIn - 300) * 1000L)

        saveTokens()
    }

    private fun isTokenExpired(): Boolean {
        return System.currentTimeMillis() >= expiresAt
    }

    private fun refreshAccessToken(): Boolean {
        val currentRefreshToken = refreshToken ?: return false

        try {
            val requestBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", currentRefreshToken)
                .build()

            val credential = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic $credential")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val tokenResponse = json.decodeFromString<TokenResponse>(responseBody)
                    updateTokens(tokenResponse)
                    println("[Spotify] Token refreshed successfully")
                    return true
                } else {
                    println("[Spotify] Failed to refresh token: ${response.code} - $responseBody")
                    disconnect()
                    return false
                }
            }
        } catch (e: Exception) {
            println("[Spotify] Failed to refresh token: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun saveTokens() {
        if (accessToken != null && refreshToken != null) {
            try {
                val tokenData = StoredTokenData(accessToken!!, refreshToken!!, expiresAt)
                val jsonString = json.encodeToString(StoredTokenData.serializer(), tokenData)
                tokenFile.writeText(jsonString)
                println("[Spotify] Tokens saved to file")
            } catch (e: Exception) {
                println("[Spotify] Failed to save tokens: ${e.message}")
            }
        }
    }

    private fun loadStoredTokens() {
        if (tokenFile.exists()) {
            try {
                val jsonString = tokenFile.readText()
                val tokenData = json.decodeFromString<StoredTokenData>(jsonString)

                accessToken = tokenData.accessToken
                refreshToken = tokenData.refreshToken
                expiresAt = tokenData.expiresAt

                println("[Spotify] Loaded stored tokens")

                if (isTokenExpired()) {
                    println("[Spotify] Stored token expired, attempting refresh...")
                    refreshAccessToken()
                }
            } catch (e: Exception) {
                println("[Spotify] Failed to load stored tokens: ${e.message}")
                tokenFile.delete()
            }
        }
    }

    private fun buildSpotifyAuthUrl(): String {
        val scope = "user-read-playback-state user-read-currently-playing"
        val state = UUID.randomUUID().toString()
        return "https://accounts.spotify.com/authorize" +
                "?response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&scope=${scope.replace(" ", "+")}" +
                "&state=$state"
    }
}

object SpotifyLoginPageServer {
    private var server: HttpServer? = null

    fun start(authUrl: String) {
        if (server != null) {
            println("[Spotify] Server already running")
            return
        }

        try {
            server = HttpServer.create(InetSocketAddress(21852), 0).apply {
                createContext("/", LoginPage("assets/cinnamon/spotify_login.html", authUrl))
                createContext("/spotify/token", TokenReceiver())
                start()
            }

            println("[Spotify] Server started at http://127.0.0.1:21852/")
        } catch (e: Exception) {
            println("[Spotify] Failed to start server: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        println("[Spotify] Server stopped")
    }

    private class LoginPage(val resourcePath: String, val authUrl: String?) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            val html = stream?.bufferedReader()?.readText()
                ?.replace("\${AUTH_URL}", authUrl ?: "#")
                ?: "<h1>Missing spotify_login.html</h1>"

            val data = html.toByteArray()
            exchange.sendResponseHeaders(200, data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
    }

    private class TokenReceiver : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val query = exchange.requestURI.query ?: ""
            val code = query.split("&")
                .map { it.split("=") }
                .firstOrNull { it[0] == "code" }?.getOrNull(1)

            if (code != null) {
                Thread {
                    SpotifyAuthManager.requestAccessToken(code)
                }.start()

                serveSuccessPage(exchange)

                Thread {
                    Thread.sleep(2000)
                    stop()
                }.start()
            } else {
                val error = query.split("&")
                    .map { it.split("=") }
                    .firstOrNull { it[0] == "error" }?.getOrNull(1) ?: "Unknown error"

                val message = "Authentication failed: $error"
                val body = message.toByteArray()
                exchange.sendResponseHeaders(400, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }

        private fun serveSuccessPage(exchange: HttpExchange) {
            val stream = this::class.java.classLoader.getResourceAsStream("assets/cinnamon/spotify_success.html")
            val html = stream?.bufferedReader()?.readText()
                ?: """
                <!DOCTYPE html>
                <html>
                <head><title>Success</title></head>
                <body>
                    <h1>✅ Login successful!</h1>
                    <p>You can close this tab and return to Minecraft.</p>
                    <script>setTimeout(() => window.close(), 3000);</script>
                </body>
                </html>
                """.trimIndent()

            val data = html.toByteArray()
            exchange.sendResponseHeaders(200, data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
    }
}