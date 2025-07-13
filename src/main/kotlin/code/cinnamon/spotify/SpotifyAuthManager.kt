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
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int
)

object SpotifyAuthManager {
    private val clientId = "56654fae013745cebd3ccdff860c5d5b"
    private val clientSecret = "62aad2d60b584ad5b52bdcdbf3ad437d"
    private val redirectUri = "http://127.0.0.1:21852/spotify/token"

    private val httpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()

    private var accessToken: String? = null
    private var refreshToken: String? = null

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
                    val json = Json { ignoreUnknownKeys = true }.decodeFromString<TokenResponse>(responseBody)
                    accessToken = json.accessToken
                    refreshToken = json.refreshToken
                    println("[Spotify] Access token received successfully")

                    // Notify in-game that connection was successful
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

    fun getAccessToken(): String? = accessToken

    fun disconnect() {
        accessToken = null
        refreshToken = null
        println("[Spotify] Disconnected from Spotify")
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
                // Start token request in background
                Thread {
                    SpotifyAuthManager.requestAccessToken(code)
                }.start()

                // Serve success page
                serveSuccessPage(exchange)

                // Stop server after successful auth
                Thread {
                    Thread.sleep(2000) // Give time for page to load
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