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

public final class GameAlertDialogs {
    private GameAlertDialogs() {
    }

    public static void showError(Node owner, String message) {
        showError(owner, "无法执行", message);
    }

    public static void showError(Node owner, String title, String message) {
        Platform.runLater(() -> showErrorNow(owner, title, message));
    }

    private static void showErrorNow(Node owner, String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title == null || title.isBlank() ? "无法执行" : title);
        ButtonType ok = new ButtonType("知道了", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);
        dialog.setResultConverter(button -> button);

        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getStyleClass().add("dialog-body");

        Label header = new Label("操作无效");
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
