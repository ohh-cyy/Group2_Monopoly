package ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.net.URL;

public final class GameSettings {
    public enum MusicScene {
        LOBBY("/audio/music/background.wav", 0.32),
        GAME("/audio/music/gameplay.wav", 0.14);

        private final String resource;
        private final double volume;

        MusicScene(String resource, double volume) {
            this.resource = resource;
            this.volume = volume;
        }
    }

    private static Stage primaryStage;
    private static MediaPlayer musicPlayer;
    private static MusicScene musicScene = MusicScene.LOBBY;
    private static boolean musicEnabled = true;
    private static boolean soundEffectsEnabled = true;
    private static boolean fullscreenEnabled = false;

    private GameSettings() {
    }

    public static void initialize(Stage stage) {
        primaryStage = stage;
        if (primaryStage != null) {
            primaryStage.setFullScreen(fullscreenEnabled);
            primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) ->
                    fullscreenEnabled = isFullScreen);
            primaryStage.sceneProperty().addListener((obs, oldScene, newScene) ->
                    GameAudio.installButtonSounds(newScene));
            GameAudio.installButtonSounds(primaryStage.getScene());
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

    public static void useLobbyMusic() {
        setMusicScene(MusicScene.LOBBY);
    }

    public static void useGameMusic() {
        setMusicScene(MusicScene.GAME);
    }

    public static boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }

    public static void setSoundEffectsEnabled(boolean enabled) {
        soundEffectsEnabled = enabled;
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
        GameAudio.clear();
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
        URL resource = GameSettings.class.getResource(musicScene.resource);
        if (resource == null) {
            return;
        }
        try {
            Media media = new Media(resource.toExternalForm());
            musicPlayer = new MediaPlayer(media);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(musicScene.volume);
        } catch (RuntimeException ignored) {
            musicPlayer = null;
        }
    }

    private static void setMusicScene(MusicScene nextScene) {
        if (nextScene == null || nextScene == musicScene) {
            return;
        }
        musicScene = nextScene;
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer.dispose();
            musicPlayer = null;
        }
        applyMusicState();
    }
}
