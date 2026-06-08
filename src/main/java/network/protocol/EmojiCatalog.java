package network.protocol;

import java.util.List;

public final class EmojiCatalog {
    public static final List<String> ALL = List.of("😀", "😂", "😎", "😮", "👏", "💰", "🎲", "🔥");

    private EmojiCatalog() {
    }

    public static boolean contains(String emoji) {
        return emoji != null && ALL.contains(emoji.trim());
    }

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
