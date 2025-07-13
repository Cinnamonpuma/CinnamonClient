package code.cinnamon.spotify

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.awt.Desktop
import java.net.*
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
    private val redirectUri = "http://127.0.0.1:8888/callback"

    private val httpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    fun authenticate() {
        val state = UUID.randomUUID().toString()
        val authUrl = HttpUrl.Builder()
            .scheme("https")
            .host("accounts.spotify.com")
            .addPathSegment("authorize")
            .addQueryParameter("response_type", "code")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("scope", "user-read-playback-state user-read-currently-playing")
            .addQueryParameter("state", state)
            .build()
            .toString()

        Desktop.getDesktop().browse(URI(authUrl))

        val server = com.sun.net.httpserver.HttpServer.create(InetSocketAddress(8888), 0)
        server.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query ?: return@createContext
            val params = query.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            val code = params["code"] ?: return@createContext
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.write("✅ Login success. You can close this.".toByteArray())
            exchange.responseBody.close()

            exchange.close()
            server.stop(1)

            requestAccessToken(code)
        }
        server.start()
    }

    private fun requestAccessToken(code: String) {
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
            println("✅ Spotify Access Token: $accessToken")
        }
    }

    fun getAccessToken(): String? = accessToken
}
