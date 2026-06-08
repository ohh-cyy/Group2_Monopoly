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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class GameAlertDialogs {
    private GameAlertDialogs() {
    }

    public static void showError(Node owner, String message) {
        showError(owner, "Action Failed", message);
    }

    public static void showError(Node owner, String title, String message) {
        GameAudio.play(GameAudio.Cue.ERROR);
        Platform.runLater(() -> showErrorNow(owner, title, message));
    }

    /** @return true if the player wants another round */
    public static boolean askPlayAgain(Node owner) {
        return askPlayAgainBlocking(owner, "Play another round?");
    }

    public static void askPlayAgain(Node owner, Consumer<Boolean> onResult) {
        askPlayAgain(owner, "Play another round?", onResult);
    }

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
        ButtonType yes = new ButtonType("Play Again");
        ButtonType no = new ButtonType("End Game", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Play Again?");
        dialog.getDialogPane().getButtonTypes().setAll(yes, no);
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
        dialog.getDialogPane().setContent(body);
        styleDialog(dialog, owner, false);

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
        dialog.showAndWait();
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
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
    }
}
