package code.cinnamon.util;


public class AutoClickerAccess {
    
    private static boolean pendingLeftClick = false;
    private static boolean pendingRightClick = false;
    

    public static void triggerLeftClick() {
        pendingLeftClick = true;
    }
    

    public static void triggerRightClick() {
        pendingRightClick = true;
    }
    
    public static boolean hasPendingLeftClick() {
        return pendingLeftClick;
    }
    
    public static boolean hasPendingRightClick() {
        return pendingRightClick;
    }
    
    public static void clearPendingLeftClick() {
        pendingLeftClick = false;
    }
    
    public static void clearPendingRightClick() {
        pendingRightClick = false;
    }
    
    public static void clearAllPendingClicks() {
        pendingLeftClick = false;
        pendingRightClick = false;
    }
}