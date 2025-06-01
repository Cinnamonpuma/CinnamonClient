// NoFallAccess.java
package code.cinnamon.util;

/**
 * Public interface for controlling NoFall functionality.
 * This class provides thread-safe access to NoFall settings and state.
 */
public class NoFallAccess {
    
    // Enum for different NoFall methods
    public enum NoFallMethod {
        PACKET("Packet Spoofing", "Spoofs onGround packets to prevent fall damage"),
        VELOCITY("Velocity Reduction", "Reduces fall velocity to minimize damage"),
        TELEPORT("Micro Teleport", "Performs small upward teleports during falls"),
        DAMAGE_CANCEL("Damage Cancel", "Directly cancels fall damage calculations"),
        SMART("Smart Mode", "Adaptively chooses the best method based on situation"),
        HYBRID("Hybrid Mode", "Combines multiple methods for maximum effectiveness");
        
        private final String displayName;
        private final String description;
        
        NoFallMethod(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Core state
    private static volatile boolean enabled = false;
    private static volatile NoFallMethod currentMethod = NoFallMethod.PACKET;
    
    // Packet spoofing settings
    private static volatile boolean groundSpoofRequested = false;
    private static volatile boolean preventFallDamage = true;
    
    // Velocity method settings
    private static volatile double velocityReduction = 2.0; // Minimum velocity to trigger reduction
    private static volatile double velocityMultiplier = 0.6; // Multiply fall velocity by this
    private static volatile double maxVelocity = 10.0; // Maximum allowed fall velocity
    
    // Teleport method settings
    private static volatile double teleportThreshold = 3.0; // Velocity threshold for teleport
    private static volatile double teleportDistance = 0.1; // Distance to teleport upward
    private static volatile boolean resetVelocityAfterTeleport = true;
    
    // Smart method settings
    private static volatile double minActivationHeight = 3.0; // Minimum fall distance to activate
    private static volatile long activationCooldown = 100; // Cooldown between activations (ms)
    
    // Hybrid method settings
    private static volatile double hybridVelocityThreshold = 5.0;
    private static volatile double hybridTeleportThreshold = 12.0;
    
    // Advanced settings
    private static volatile boolean onlyWhenFalling = true; // Only activate when actually falling
    private static volatile boolean ignoreVoidFalls = false; // Don't activate in void
    private static volatile boolean respectWater = true; // Don't activate in water
    private static volatile boolean respectLava = true; // Don't activate in lava
    private static volatile int maxActivationsPerSecond = 20; // Rate limiting
    
    // Statistics and state tracking
    private static volatile long lastActivation = 0;
    private static volatile long totalActivations = 0;
    private static volatile long packetsSpoofed = 0;
    private static volatile double totalDamagePrevented = 0.0;
    private static volatile long sessionStartTime = System.currentTimeMillis();
    
    // === Core Control Methods ===
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        NoFallAccess.enabled = enabled;
        if (enabled) {
            sessionStartTime = System.currentTimeMillis();
        }
    }
    
    public static NoFallMethod getMethod() {
        return currentMethod;
    }
    
    public static void setMethod(NoFallMethod method) {
        currentMethod = method;
    }
    
    // === Packet Spoofing Methods ===
    
    public static void requestGroundSpoof() {
        groundSpoofRequested = true;
    }
    
    public static boolean shouldSpoofGround() {
        return enabled && groundSpoofRequested;
    }
    
    public static void clearGroundSpoofRequest() {
        groundSpoofRequested = false;
    }
    
    public static boolean shouldPreventFallDamage() {
        return enabled && preventFallDamage;
    }
    
    public static void setPreventFallDamage(boolean prevent) {
        preventFallDamage = prevent;
    }
    
    // === Velocity Method Configuration ===
    
    public static double getVelocityReduction() {
        return velocityReduction;
    }
    
    public static void setVelocityReduction(double reduction) {
        velocityReduction = Math.max(0.1, Math.min(50.0, reduction));
    }
    
    public static double getVelocityMultiplier() {
        return velocityMultiplier;
    }
    
    public static void setVelocityMultiplier(double multiplier) {
        velocityMultiplier = Math.max(0.1, Math.min(1.0, multiplier));
    }
    
    public static double getMaxVelocity() {
        return maxVelocity;
    }
    
    public static void setMaxVelocity(double maxVel) {
        maxVelocity = Math.max(1.0, Math.min(100.0, maxVel));
    }
    
    // === Teleport Method Configuration ===
    
    public static double getTeleportThreshold() {
        return teleportThreshold;
    }
    
    public static void setTeleportThreshold(double threshold) {
        teleportThreshold = Math.max(0.5, Math.min(50.0, threshold));
    }
    
    public static double getTeleportDistance() {
        return teleportDistance;
    }
    
    public static void setTeleportDistance(double distance) {
        teleportDistance = Math.max(0.01, Math.min(2.0, distance));
    }
    
    public static boolean shouldResetVelocityAfterTeleport() {
        return resetVelocityAfterTeleport;
    }
    
    public static void setResetVelocityAfterTeleport(boolean reset) {
        resetVelocityAfterTeleport = reset;
    }
    
    // === Smart Method Configuration ===
    
    public static double getMinActivationHeight() {
        return minActivationHeight;
    }
    
    public static void setMinActivationHeight(double height) {
        minActivationHeight = Math.max(0.5, Math.min(50.0, height));
    }
    
    public static long getActivationCooldown() {
        return activationCooldown;
    }
    
    public static void setActivationCooldown(long cooldown) {
        activationCooldown = Math.max(10, Math.min(5000, cooldown));
    }
    
    // === Hybrid Method Configuration ===
    
    public static double getHybridVelocityThreshold() {
        return hybridVelocityThreshold;
    }
    
    public static void setHybridVelocityThreshold(double threshold) {
        hybridVelocityThreshold = Math.max(1.0, Math.min(50.0, threshold));
    }
    
    public static double getHybridTeleportThreshold() {
        return hybridTeleportThreshold;
    }
    
    public static void setHybridTeleportThreshold(double threshold) {
        hybridTeleportThreshold = Math.max(5.0, Math.min(100.0, threshold));
    }
    
    // === Advanced Settings ===
    
    public static boolean isOnlyWhenFalling() {
        return onlyWhenFalling;
    }
    
    public static void setOnlyWhenFalling(boolean onlyWhenFalling) {
        NoFallAccess.onlyWhenFalling = onlyWhenFalling;
    }
    
    public static boolean shouldIgnoreVoidFalls() {
        return ignoreVoidFalls;
    }
    
    public static void setIgnoreVoidFalls(boolean ignore) {
        ignoreVoidFalls = ignore;
    }
    
    public static boolean shouldRespectWater() {
        return respectWater;
    }
    
    public static void setRespectWater(boolean respect) {
        respectWater = respect;
    }
    
    public static boolean shouldRespectLava() {
        return respectLava;
    }
    
    public static void setRespectLava(boolean respect) {
        respectLava = respect;
    }
    
    public static int getMaxActivationsPerSecond() {
        return maxActivationsPerSecond;
    }
    
    public static void setMaxActivationsPerSecond(int maxActivations) {
        maxActivationsPerSecond = Math.max(1, Math.min(100, maxActivations));
    }
    
    // === Statistics and State Tracking ===
    
    public static long getLastActivation() {
        return lastActivation;
    }
    
    public static void setLastActivation(long time) {
        lastActivation = time;
    }
    
    public static long getTotalActivations() {
        return totalActivations;
    }
    
    public static void incrementActivations() {
        totalActivations++;
    }
    
    public static long getPacketsSpoofed() {
        return packetsSpoofed;
    }
    
    public static void incrementPacketsSpoofed() {
        packetsSpoofed++;
    }
    
    public static double getTotalDamagePrevented() {
        return totalDamagePrevented;
    }
    
    public static void incrementDamagePrevented(double damage) {
        totalDamagePrevented += damage;
    }
    
    public static long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public static double getSessionTime() {
        return (System.currentTimeMillis() - sessionStartTime) / 1000.0;
    }
    
    public static double getActivationsPerSecond() {
        double sessionTime = getSessionTime();
        return sessionTime > 0 ? totalActivations / sessionTime : 0.0;
    }
    
    // === Utility Methods ===
    
    public static void resetStatistics() {
        totalActivations = 0;
        packetsSpoofed = 0;
        totalDamagePrevented = 0.0;
        sessionStartTime = System.currentTimeMillis();
        lastActivation = 0;
    }
    
    public static void resetToDefaults() {
        enabled = false;
        currentMethod = NoFallMethod.PACKET;
        preventFallDamage = true;
        
        velocityReduction = 2.0;
        velocityMultiplier = 0.6;
        maxVelocity = 10.0;
        
        teleportThreshold = 3.0;
        teleportDistance = 0.1;
        resetVelocityAfterTeleport = true;
        
        minActivationHeight = 3.0;
        activationCooldown = 100;
        
        hybridVelocityThreshold = 5.0;
        hybridTeleportThreshold = 12.0;
        
        onlyWhenFalling = true;
        ignoreVoidFalls = false;
        respectWater = true;
        respectLava = true;
        maxActivationsPerSecond = 20;
        
        resetStatistics();
    }
    
    public static boolean isRateLimited() {
        double currentRate = getActivationsPerSecond();
        return currentRate > maxActivationsPerSecond;
    }
    
    public static String getMethodName() {
        return currentMethod.getDisplayName();
    }
    
    public static String getMethodDescription() {
        return currentMethod.getDescription();
    }
    
    public static NoFallMethod[] getAllMethods() {
        return NoFallMethod.values();
    }
}