package network.protocol;

import java.util.List;

/**
 * 联机对局中允许的表情反应。
 * 服务端会拒绝不在此白名单中的 SEND_EMOJI 消息。
 */
public final class EmojiCatalog {
    public static final List<String> ALL = List.of("😀", "😂", "😎", "😮", "👏", "💰", "🎲", "🔥");

    private EmojiCatalog() {
    }

    /** 判断该表情是否允许通过网络发送。 */
    public static boolean contains(String emoji) {
        return emoji != null && ALL.contains(emoji.trim());
    }

    /** UI 工具提示与无障碍用的英文标签。 */
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
