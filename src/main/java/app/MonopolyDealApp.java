package app;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import ui.GameSettings;
import ui.SettingsOverlay;

/**
 * JavaFX {@link Application} entry point for Monopoly Deal.
 * Shows an animated title screen, then routes to the lobby on start.
 */
public class MonopolyDealApp extends Application {
    /** Classpath URL for the shared game stylesheet. */
    private static final String THEME_CSS = "/ui/game-theme.css";

    /** Loops floating-card motion on the cover screen; stopped when entering the lobby. */
    private Timeline coverAnimation;

    /** 初始化窗口，设置最小尺寸，设置标题，初始化设置，显示封面，显示窗口 */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setTitle("Monopoly Deal");
        GameSettings.initialize(primaryStage);
        showCover(primaryStage);
        primaryStage.show();
    }

    /** 展示封面动画，点击进入大厅 */
    private void showCover(Stage primaryStage) {
        GameSettings.useLobbyMusic();
        StackPane root = new StackPane();
        root.getStyleClass().add("cover-root");

        Pane motionLayer = new Pane();
        motionLayer.setMouseTransparent(true);
        motionLayer.getStyleClass().add("cover-motion-layer");
        motionLayer.getChildren().addAll(
                createFloatingCard(105, 105, -12, 0.20),
                createFloatingCard(760, 95, 14, 0.17),
                createFloatingCard(165, 470, 9, 0.15),
                createFloatingCard(735, 430, -10, 0.19),
                createLightOrb(210, 170, 130, 0.16),
                createLightOrb(735, 380, 180, 0.12)
        );

        Label title = new Label("Monopoly Deal");
        title.getStyleClass().add("cover-title");

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("cover-start-button");
        startButton.setOnAction(event -> showLobby(primaryStage));

        VBox content = new VBox(32, title, startButton);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("cover-content");

        root.getChildren().addAll(motionLayer, content);
        StackPane.setAlignment(content, Pos.CENTER);
        SettingsOverlay.addTo(root, primaryStage);

        Scene scene = new Scene(root, 960, 640);
        addTheme(scene);
        primaryStage.setScene(scene);
        startCoverAnimation(motionLayer);
    }

    /** 装饰卡片，用于封面背景 */
    private Rectangle createFloatingCard(double x, double y, double rotate, double opacity) {
        Rectangle card = new Rectangle(116, 162);
        card.getStyleClass().add("cover-floating-card");
        card.setLayoutX(x);
        card.setLayoutY(y);
        card.setRotate(rotate);
        card.setOpacity(opacity);
        card.setArcWidth(16);
        card.setArcHeight(16);
        return card;
    }

    /** 装饰光球，用于封面背景 */
    private Circle createLightOrb(double x, double y, double radius, double opacity) {
        Circle orb = new Circle(radius);
        orb.getStyleClass().add("cover-light-orb");
        orb.setLayoutX(x);
        orb.setLayoutY(y);
        orb.setOpacity(opacity);
        return orb;
    }

    /** 启动封面动画，使每个装饰节点无限浮动 */
    private void startCoverAnimation(Pane motionLayer) {
        if (coverAnimation != null) {
            coverAnimation.stop();
        }
        coverAnimation = new Timeline();
        int index = 0;
        for (var node : motionLayer.getChildren()) {
            double direction = index % 2 == 0 ? 1 : -1;
            coverAnimation.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(node.translateYProperty(), 0),
                            new KeyValue(node.translateXProperty(), 0),
                            new KeyValue(node.rotateProperty(), node.getRotate())),
                    new KeyFrame(Duration.seconds(5 + index * 0.55),
                            new KeyValue(node.translateYProperty(), direction * 28, Interpolator.EASE_BOTH),
                            new KeyValue(node.translateXProperty(), direction * 14, Interpolator.EASE_BOTH),
                            new KeyValue(node.rotateProperty(), node.getRotate() + direction * 5, Interpolator.EASE_BOTH))
            );
            index++;
        }
        coverAnimation.setCycleCount(Animation.INDEFINITE);
        coverAnimation.setAutoReverse(true);
        coverAnimation.play();
    }

    /** 停止封面动画，显示大厅 */
    private void showLobby(Stage primaryStage) {
        if (coverAnimation != null) {
            coverAnimation.stop();
            coverAnimation = null;
        }
        SettingsOverlay.showLobby(primaryStage);
        if (primaryStage.getScene() != null) {
            addTheme(primaryStage.getScene());
        }
    }

    /** 添加游戏样式表，如果样式表不存在 */
    private void addTheme(Scene scene) {
        String css = getClass().getResource(THEME_CSS).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    /** 启动JavaFX应用程序 */
    public static void main(String[] args) {
        launch(args);
    }

    /** 停止封面动画，释放设置资源 */
    @Override
    public void stop() {
        if (coverAnimation != null) {
            coverAnimation.stop();
            coverAnimation = null;
        }
        GameSettings.dispose();
    }
}
