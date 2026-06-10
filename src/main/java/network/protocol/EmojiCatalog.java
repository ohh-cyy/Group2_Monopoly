package network.protocol;

import java.util.List;

/**
 * Allowed emoji reactions in online play.
 * The server rejects SEND_EMOJI messages not in this whitelist.
 */
public final class EmojiCatalog {
    public static final List<String> ALL = List.of("😀", "😂", "😎", "😮", "👏", "💰", "🎲", "🔥");

    private EmojiCatalog() {
    }

    /** Returns true if the emoji is allowed to be sent over the network. */
    public static boolean contains(String emoji) {
        return emoji != null && ALL.contains(emoji.trim());
    }

    /** English label for UI tooltips and accessibility. */
    public static String nameFor(String emoji) {
        return switch (emoji) {
            case "😀" -> "Smile";
            case "😂" -> "Laugh";
            case "😎" -> "Cool";
            case "😮" -> "Surprised";
            case "👏" -> "Applause";
            case "💰" -> "Money";
            case "🎲" -> "Lucky roll";
            case "🔥" -> "On fire";
            default -> "Reaction";
        };
    }
}
