// PacketCombinerAccess.java
package code.cinnamon.util;

import code.cinnamon.modules.all.JoinPacketCombinerModule;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;

/**
 * Public interface for interacting with the JoinPacketCombiner module.
 * This class provides thread-safe access to packet combination functionality.
 */
public class PacketCombinerAccess {
    
    private static JoinPacketCombinerModule moduleInstance = null;
    private static final Object LOCK = new Object();
    
    // Packet interception flags
    private static volatile boolean interceptingHandshake = false;
    private static volatile boolean interceptingLogin = false;
    private static volatile boolean bypassNextPacket = false;
    
    // Temporary packet storage for combination
    private static HandshakeC2SPacket pendingIntention = null;
    private static LoginHelloC2SPacket pendingLogin = null;
    private static String currentClientId = null;
    
    /**
     * Set the module instance (called by the module itself)
     */
    public static void setModule(JoinPacketCombinerModule module) {
        synchronized (LOCK) {
            moduleInstance = module;
        }
    }
    
    /**
     * Check if the combiner module is active
     */
    public static boolean isActive() {
        synchronized (LOCK) {
            return moduleInstance != null && moduleInstance.isEnabled();
        }
    }
    
    /**
     * Intercept and process a handshake packet
     */
    public static boolean interceptHandshake(HandshakeC2SPacket packet) {
        synchronized (LOCK) {
            if (!isActive() || bypassNextPacket) {
                if (bypassNextPacket) bypassNextPacket = false;
                return false; // Let packet through normally
            }
            
            pendingIntention = packet;
            interceptingHandshake = true;
            
            System.out.println("PacketCombinerAccess: Intercepted handshake packet");
            return true; // Packet intercepted
        }
    }
    
    /**
     * Intercept and process a login start packet
     */
    public static boolean interceptLogin(LoginHelloC2SPacket packet, String clientId) {
        synchronized (LOCK) {
            if (!isActive() || bypassNextPacket) {
                if (bypassNextPacket) bypassNextPacket = false;
                return false; // Let packet through normally
            }
            
            pendingLogin = packet;
            currentClientId = clientId;
            interceptingLogin = true;
            
            // Trigger combination if we have both packets
            if (pendingIntention != null && pendingLogin != null) {
                processCombinedPackets();
            }
            
            System.out.println("PacketCombinerAccess: Intercepted login packet from " + clientId);
            return true; // Packet intercepted
        }
    }
    
    /**
     * Process the combination of intercepted packets
     */
    private static void processCombinedPackets() {
        if (moduleInstance != null && pendingLogin != null) {
            try {
                moduleInstance.handleClientLogin(
                    currentClientId != null ? currentClientId : "unknown", 
                    pendingLogin, 
                    pendingIntention
                );
            } catch (Exception e) {
                System.out.println("PacketCombinerAccess: Error processing combined packets: " + e.getMessage());
            }
        }
        
        // Clear pending packets
        clearPendingPackets();
    }
    
    /**
     * Clear all pending packet data
     */
    public static void clearPendingPackets() {
        synchronized (LOCK) {
            pendingIntention = null;
            pendingLogin = null;
            currentClientId = null;
            interceptingHandshake = false;
            interceptingLogin = false;
        }
    }
    
    /**
     * Allow the next packet to bypass interception
     */
    public static void bypassNextPacket() {
        bypassNextPacket = true;
    }
    
    /**
     * Force send a packet through the combiner
     */
    public static void forceSendPacket(Object packet, String clientId) {
        synchronized (LOCK) {
            if (moduleInstance != null) {
                if (packet instanceof HandshakeC2SPacket) {
                    interceptHandshake((HandshakeC2SPacket) packet);
                } else if (packet instanceof LoginHelloC2SPacket) {
                    interceptLogin((LoginHelloC2SPacket) packet, clientId);
                }
            }
        }
    }
    
    /**
     * Get current interception status
     */
    public static boolean isIntercepting() {
        return interceptingHandshake || interceptingLogin;
    }
    
    /**
     * Check if handshake is being intercepted
     */
    public static boolean isInterceptingHandshake() {
        return interceptingHandshake;
    }
    
    /**
     * Check if login is being intercepted
     */
    public static boolean isInterceptingLogin() {
        return interceptingLogin;
    }
    
    /**
     * Get the current pending intention packet
     */
    public static HandshakeC2SPacket getPendingIntention() {
        synchronized (LOCK) {
            return pendingIntention;
        }
    }
    
    /**
     * Get the current pending login packet
     */
    public static LoginHelloC2SPacket getPendingLogin() {
        synchronized (LOCK) {
            return pendingLogin;
        }
    }
    
    /**
     * Manually trigger packet combination (for testing/debugging)
     */
    public static void triggerCombination() {
        synchronized (LOCK) {
            if (pendingIntention != null || pendingLogin != null) {
                processCombinedPackets();
            }
        }
    }
    
    /**
     * Get combination statistics
     */
    public static String getStats() {
        synchronized (LOCK) {
            if (moduleInstance != null) {
                return moduleInstance.getStatus();
            }
            return "PacketCombiner: Not active";
        }
    }
    
    /**
     * Emergency cleanup - clears all state
     */
    public static void emergencyCleanup() {
        synchronized (LOCK) {
            clearPendingPackets();
            bypassNextPacket = false;
            System.out.println("PacketCombinerAccess: Emergency cleanup performed");
        }
    }
    
    // Configuration helper methods
    
    /**
     * Quick configuration for dual client setup
     */
    public static boolean configureDualClient(int proxyPort, String serverHost, int serverPort) {
        synchronized (LOCK) {
            if (moduleInstance != null) {
                try {
                    moduleInstance.setProxyPort(proxyPort);
                    moduleInstance.setTargetServer(serverHost, serverPort);
                    moduleInstance.setCombinationMode(JoinPacketCombinerModule.CombinationMode.PRIMARY_WINS);
                    moduleInstance.setPacketMirrorEnabled(true);
                    return true;
                } catch (Exception e) {
                    System.out.println("PacketCombinerAccess: Configuration error: " + e.getMessage());
                    return false;
                }
            }
            return false;
        }
    }
    
    /**
     * Quick enable/disable
     */
    public static boolean setEnabled(boolean enabled) {
        synchronized (LOCK) {
            if (moduleInstance != null) {
                if (enabled && !moduleInstance.isEnabled()) {
                    moduleInstance.enable();
                    return true;
                } else if (!enabled && moduleInstance.isEnabled()) {
                    moduleInstance.disable();
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Get detailed status information
     */
    public static String getDetailedStatus() {
        synchronized (LOCK) {
            if (moduleInstance != null) {
                return moduleInstance.getDetailedStatus();
            }
            return "PacketCombiner module not initialized";
        }
    }
}