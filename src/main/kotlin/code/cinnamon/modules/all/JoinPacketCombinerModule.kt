// JoinPacketCombinerModule.kt
package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.util.PacketCombinerAccess
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.handshake.ClientIntentionPacket
import net.minecraft.network.packet.c2s.login.LoginStartPacket
import net.minecraft.network.packet.s2c.login.LoginSuccessPacket
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

class JoinPacketCombinerModule : Module("JoinPacketCombiner", "Combines dual client logins into single server connection") {
    
    // Network components
    private var proxyServer: Channel? = null
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()
    
    // Connection management
    private val clientConnections = ConcurrentHashMap<String, ClientConnection>()
    private val serverConnection = AtomicReference<ServerConnection?>(null)
    private val isProxyRunning = AtomicBoolean(false)
    
    // Configuration
    var proxyPort: Int = 25566
        private set
    var targetServerHost: String = "localhost"
        private set
    var targetServerPort: Int = 25565
        private set
    var primaryClientId: String = ""
        private set
    var enablePacketMirror: Boolean = true
        private set
    var combinationMode: CombinationMode = CombinationMode.PRIMARY_WINS
        private set
    
    // Statistics
    var totalCombinedLogins: Long = 0
        private set
    var activeConnections: Int = 0
        private set
    private var sessionStartTime: Long = 0
    
    enum class CombinationMode {
        PRIMARY_WINS,      // Primary client's data takes precedence
        FUSE_DATA,        // Attempt to merge compatible data
        RANDOM_SELECTION  // Randomly pick between clients
    }
    
    data class ClientConnection(
        val id: String,
        val channel: Channel,
        var loginPacket: LoginStartPacket? = null,
        var intentionPacket: ClientIntentionPacket? = null,
        var isAuthenticated: Boolean = false,
        val connectTime: Long = System.currentTimeMillis()
    )
    
    data class ServerConnection(
        val channel: Channel,
        var isConnected: Boolean = false,
        var selectedClientId: String? = null
    )
    
    companion object {
        @JvmStatic
        var instance: JoinPacketCombinerModule? = null
            private set
            
        private const val MAX_CLIENTS = 2
        private const val CONNECTION_TIMEOUT_MS = 30000L
        private const val PACKET_BUFFER_SIZE = 1024 * 8
    }
    
    init {
        instance = this
    }
    
    override fun onEnable() {
        println("JoinPacketCombiner: Module enabled on port $proxyPort -> $targetServerHost:$targetServerPort")
        sessionStartTime = System.currentTimeMillis()
        totalCombinedLogins = 0
        
        try {
            startProxyServer()
            PacketCombinerAccess.setModule(this)
            println("JoinPacketCombiner: Proxy server started successfully")
        } catch (e: Exception) {
            println("JoinPacketCombiner: Error starting proxy server: ${e.message}")
            disable()
        }
    }
    
    override fun onDisable() {
        println("JoinPacketCombiner: Module disabled")
        
        try {
            stopProxyServer()
            cleanupConnections()
            logSessionStats()
        } catch (e: Exception) {
            println("JoinPacketCombiner: Error stopping module: ${e.message}")
        }
    }
    
    private fun startProxyServer() {
        if (isProxyRunning.get()) return
        
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        "packet-decoder", PacketDecoder(),
                        "packet-encoder", PacketEncoder(),
                        "client-handler", ClientHandler()
                    )
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
        
        val future = bootstrap.bind(proxyPort).sync()
        proxyServer = future.channel()
        isProxyRunning.set(true)
        
        println("JoinPacketCombiner: Proxy server bound to port $proxyPort")
    }
    
    private fun stopProxyServer() {
        isProxyRunning.set(false)
        
        proxyServer?.close()?.sync()
        proxyServer = null
        
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
        
        println("JoinPacketCombiner: Proxy server stopped")
    }
    
    private fun cleanupConnections() {
        clientConnections.values.forEach { connection ->
            try {
                connection.channel.close()
            } catch (e: Exception) {
                println("JoinPacketCombiner: Error closing client connection: ${e.message}")
            }
        }
        clientConnections.clear()
        
        serverConnection.get()?.let { server ->
            try {
                server.channel.close()
            } catch (e: Exception) {
                println("JoinPacketCombiner: Error closing server connection: ${e.message}")
            }
        }
        serverConnection.set(null)
        
        activeConnections = 0
    }
    
    private fun logSessionStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0
        println("JoinPacketCombiner: Session stats - Combined logins: $totalCombinedLogins, Session time: %.1fs"
            .format(sessionTime))
    }
    
    // Core packet combination logic
    fun handleClientLogin(clientId: String, loginPacket: LoginStartPacket, intentionPacket: ClientIntentionPacket?) {
        val client = clientConnections[clientId] ?: return
        
        client.loginPacket = loginPacket
        client.intentionPacket = intentionPacket
        
        println("JoinPacketCombiner: Received login from client $clientId: ${loginPacket.name}")
        
        // Check if we have enough clients to proceed
        val readyClients = clientConnections.values.filter { 
            it.loginPacket != null && it.intentionPacket != null 
        }
        
        if (readyClients.size >= 2 || (readyClients.size == 1 && shouldProceedWithSingleClient())) {
            processCombinedLogin(readyClients)
        } else {
            println("JoinPacketCombiner: Waiting for more clients (${readyClients.size}/$MAX_CLIENTS ready)")
        }
    }
    
    private fun shouldProceedWithSingleClient(): Boolean {
        // Proceed with single client after timeout or if explicitly configured
        val oldestConnection = clientConnections.values.minByOrNull { it.connectTime }
        val waitTime = System.currentTimeMillis() - (oldestConnection?.connectTime ?: 0)
        return waitTime > CONNECTION_TIMEOUT_MS
    }
    
    private fun processCombinedLogin(clients: List<ClientConnection>) {
        try {
            val combinedData = combineLoginData(clients)
            val selectedClient = selectPrimaryClient(clients)
            
            println("JoinPacketCombiner: Processing combined login for ${clients.size} clients")
            println("JoinPacketCombiner: Selected primary client: ${selectedClient.id}")
            println("JoinPacketCombiner: Combined username: ${combinedData.name}")
            
            // Establish server connection
            connectToServer(combinedData, selectedClient)
            
            totalCombinedLogins++
            
        } catch (e: Exception) {
            println("JoinPacketCombiner: Error processing combined login: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun combineLoginData(clients: List<ClientConnection>): LoginStartPacket {
        return when (combinationMode) {
            CombinationMode.PRIMARY_WINS -> {
                val primary = clients.find { it.id == primaryClientId } 
                    ?: clients.first()
                primary.loginPacket!!
            }
            
            CombinationMode.FUSE_DATA -> {
                fuseLoginData(clients)
            }
            
            CombinationMode.RANDOM_SELECTION -> {
                clients.random().loginPacket!!
            }
        }
    }
    
    private fun fuseLoginData(clients: List<ClientConnection>): LoginStartPacket {
        // Attempt to create a meaningful fusion of login data
        val names = clients.mapNotNull { it.loginPacket?.name }.distinct()
        val uuids = clients.mapNotNull { it.loginPacket?.profileId }.distinct()
        
        val fusedName = when {
            names.size == 1 -> names.first()
            names.size == 2 -> "${names[0]}_${names[1]}"
            else -> "CombinedUser_${Random.nextInt(1000, 9999)}"
        }
        
        val fusedUuid = uuids.firstOrNull() ?: java.util.UUID.randomUUID()
        
        // Create new login packet with fused data
        val basePacket = clients.first().loginPacket!!
        return LoginStartPacket(fusedName, fusedUuid)
    }
    
    private fun selectPrimaryClient(clients: List<ClientConnection>): ClientConnection {
        return when {
            primaryClientId.isNotEmpty() -> 
                clients.find { it.id == primaryClientId } ?: clients.first()
            else -> 
                clients.minByOrNull { it.connectTime } ?: clients.first()
        }
    }
    
    private fun connectToServer(loginPacket: LoginStartPacket, primaryClient: ClientConnection) {
        // Implementation would create actual server connection
        // This is a simplified version showing the structure
        
        println("JoinPacketCombiner: Connecting to server $targetServerHost:$targetServerPort")
        println("JoinPacketCombiner: Using login data: ${loginPacket.name}")
        
        // Set up server connection tracking
        // In real implementation, this would establish the actual network connection
        // and handle the full login sequence
        
        primaryClient.isAuthenticated = true
        
        // Mirror packets to other clients if enabled
        if (enablePacketMirror) {
            mirrorAuthenticationToClients(loginPacket, primaryClient)
        }
    }
    
    private fun mirrorAuthenticationToClients(loginPacket: LoginStartPacket, primaryClient: ClientConnection) {
        clientConnections.values.forEach { client ->
            if (client.id != primaryClient.id) {
                try {
                    // Send authentication success to secondary clients
                    client.isAuthenticated = true
                    println("JoinPacketCombiner: Mirrored authentication to client ${client.id}")
                } catch (e: Exception) {
                    println("JoinPacketCombiner: Error mirroring to client ${client.id}: ${e.message}")
                }
            }
        }
    }
    
    // Network handlers
    inner class ClientHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val clientId = "client_${ctx.channel().id()}"
            val connection = ClientConnection(clientId, ctx.channel())
            
            clientConnections[clientId] = connection
            activeConnections = clientConnections.size
            
            println("JoinPacketCombiner: Client connected: $clientId (${activeConnections}/$MAX_CLIENTS)")
            super.channelActive(ctx)
        }
        
        override fun channelInactive(ctx: ChannelHandlerContext) {
            val clientId = "client_${ctx.channel().id()}"
            clientConnections.remove(clientId)
            activeConnections = clientConnections.size
            
            println("JoinPacketCombiner: Client disconnected: $clientId (${activeConnections}/$MAX_CLIENTS)")
            super.channelInactive(ctx)
        }
        
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            val clientId = "client_${ctx.channel().id()}"
            
            when (msg) {
                is ClientIntentionPacket -> {
                    println("JoinPacketCombiner: Received intention packet from $clientId")
                    clientConnections[clientId]?.intentionPacket = msg
                }
                
                is LoginStartPacket -> {
                    println("JoinPacketCombiner: Received login packet from $clientId")
                    handleClientLogin(clientId, msg, clientConnections[clientId]?.intentionPacket)
                }
                
                else -> {
                    // Forward other packets to server if connection established
                    forwardPacketToServer(msg, clientId)
                }
            }
        }
        
        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            println("JoinPacketCombiner: Client handler exception: ${cause.message}")
            ctx.close()
        }
    }
    
    private fun forwardPacketToServer(packet: Any, clientId: String) {
        serverConnection.get()?.let { server ->
            if (server.isConnected && server.selectedClientId == clientId) {
                try {
                    server.channel.writeAndFlush(packet)
                } catch (e: Exception) {
                    println("JoinPacketCombiner: Error forwarding packet: ${e.message}")
                }
            }
        }
    }
    
    // Configuration methods
    fun setProxyPort(port: Int) {
        if (port in 1024..65535) {
            this.proxyPort = port
            println("JoinPacketCombiner: Proxy port set to $port")
        }
    }
    
    fun setTargetServer(host: String, port: Int) {
        this.targetServerHost = host
        this.targetServerPort = port
        println("JoinPacketCombiner: Target server set to $host:$port")
    }
    
    fun setPrimaryClient(clientId: String) {
        this.primaryClientId = clientId
        println("JoinPacketCombiner: Primary client set to $clientId")
    }
    
    fun setCombinationMode(mode: CombinationMode) {
        this.combinationMode = mode
        println("JoinPacketCombiner: Combination mode set to $mode")
    }
    
    fun setPacketMirrorEnabled(enabled: Boolean) {
        this.enablePacketMirror = enabled
        println("JoinPacketCombiner: Packet mirroring ${if (enabled) "enabled" else "disabled"}")
    }
    
    // Status methods
    fun getStatus(): String {
        return buildString {
            append("JoinPacketCombiner: ${if (isEnabled) "ON" else "OFF"}")
            if (isEnabled) {
                append(" | Port: $proxyPort")
                append(" | Target: $targetServerHost:$targetServerPort")
                append(" | Clients: $activeConnections/$MAX_CLIENTS")
                append(" | Combined: $totalCombinedLogins")
                append(" | Mode: $combinationMode")
            }
        }
    }
    
    fun getDetailedStatus(): String {
        return buildString {
            appendLine("=== JoinPacketCombiner Status ===")
            appendLine("Enabled: $isEnabled")
            appendLine("Proxy Port: $proxyPort")
            appendLine("Target Server: $targetServerHost:$targetServerPort")
            appendLine("Active Connections: $activeConnections/$MAX_CLIENTS") 
            appendLine("Total Combined Logins: $totalCombinedLogins")
            appendLine("Combination Mode: $combinationMode")
            appendLine("Packet Mirroring: $enablePacketMirror")
            appendLine("Primary Client ID: ${primaryClientId.ifEmpty { "Auto-select" }}")
            appendLine("Server Connection: ${if (serverConnection.get()?.isConnected == true) "Connected" else "Disconnected"}")
            
            if (clientConnections.isNotEmpty()) {
                appendLine("Connected Clients:")
                clientConnections.values.forEach { client ->
                    appendLine("  - ${client.id}: ${if (client.isAuthenticated) "Authenticated" else "Pending"}")
                }
            }
        }
    }
    
    fun resetStats() {
        totalCombinedLogins = 0
        sessionStartTime = System.currentTimeMillis()
        println("JoinPacketCombiner: Statistics reset")
    }
    
    // Placeholder classes for packet handling
    inner class PacketDecoder : ChannelInboundHandlerAdapter()
    inner class PacketEncoder : ChannelOutboundHandlerAdapter()
}