package ui;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import ui.render.GameLogFormatter;

/** Scrollable game log with concise, highlighted entries. */
public class GameLogPane extends ScrollPane {
    private static final int MAX_ENTRIES = 40;

    private final VBox entries = new VBox(6);

    public GameLogPane() {
        getStyleClass().add("game-log");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setContent(entries);
    }

    public void append(String rawLine) {
        Runnable task = () -> {
            String simplified = GameLogFormatter.simplify(rawLine);
            if (simplified.isBlank()) {
                return;
            }
            entries.getChildren().add(GameLogFormatter.buildStyledLine(simplified));
            trimOldEntries();
            scrollToBottom();
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public void clear() {
        Runnable task = () -> entries.getChildren().clear();
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    private void trimOldEntries() {
        while (entries.getChildren().size() > MAX_ENTRIES) {
            entries.getChildren().remove(0);
        }
    }

    private void scrollToBottom() {
        layout();
        setVvalue(1.0);
    }
}
