package ui;

import javafx.scene.control.Dialog;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * Removes the native OS window chrome from JavaFX dialogs while keeping the themed card body.
 */
public final class DialogFrameStyle {
    private DialogFrameStyle() {
    }

    /** Hides title bar and system border; makes the scene background transparent outside the card. */
    public static void hideSystemFrame(Dialog<?> dialog) {
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setOnShown(event -> {
            if (dialog.getDialogPane().getScene() != null) {
                dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
            }
        });
    }
}
