package ui.render;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts verbose game events into short, styled log lines. */
public final class GameLogFormatter {
    private static final double FONT_SIZE = 15;
    private static final Pattern TIMESTAMP = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\]\\s*");

    private GameLogFormatter() {
    }

    public static String stripTimestamp(String line) {
        if (line == null) {
            return "";
        }
        return TIMESTAMP.matcher(line.trim()).replaceFirst("");
    }

    public static String simplify(String raw) {
        String line = stripTimestamp(raw);
        if (line.isBlank()) {
            return "";
        }

        if (line.startsWith("Players:")) {
            return "";
        }

        line = line.replaceAll("^=== Starting New Game ===$", "▶ New game");
        line = line.replaceAll("^=== Game started with (\\d+) players ===$", "▶ Start · $1 players");
        line = line.replaceAll("^=== (.+) wins! ===$", "★ $1 wins");

        line = line.replaceAll("^(.+?) drew 2 cards$", "$1 · Draw ×2");
        line = line.replaceAll("^(.+?) played: (.+)$", "$1 · ▶ $2");
        line = line.replaceAll("^(.+?) played (.+)$", "$1 · ▶ $2");
        line = line.replaceAll("^(.+?) banked (.+)$", "$1 · Bank $2");
        line = line.replaceAll("^(.+?) discarded (.+?) \\(hand size limit\\)$", "$1 · Discard $2");
        line = line.replaceAll("^(.+?) discarded (.+)$", "$1 · Discard $2");
        line = line.replaceAll("^(.+?) ended turn voluntarily$", "$1 · End turn");
        line = line.replaceAll("^(.+?) ended turn$", "$1 · End turn");
        line = line.replaceAll("^(.+?) turn ends → (.+?)'s turn$", "$1 · Turn → $2");
        line = line.replaceAll("^(.+?) played 3 cards, turn ending$", "$1 · 3 cards played");
        line = line.replaceAll("^(.+?) Three cards have been played in this turn, turn end$", "$1 · 3 cards played");
        line = line.replaceAll("^(.+?) has 10 seconds left$", "⏱ $1 · 10s left");
        line = line.replaceAll("^(.+?) auto-discarded (.+?) \\(hand size limit\\)$", "$1 · Auto discard $2");
        line = line.replaceAll("^(.+?) ran out of time and was skipped(?: → (.+?)'s turn)?$", "$1 · Skipped");
        line = line.replaceAll("^(.+?) ran out of time and was skipped$", "$1 · Skipped");

        line = line.replaceAll("^(.+?) banked (.+?) \\(\\d+M\\)$", "$1 · Bank $2");
        line = line.replaceAll("^(.+?) used effect: (.+)$", "$1 · ▶ $2");
        line = line.replaceAll("^(.+?) used (.+?) but was blocked by Just Say No$", "$1 · Blocked · $2");
        line = line.replaceAll("^(.+?) failed to use (.+?); card discarded$", "$1 · Failed · $2");
        line = line.replaceAll("^(.+?) deposited wild property \\[(.+?)\\].*$", "$1 · Wild $2");
        line = line.replaceAll("^(.+?) played wild property as (.+)$", "$1 · ▶ $2");

        line = line.replaceAll("^(.+?) played Just Say No and cancelled \"(.+?)\"$", "$1 · No! · $2");
        line = line.replaceAll("^(.+?) played Just Say No and countered the previous Just Say No$", "$1 · No! counter");
        line = line.replaceAll("^(.+?) played Just Say No against (.+)$", "$1 · No! · $2");
        line = line.replaceAll("^(.+?) countered with Just Say No$", "$1 · No! counter");
        line = line.replaceAll("^(.+?) blocked (.+?) with Just Say No$", "$1 · No! · $2");

        line = line.replaceAll("^(.+?) paid (.+?) \\((\\d+)M\\) to (.+)$", "$1 · Pay $3M → $4");
        line = line.replaceAll("^(.+?) paid (.+?) \\((\\d+)M\\) to (.+?) for rent$", "$1 · Rent $3M → $4");
        line = line.replaceAll("^(.+?) paid (.+?) \\((\\d+)M\\)$", "$1 · Pay $3M");

        line = line.replaceAll("^(.+?) collected (\\d+)M from (.+)$", "$1 · +$2M ← $3");
        line = line.replaceAll("^(.+?) collected total (\\d+)M rent$", "$1 · Rent +$2M");
        line = line.replaceAll("^(.+?) collected (\\d+)M from My Birthday$", "$1 · Birthday +$2M");
        line = line.replaceAll("^(.+?) collected (\\d+)M from Debt Collector$", "$1 · Debt +$2M");
        line = line.replaceAll("^(.+?) used My Birthday and collected (\\d+)M.*$", "$1 · Birthday +$2M");
        line = line.replaceAll("^(.+?) used \"(.+?)\" to charge .+$", "$1 · Rent · $2");
        line = line.replaceAll("^(.+?) used Double the Rent with \"(.+?)\" to charge .+$", "$1 · 2× Rent · $2");
        line = line.replaceAll("^(.+?) stole (.+?) from (.+)$", "$1 · Steal $2 ← $3");
        line = line.replaceAll("^(.+?) stole the full (.+?) set from (.+)$", "$1 · Steal set · $2");
        line = line.replaceAll("^(.+?) swapped properties with (.+?): .+$", "$1 · Swap ↔ $2");
        line = line.replaceAll("^(.+?) played (.+?) for 0M rent$", "$1 · ▶ $2 · 0M");
        line = line.replaceAll("^(.+?) played My Birthday$", "$1 · ▶ Birthday");
        line = line.replaceAll("^(.+?) played Debt Collector on (.+)$", "$1 · ▶ Debt · $2");
        line = line.replaceAll("^(.+?) played (.+?) on (.+)$", "$1 · ▶ $2 → $3");
        line = line.replaceAll("^(.+?) failed to play (.+)$", "$1 · Failed · $2");
        line = line.replaceAll("^(.+?) played Pass Go$", "$1 · ▶ Pass Go");
        line = line.replaceAll("^(.+?) must discard down to .+$", "$1 · Must discard");
        line = line.replaceAll("^(.+?) wants a rematch$", "$1 · Rematch ✓");
        line = line.replaceAll("^(.+?) declined a rematch$", "$1 · Rematch ✗");
        line = line.replaceAll("^All players voted yes — starting a new game$", "▶ Rematch start");

        line = line.replaceAll("^Error playing card: .+$", "⚠ Play error");

        return shortenIfNeeded(line);
    }

    private static String shortenIfNeeded(String line) {
        if (line.length() <= 72) {
            return line;
        }
        return line.substring(0, 69) + "...";
    }

    public static TextFlow buildStyledLine(String line) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("game-log-entry");

        if (line.startsWith("★")) {
            flow.getChildren().add(styled(line, "-fx-fill: #b45309; -fx-font-size: "
                    + (FONT_SIZE + 1) + "px; -fx-font-weight: bold;"));
            return flow;
        }
        if (line.startsWith("▶") || line.startsWith("⏱") || line.startsWith("⚠")) {
            flow.getChildren().add(styled(line, "-fx-fill: #1f5a2f; -fx-font-size: "
                    + FONT_SIZE + "px; -fx-font-weight: bold;"));
            return flow;
        }

        int split = line.indexOf(" · ");
        if (split < 0) {
            flow.getChildren().add(styled(line, baseStyle()));
            return flow;
        }

        String player = line.substring(0, split);
        String action = line.substring(split + 3);
        flow.getChildren().add(styled(player, "-fx-fill: #1f5a2f; -fx-font-size: "
                + FONT_SIZE + "px; -fx-font-weight: bold;"));
        flow.getChildren().add(styled(" · ", "-fx-fill: #94a3b8; -fx-font-size: "
                + (FONT_SIZE - 1) + "px;"));
        appendAction(flow, action);
        return flow;
    }

    private static void appendAction(TextFlow flow, String action) {
        if (action.startsWith("▶ ")) {
            flow.getChildren().add(styled("▶ ", "-fx-fill: #64748b; -fx-font-size: "
                    + FONT_SIZE + "px; -fx-font-weight: bold;"));
            appendHighlightedTail(flow, action.substring(2));
            return;
        }
        if (action.startsWith("+") || action.contains("M")) {
            appendMoneyAction(flow, action);
            return;
        }
        appendHighlightedTail(flow, action);
    }

    private static void appendMoneyAction(TextFlow flow, String action) {
        Matcher matcher = Pattern.compile("^(\\+\\d+M)(.*)$").matcher(action);
        if (matcher.matches()) {
            flow.getChildren().add(styled(matcher.group(1), "-fx-fill: #b45309; -fx-font-size: "
                    + FONT_SIZE + "px; -fx-font-weight: bold;"));
            appendHighlightedTail(flow, matcher.group(2));
            return;
        }
        matcher = Pattern.compile("^(Pay|Rent|Debt|Birthday)(.*)$").matcher(action);
        if (matcher.matches()) {
            flow.getChildren().add(styled(matcher.group(1), "-fx-fill: #64748b; -fx-font-size: "
                    + FONT_SIZE + "px; -fx-font-weight: bold;"));
            appendHighlightedTail(flow, matcher.group(2));
            return;
        }
        appendHighlightedTail(flow, action);
    }

    private static void appendHighlightedTail(TextFlow flow, String tail) {
        if (tail.isEmpty()) {
            return;
        }
        if (tail.startsWith(" · ") || tail.startsWith(" ← ") || tail.startsWith(" → ")
                || tail.startsWith(" ↔ ") || tail.startsWith(" ·")) {
            int idx = tail.indexOf(' ', 1);
            if (idx < 0) {
                flow.getChildren().add(styled(tail, "-fx-fill: #64748b; -fx-font-size: "
                        + FONT_SIZE + "px;"));
                return;
            }
            flow.getChildren().add(styled(tail.substring(0, idx + 1), "-fx-fill: #64748b; -fx-font-size: "
                    + FONT_SIZE + "px;"));
            flow.getChildren().add(styled(tail.substring(idx + 1), "-fx-fill: #1d4ed8; -fx-font-size: "
                    + FONT_SIZE + "px; -fx-font-weight: bold;"));
            return;
        }
        flow.getChildren().add(styled(tail, "-fx-fill: #334155; -fx-font-size: "
                + FONT_SIZE + "px; -fx-font-weight: bold;"));
    }

    private static Text styled(String text, String style) {
        Text node = new Text(text);
        node.setStyle(style);
        return node;
    }

    private static String baseStyle() {
        return "-fx-fill: #334155; -fx-font-size: " + FONT_SIZE + "px;";
    }
}
