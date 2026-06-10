package ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Global game preferences for background music, sound effects, and fullscreen mode.
 * <p>
 * Call {@link #initialize(Stage)} once at startup to bind the primary stage.
 */
public final class GameSettings {
    /** Background music tracks used in the lobby and during gameplay. */
    public enum MusicScene {
        /** Lobby menu background music. */
        LOBBY("/audio/music/background.wav", 0.32),
        /** In-match background music. */
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

    /**
     * Binds the primary stage, installs button sounds, and starts background music.
     *
     * @param stage application primary stage; may be {@code null}
     */
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

    /** Returns whether background music is enabled. */
    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    /** Enables or disables background music and applies the change immediately. */
    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        applyMusicState();
    }

    /** Switches to lobby background music. */
    public static void useLobbyMusic() {
        setMusicScene(MusicScene.LOBBY);
    }

    /** Switches to in-game background music. */
    public static void useGameMusic() {
        setMusicScene(MusicScene.GAME);
    }

    /** Returns whether sound effects are enabled. */
    public static boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }

    /** Enables or disables sound effects. */
    public static void setSoundEffectsEnabled(boolean enabled) {
        soundEffectsEnabled = enabled;
    }

    /** Returns whether fullscreen mode is enabled. */
    public static boolean isFullscreenEnabled() {
        return fullscreenEnabled;
    }

    /** Enables or disables fullscreen on the primary stage. */
    public static void setFullscreenEnabled(boolean enabled) {
        fullscreenEnabled = enabled;
        if (primaryStage != null) {
            primaryStage.setFullScreen(enabled);
        }
    }

    /** Stops and disposes the music player and clears cached audio clips. */
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
