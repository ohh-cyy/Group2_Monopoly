package app;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
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
    /** Classpath URL for the looping cover video. */
    private static final String COVER_VIDEO = "/video/cover.mp4";

    /** Plays the looping cover video; disposed when entering the lobby. */
    private MediaPlayer coverMediaPlayer;
    /** Runs the short yellow wipe after the start button is pressed. */
    private Timeline coverTransition;

    /** Configures the stage, initializes settings, and displays the cover screen. */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setTitle("Monopoly Deal");
        GameSettings.initialize(primaryStage);
        showCover(primaryStage);
        primaryStage.show();
    }

    /** Builds and displays the video title screen. */
    private void showCover(Stage primaryStage) {
        GameSettings.useLobbyMusic();
        StackPane root = new StackPane();
        root.getStyleClass().add("cover-root");

        Media media = new Media(getClass().getResource(COVER_VIDEO).toExternalForm());
        coverMediaPlayer = new MediaPlayer(media);
        coverMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        coverMediaPlayer.setMute(true);

        MediaView coverVideo = new MediaView(coverMediaPlayer);
        coverVideo.setPreserveRatio(true);
        coverVideo.setSmooth(true);
        coverVideo.setMouseTransparent(true);
        coverVideo.fitWidthProperty().bind(root.widthProperty());
        coverVideo.fitHeightProperty().bind(root.heightProperty());

        Region videoShade = new Region();
        videoShade.setMouseTransparent(true);
        videoShade.getStyleClass().add("cover-video-shade");

        Label eyebrow = new Label("QUICK DEAL. BIG COMEBACK.");
        eyebrow.getStyleClass().add("cover-eyebrow");

        Label title = new Label("MONOPOLY\nDEAL");
        title.getStyleClass().add("cover-title");

        Label subtitle = new Label("Build your fortune before the deal turns.");
        subtitle.getStyleClass().add("cover-subtitle");

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("cover-start-button");
        startButton.setOnAction(event ->
                playCoverTransition(primaryStage, root, startButton));

        VBox content = new VBox(18, eyebrow, title, subtitle, startButton);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(530);
        content.getStyleClass().add("cover-content");

        root.getChildren().addAll(coverVideo, videoShade, content);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        StackPane.setMargin(content, new Insets(28, 32, 28, 74));
        SettingsOverlay.addTo(root, primaryStage);

        Scene scene = new Scene(root, 960, 640);
        addTheme(scene);
        primaryStage.setScene(scene);
        coverMediaPlayer.play();
    }

    /** Covers the video with a quick yellow wipe before opening the lobby. */
    private void playCoverTransition(Stage primaryStage, StackPane root, Button startButton) {
        if (coverTransition != null) {
            return;
        }
        startButton.setDisable(true);

        Region wipe = new Region();
        wipe.getStyleClass().add("cover-transition-wipe");
        wipe.setPrefSize(root.getWidth() + 24, root.getHeight() + 24);
        wipe.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wipe.setTranslateX(-root.getWidth() - 24);

        Label transitionText = new Label("LET'S DEAL");
        transitionText.getStyleClass().add("cover-transition-text");
        transitionText.setOpacity(0);

        root.getChildren().addAll(wipe, transitionText);
        StackPane.setAlignment(transitionText, Pos.CENTER);

        coverTransition = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(wipe.translateXProperty(), -root.getWidth() - 24),
                        new KeyValue(transitionText.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(480),
                        new KeyValue(wipe.translateXProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(transitionText.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(650),
                        new KeyValue(transitionText.opacityProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(900))
        );
        coverTransition.setOnFinished(event -> {
            coverTransition = null;
            showLobby(primaryStage);
        });
        coverTransition.play();
    }

    /** Stops cover media and switches the stage to the lobby view. */
    private void showLobby(Stage primaryStage) {
        disposeCover();
        SettingsOverlay.showLobby(primaryStage);
        if (primaryStage.getScene() != null) {
            addTheme(primaryStage.getScene());
        }
    }

    /** Attaches the shared game stylesheet if not already present. */
    private void addTheme(Scene scene) {
        String css = getClass().getResource(THEME_CSS).toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }

    /** Launches the JavaFX application. */
    public static void main(String[] args) {
        launch(args);
    }

    /** Stops cover media and releases shared settings resources. */
    @Override
    public void stop() {
        disposeCover();
        GameSettings.dispose();
    }

    /** Stops and releases all cover-screen animation resources. */
    private void disposeCover() {
        if (coverTransition != null) {
            coverTransition.stop();
            coverTransition = null;
        }
        if (coverMediaPlayer != null) {
            coverMediaPlayer.stop();
            coverMediaPlayer.dispose();
            coverMediaPlayer = null;
        }
    }
}
