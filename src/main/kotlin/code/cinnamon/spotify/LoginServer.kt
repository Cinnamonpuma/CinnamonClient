package code.cinnamon.spotify

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.io.OutputStream
import java.net.InetSocketAddress

object SpotifyLoginServer {

    private var server: HttpServer? = null

    fun start(authUrl: String) {
        if (server != null) return

        server = HttpServer.create(InetSocketAddress(21852), 0).apply {
            createContext("/", Page("craftify/spotify_login", authUrl))
            createContext("/spotify/token", TokenReceiver())
            start()
        }

        // Notify user in-game
        MinecraftClient.getInstance().execute {
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(
                Text.literal("§a[Spotify] Please open http://localhost:21852/ in your browser to login.")
            )
        }

        println("[Spotify] Server listening at http://localhost:21852/")
    }

    private class Page(val resource: String, val authUrl: String?) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val template = this::class.java.classLoader.getResourceAsStream("$resource.html")
            val html = template?.bufferedReader()?.readText()
                ?.replace("\${AUTH_URL}", authUrl ?: "#") ?: "<h1>Template missing</h1>"

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
                exchange.responseBody.use { it.write("✅ Login successful. You can close this tab.".toByteArray()) }

                server?.stop(0)
                server = null
            } else {
                val msg = "No code found."
                exchange.sendResponseHeaders(400, msg.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(msg.toByteArray()) }
            }
        }
    }
}
