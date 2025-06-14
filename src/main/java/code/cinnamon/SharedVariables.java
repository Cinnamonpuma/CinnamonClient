package code.cinnamon;

import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;

public class SharedVariables {
    public static boolean enabled = true;
    public static boolean packetSendingEnabled = true; // Separate variable for packet sending

    // Custom font identifier
    public static final Identifier CINNA_FONT = Identifier.of("cinnamon", "cinna_font");

    // For Save GUI / Open Saved GUI functionality
    public static Screen storedScreen = null;
    public static ScreenHandler storedScreenHandler = null;
}