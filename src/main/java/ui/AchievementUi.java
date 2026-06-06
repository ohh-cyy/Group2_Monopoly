package ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.achievement.Achievement;
import model.achievement.AchievementManager;

import java.util.Objects;
import java.util.Optional;

public final class AchievementUi {
    private AchievementUi() {
    }

    public static boolean unlockAndShow(String achievementId, Node owner) {
        Optional<Achievement> unlocked = AchievementManager.unlock(achievementId);
        unlocked.ifPresent(achievement -> showUnlockedDialog(achievement, owner));
        return unlocked.isPresent();
    }

    public static void showUnlockedDialog(Achievement achievement, Node owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Achievement Unlocked");
        ButtonType ok = new ButtonType("Nice!", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);

        HBox body = new HBox(14);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getStyleClass().add("achievement-dialog-body");

        Label icon = new Label(achievement.icon());
        icon.getStyleClass().add("achievement-icon");

        VBox textBox = new VBox(6);
        Label titleLabel = new Label("成就解锁 · " + achievement.title());
        titleLabel.getStyleClass().add("achievement-title");
        Label descLabel = new Label(achievement.description());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("achievement-description");
        textBox.getChildren().addAll(titleLabel, descLabel);
        body.getChildren().addAll(icon, textBox);

        dialog.getDialogPane().setContent(body);
        styleDialog(dialog, owner);
        dialog.showAndWait();
    }

    public static void showLibraryDialog(Node owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Achievement Library");
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(close);

        VBox body = new VBox(14);
        body.getStyleClass().add("achievement-library-body");

        Label header = new Label("成就库");
        header.getStyleClass().add("achievement-library-title");
        Label subtitle = new Label("已解锁 " + AchievementManager.progressText() + " · 这些成就都可以通过大厅和基础出牌操作轻松完成。");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("achievement-library-subtitle");
        ProgressBar progressBar = new ProgressBar(AchievementManager.unlockedCount() / (double) AchievementManager.totalCount());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("achievement-progress-bar");

        VBox list = new VBox(10);
        for (Achievement achievement : AchievementManager.getCatalog()) {
            list.getChildren().add(createAchievementRow(achievement));
        }
        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(360);
        scrollPane.getStyleClass().add("transparent-scroll");

        body.getChildren().addAll(header, subtitle, progressBar, scrollPane);
        dialog.getDialogPane().setContent(body);
        styleDialog(dialog, owner);
        dialog.showAndWait();
    }

    private static HBox createAchievementRow(Achievement achievement) {
        boolean unlocked = AchievementManager.isUnlocked(achievement.id());
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(unlocked ? "achievement-row-unlocked" : "achievement-row-locked");

        Label icon = new Label(unlocked ? achievement.icon() : "🔒");
        icon.getStyleClass().add("achievement-row-icon");

        VBox textBox = new VBox(3);
        Label title = new Label(achievement.title());
        title.getStyleClass().add("achievement-row-title");
        Label desc = new Label(achievement.description());
        desc.setWrapText(true);
        desc.getStyleClass().add("achievement-row-description");
        textBox.getChildren().addAll(title, desc);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(unlocked ? "已解锁" : "未解锁");
        badge.getStyleClass().add(unlocked ? "achievement-badge-unlocked" : "achievement-badge-locked");

        row.getChildren().addAll(icon, textBox, spacer, badge);
        return row;
    }

    private static void styleDialog(Dialog<?> dialog, Node owner) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(AchievementUi.class.getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().add("game-dialog");
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
    }
}
