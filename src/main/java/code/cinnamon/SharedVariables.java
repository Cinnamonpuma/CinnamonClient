package code.cinnamon;

import net.minecraft.util.Identifier;

public class SharedVariables {
    public static boolean enabled = true;
    public static boolean packetSendingEnabled = true; // Separate variable for packet sending
    
    // Add your custom font identifier
    public static final Identifier CINNA_FONT = Identifier.of("cinnamon", "cinna_font");
}