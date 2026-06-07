package ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Objects;

public final class GameVictoryScreen {
    private GameVictoryScreen() {
    }

    public static void show(Node owner, String winnerName) {
        Platform.runLater(() -> showNow(owner, winnerName));
    }

    private static void showNow(Node owner, String winnerName) {
        String name = winnerName == null || winnerName.isBlank() ? "Unknown" : winnerName.trim();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("游戏结束");
        ButtonType ok = new ButtonType("知道了", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);
        dialog.setResultConverter(button -> button);

        VBox body = new VBox(14);
        body.setAlignment(Pos.CENTER);
        body.getStyleClass().add("victory-screen-body");

        ImageView fireworks = createFireworksView();
        Label title = new Label("恭喜获胜！");
        title.getStyleClass().add("victory-title");
        Label winner = new Label(name + " 赢得本局");
        winner.getStyleClass().add("victory-winner-label");
        Label subtitle = new Label("成功集齐 3 套完整地产");
        subtitle.getStyleClass().add("victory-subtitle");

        body.getChildren().addAll(fireworks, title, winner, subtitle);
        dialog.getDialogPane().setContent(body);
        styleDialog(dialog, owner);

        playEntranceAnimation(fireworks, title, winner, subtitle);
        dialog.showAndWait();
    }

    private static ImageView createFireworksView() {
        ImageView view = new ImageView();
        view.getStyleClass().add("victory-fireworks-image");
        view.setFitWidth(240);
        view.setPreserveRatio(true);
        try {
            Image image = new Image(Objects.requireNonNull(
                    GameVictoryScreen.class.getResourceAsStream("/ui/victory-fireworks.png")));
            view.setImage(image);
        } catch (Exception ignored) {
        }
        return view;
    }

    private static void playEntranceAnimation(ImageView fireworks, Label... labels) {
        fireworks.setOpacity(0);
        fireworks.setScaleX(0.72);
        fireworks.setScaleY(0.72);

        ScaleTransition scale = new ScaleTransition(Duration.millis(520), fireworks);
        scale.setFromX(0.72);
        scale.setFromY(0.72);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(520), fireworks);
        fade.setFromValue(0);
        fade.setToValue(1);

        scale.play();
        fade.play();

        for (int i = 0; i < labels.length; i++) {
            Label label = labels[i];
            label.setOpacity(0);
            FadeTransition labelFade = new FadeTransition(Duration.millis(360), label);
            labelFade.setFromValue(0);
            labelFade.setToValue(1);
            labelFade.setDelay(Duration.millis(180L + i * 80L));
            labelFade.play();
        }
    }

    private static void styleDialog(Dialog<?> dialog, Node owner) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(GameVictoryScreen.class.getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().addAll("game-dialog", "victory-dialog");
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
    }
}
