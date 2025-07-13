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

    fun authenticate() {
        val authUrl = buildSpotifyAuthUrl()
        SpotifyLoginPageServer.start(authUrl)
    }

    fun requestAccessToken(code: String) {
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
            val json = Json.decodeFromString<TokenResponse>(response.body!!.string())
            accessToken = json.accessToken
            refreshToken = json.refreshToken
            println("[Spotify] Access token received: $accessToken")
        }
    }

    fun getAccessToken(): String? = accessToken
    fun stopServer() = SpotifyLoginPageServer.stop()

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
        if (server != null) return

        server = HttpServer.create(InetSocketAddress(21852), 0).apply {
            createContext("/", Page("assets/cinnamon/spotify_login.html", authUrl))
            createContext("/spotify/token", TokenReceiver())
            start()
        }

        MinecraftClient.getInstance().execute {
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(
                Text.literal("\u00a7a[Spotify] Please open http://127.0.0.1:21852/ in your browser to login.")
            )
        }

        println("[Spotify] Server listening at http://127.0.0.1:21852/")
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private class Page(val resourcePath: String, val authUrl: String?) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val stream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            val html = stream?.bufferedReader()?.readText()
                ?.replace("\${AUTH_URL}", authUrl ?: "#") ?: "<h1>Missing spotify_login.html</h1>"

            exchange.sendResponseHeaders(200, html.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(html.toByteArray()) }
        }
    }

    private class TokenReceiver : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val query = exchange.requestURI.query ?: ""
            val code = query.split("&")
                .map { it.split("=") }
                .firstOrNull { it[0] == "code" }?.getOrNull(1)

            if (code != null) {
                SpotifyAuthManager.requestAccessToken(code)
                exchange.sendResponseHeaders(200, 0)
                exchange.responseBody.use { it.write("\u2705 Login successful. You can close this tab.".toByteArray()) }
                stop()
            } else {
                val msg = "No code found."
                exchange.sendResponseHeaders(400, msg.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(msg.toByteArray()) }
            }
        }
    }
}
