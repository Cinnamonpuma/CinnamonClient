// AutoClickerAccess.java
package code.cinnamon.util;

/**
 * Public interface for triggering autoclicker actions.
 * This class is separate from the mixin to avoid package restrictions.
 */
public class AutoClickerAccess {
    
    private static boolean pendingLeftClick = false;
    private static boolean pendingRightClick = false;
    
    /**
     * Request a left click to be performed on the next mouse tick
     */
    public static void triggerLeftClick() {
        pendingLeftClick = true;
    }
    
    /**
     * Request a right click to be performed on the next mouse tick
     */
    public static void triggerRightClick() {
        pendingRightClick = true;
    }
    
    /**
     * Check if there's a pending left click (used by mixin)
     */
    public static boolean hasPendingLeftClick() {
        return pendingLeftClick;
    }
    
    /**
     * Check if there's a pending right click (used by mixin)
     */
    public static boolean hasPendingRightClick() {
        return pendingRightClick;
    }
    
    /**
     * Clear pending left click flag (used by mixin)
     */
    public static void clearPendingLeftClick() {
        pendingLeftClick = false;
    }
    
    /**
     * Clear pending right click flag (used by mixin)
     */
    public static void clearPendingRightClick() {
        pendingRightClick = false;
    }
    
    /**
     * Clear all pending clicks
     */
    public static void clearAllPendingClicks() {
        pendingLeftClick = false;
        pendingRightClick = false;
    }
}