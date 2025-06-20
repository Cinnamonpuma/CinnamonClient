package code.cinnamon.util

enum class MinecraftColorCodes(val code: String, val friendlyName: String, val isColor: Boolean = true) {
    BLACK("&0", "Black"),
    DARK_BLUE("&1", "Dark Blue"),
    DARK_GREEN("&2", "Dark Green"),
    DARK_AQUA("&3", "Dark Aqua"),
    DARK_RED("&4", "Dark Red"),
    DARK_PURPLE("&5", "Dark Purple"),
    GOLD("&6", "Gold"),
    GRAY("&7", "Gray"),
    DARK_GRAY("&8", "Dark Gray"),
    BLUE("&9", "Blue"),
    GREEN("&a", "Green"),
    AQUA("&b", "Aqua"),
    RED("&c", "Red"),
    LIGHT_PURPLE("&d", "Light Purple"),
    YELLOW("&e", "Yellow"),
    WHITE("&f", "White"),

    OBFUSCATED("&k", "Obfuscated", false),
    BOLD("&l", "Bold", false),
    STRIKETHROUGH("&m", "Strikethrough", false),
    UNDERLINE("&n", "Underline", false),
    ITALIC("&o", "Italic", false),
    RESET("&r", "Reset", false);

    companion object {
        fun getByCode(code: String): MinecraftColorCodes? {
            return entries.find { it.code == code }
        }

        fun getFriendlyNameByCode(code: String): String? {
            return getByCode(code)?.friendlyName
        }

        val colors: List<MinecraftColorCodes>
            get() = entries.filter { it.isColor }
    }
}
