package ui;

import model.enums.Color;

/**
 * CSS color helpers for property set boxes on the public board.
 * <p>
 * Provides border, background, and title colors keyed by {@link model.enums.Color},
 * with stronger background opacity when a set is complete.
 */
public final class PropertyColorStyles {
    private static final double COMPLETE_BG_ALPHA_MULTIPLIER = 3.1;
    private static final double COMPLETE_BG_ALPHA_MAX = 0.58;

    private PropertyColorStyles() {
    }

    /** Returns the border hex color for a property set of the given color. */
    public static String borderHex(Color color) {
        if (color == null) {
            return "#95a5a6";
        }
        return switch (color) {
            case BROWN -> "#8B5A2B";
            case DARK_BLUE -> "#174EA6";
            case GREEN -> "#1B7F43";
            case ORANGE -> "#F2994A";
            case RED -> "#D64545";
            case YELLOW -> "#F2C94C";
            case BLACK -> "#2D3436";
            case LIGHT_BLUE -> "#7EC8E3";
            case LIGHT_GREEN -> "#6FCF97";
            case PINK -> "#E84393";
        };
    }

    /** Returns the background rgba color for an incomplete property set. */
    public static String backgroundHex(Color color) {
        return backgroundHex(color, false);
    }

    /** Returns the background rgba color, with stronger opacity when {@code complete}. */
    public static String backgroundHex(Color color, boolean complete) {
        if (color == null) {
            return complete ? "rgba(245,248,247,0.96)" : "rgba(245,248,247,0.92)";
        }
        return switch (color) {
            case BROWN -> rgba(139, 90, 43, 0.14, complete);
            case DARK_BLUE -> rgba(23, 78, 166, 0.12, complete);
            case GREEN -> rgba(27, 127, 67, 0.12, complete);
            case ORANGE -> rgba(242, 153, 74, 0.14, complete);
            case RED -> rgba(214, 69, 69, 0.12, complete);
            case YELLOW -> rgba(242, 201, 76, 0.18, complete);
            case BLACK -> rgba(45, 52, 54, 0.10, complete);
            case LIGHT_BLUE -> rgba(126, 200, 227, 0.18, complete);
            case LIGHT_GREEN -> rgba(111, 207, 151, 0.16, complete);
            case PINK -> rgba(232, 67, 147, 0.12, complete);
        };
    }

    private static String rgba(int r, int g, int b, double alpha, boolean complete) {
        if (complete) {
            double completeAlpha = Math.min(COMPLETE_BG_ALPHA_MAX, alpha * COMPLETE_BG_ALPHA_MULTIPLIER);
            return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, completeAlpha);
        }
        return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
    }

    /** Returns the title text color for a property set header. */
    public static String titleTextHex(Color color) {
        if (color == null) {
            return "#25342d";
        }
        return switch (color) {
            case YELLOW, LIGHT_BLUE, LIGHT_GREEN, ORANGE -> "#1F2A2E";
            default -> borderHex(color);
        };
    }

    /** Returns a human-readable color name such as {@code Light Blue}. */
    public static String displayName(Color color) {
        if (color == null) {
            return "Unknown";
        }
        String[] parts = color.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /** Returns title text in the form {@code Light Blue  2/3}. */
    public static String setTitleText(Color color, int ownedCount, int setSize) {
        return displayName(color) + "  " + ownedCount + "/" + setSize;
    }

    /** Extra horizontal room for longer color names such as Light Blue. */
    public static double minBoxWidthBonus(Color color) {
        if (color == null) {
            return 0;
        }
        return switch (color) {
            case LIGHT_BLUE, DARK_BLUE, LIGHT_GREEN -> 20;
            default -> 0;
        };
    }
}
