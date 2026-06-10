package ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Styled modal dialogs for game errors and end-of-game prompts.
 * <p>
 * All public show methods marshal work onto the JavaFX application thread when needed.
 */
public final class GameAlertDialogs {
    /** Message shown when the player has no remaining plays this turn. */
    public static final String NO_PLAYS_REMAINING_MESSAGE =
            "No plays remaining this turn. You cannot play any more cards.";

    private GameAlertDialogs() {
    }

    /** Shows the standard "no plays remaining" error dialog. */
    public static void showNoPlaysRemaining(Node owner) {
        showError(owner, "Cannot Play Card", NO_PLAYS_REMAINING_MESSAGE);
    }

    /** Shows an error dialog with title {@code Action Failed}. */
    public static void showError(Node owner, String message) {
        showError(owner, "Action Failed", message);
    }

    /** Shows a styled error dialog with the given title and message. */
    public static void showError(Node owner, String title, String message) {
        Runnable show = () -> {
            GameAudio.play(GameAudio.Cue.ERROR);
            showErrorNow(owner, title, message);
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    /**
     * Blocks and asks whether to play another round.
     *
     * @return {@code true} if the player chose to play again
     */
    public static boolean askPlayAgain(Node owner) {
        return askPlayAgainBlocking(owner, "Play another round?");
    }

    /** Asynchronously asks whether to play again and delivers the result to {@code onResult}. */
    public static void askPlayAgain(Node owner, Consumer<Boolean> onResult) {
        askPlayAgain(owner, "Play another round?", onResult);
    }

    /** Asynchronously asks with a custom message and delivers the result to {@code onResult}. */
    public static void askPlayAgain(Node owner, String message, Consumer<Boolean> onResult) {
        Platform.runLater(() -> {
            boolean accept = showPlayAgainDialog(owner, message);
            if (onResult != null) {
                onResult.accept(accept);
            }
        });
    }

    private static boolean askPlayAgainBlocking(Node owner, String message) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("askPlayAgainBlocking must run on the JavaFX thread");
        }
        return showPlayAgainDialog(owner, message);
    }

    private static boolean showPlayAgainDialog(Node owner, String message) {
        ButtonType yes = new ButtonType("Play Again", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("End Game", ButtonBar.ButtonData.NO);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Play Again?");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(yes, no);
        dialog.setResultConverter(button -> button);

        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getStyleClass().add("dialog-body");
        Label header = new Label("Game Over");
        header.getStyleClass().add("dialog-header-label");
        Label contentLabel = new Label(message == null || message.isBlank() ? "Play another round?" : message);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("dialog-content-label");
        body.getChildren().addAll(header, contentLabel);
        pane.setContent(body);
        styleDialog(dialog, owner, false);
        pane.getStyleClass().add("game-dialog-play-again");
        dialog.setOnShown(event -> {
            ButtonBar buttonBar = (ButtonBar) pane.lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    private static void showErrorNow(Node owner, String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title == null || title.isBlank() ? "Action Failed" : title);
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);
        dialog.setResultConverter(button -> button);

        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getStyleClass().add("dialog-body");

        Label header = new Label("Invalid Operation");
        header.getStyleClass().add("dialog-error-header");
        Label content = new Label(message == null ? "" : message);
        content.setWrapText(true);
        content.getStyleClass().add("dialog-content-label");
        body.getChildren().addAll(header, content);

        dialog.getDialogPane().setContent(body);
        styleDialog(dialog, owner, true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    private static Window resolveWindow(Node owner) {
        if (owner != null) {
            if (owner.getScene() != null && owner.getScene().getWindow() != null) {
                return owner.getScene().getWindow();
            }
            Node node = owner;
            while (node != null) {
                if (node.getScene() != null && node.getScene().getWindow() != null) {
                    return node.getScene().getWindow();
                }
                node = node instanceof javafx.scene.Parent parent ? parent.getParent() : null;
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing()) {
                return window;
            }
        }
        return null;
    }

    private static void styleDialog(Dialog<?> dialog, Node owner, boolean error) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(GameAlertDialogs.class.getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().add("game-dialog");
        if (error) {
            pane.getStyleClass().add("game-dialog-error");
        }
        Window window = resolveWindow(owner);
        if (window != null) {
            dialog.initOwner(window);
        }
    }
}
