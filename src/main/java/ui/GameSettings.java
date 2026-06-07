package ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.net.URL;

public final class GameSettings {
    private static final String BACKGROUND_MUSIC = "/audio/music/background.wav";

    private static Stage primaryStage;
    private static MediaPlayer musicPlayer;
    private static boolean musicEnabled = true;
    private static boolean fullscreenEnabled = false;

    private GameSettings() {
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
        if (primaryStage != null) {
            primaryStage.setFullScreen(fullscreenEnabled);
            primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) ->
                    fullscreenEnabled = isFullScreen);
        }
        applyMusicState();
    }

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        applyMusicState();
    }

    public static boolean isFullscreenEnabled() {
        return fullscreenEnabled;
    }

    public static void setFullscreenEnabled(boolean enabled) {
        fullscreenEnabled = enabled;
        if (primaryStage != null) {
            primaryStage.setFullScreen(enabled);
        }
    }

    public static void dispose() {
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
    }

    private static void applyMusicState() {
        if (!musicEnabled) {
            if (musicPlayer != null) {
                musicPlayer.pause();
            }
            return;
        }
        ensureMusicPlayer();
        if (musicPlayer != null) {
            musicPlayer.play();
        }
    }

    private static void ensureMusicPlayer() {
        if (musicPlayer != null) {
            return;
        }
        URL resource = GameSettings.class.getResource(BACKGROUND_MUSIC);
        if (resource == null) {
            return;
        }
        try {
            Media media = new Media(resource.toExternalForm());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(0.32);
        } catch (RuntimeException ignored) {
            musicPlayer = null;
        }
    }
}
