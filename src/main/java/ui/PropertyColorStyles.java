package ui;

import model.enums.Color;

/** CSS colors for property set boxes on the public board. */
public final class PropertyColorStyles {
    private PropertyColorStyles() {
    }

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

    public static String backgroundHex(Color color) {
        if (color == null) {
            return "rgba(245,248,247,0.92)";
        }
        return switch (color) {
            case BROWN -> "rgba(139,90,43,0.14)";
            case DARK_BLUE -> "rgba(23,78,166,0.12)";
            case GREEN -> "rgba(27,127,67,0.12)";
            case ORANGE -> "rgba(242,153,74,0.14)";
            case RED -> "rgba(214,69,69,0.12)";
            case YELLOW -> "rgba(242,201,76,0.18)";
            case BLACK -> "rgba(45,52,54,0.10)";
            case LIGHT_BLUE -> "rgba(126,200,227,0.18)";
            case LIGHT_GREEN -> "rgba(111,207,151,0.16)";
            case PINK -> "rgba(232,67,147,0.12)";
        };
    }

    public static String titleTextHex(Color color) {
        if (color == null) {
            return "#25342d";
        }
        return switch (color) {
            case YELLOW, LIGHT_BLUE, LIGHT_GREEN, ORANGE -> "#1F2A2E";
            default -> borderHex(color);
        };
    }

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
}
