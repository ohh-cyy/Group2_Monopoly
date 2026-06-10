package ui;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Plays short sound cues for UI actions and inferred game-log events.
 * <p>
 * Respects the sound-effects toggle from {@link GameSettings}.
 */
public final class GameAudio {
    /** Named sound clips loaded from classpath resources. */
    public enum Cue {
        /** Generic UI button click. */
        BUTTON("/audio/sfx/button.wav", 0.55),
        /** Drawing cards from the deck. */
        DRAW("/audio/sfx/draw.wav", 0.75),
        /** Playing a card from hand. */
        PLAY("/audio/sfx/play.wav", 0.72),
        /** Banking a card or collecting money into bank. */
        BANK("/audio/sfx/bank.wav", 0.72),
        /** Charging or paying rent. */
        RENT("/audio/sfx/rent.wav", 0.78),
        /** Turn change or turn timeout. */
        TURN("/audio/sfx/turn.wav", 0.62),
        /** Invalid action or blocked play. */
        ERROR("/audio/sfx/error.wav", 0.68),
        /** Game won. */
        VICTORY("/audio/sfx/victory.wav", 0.78),
        /** Emoji reaction sent in online play. */
        EMOJI("/audio/sfx/emoji.wav", 0.68);

        private final String resource;
        private final double volume;

        Cue(String resource, double volume) {
            this.resource = resource;
            this.volume = volume;
        }
    }

    private static final Map<Cue, AudioClip> CLIPS = new EnumMap<>(Cue.class);
    private static final Set<Scene> INSTALLED_SCENES =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private GameAudio() {
    }

    /**
     * Installs a scene-wide filter that plays a click sound for buttons.
     *
     * @param scene JavaFX scene to instrument; ignored when {@code null} or already installed
     */
    public static void installButtonSounds(Scene scene) {
        if (scene == null || !INSTALLED_SCENES.add(scene)) {
            return;
        }
        scene.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase button
                    && !button.getStyleClass().contains("emoji-button")) {
                play(Cue.BUTTON);
            }
        });
    }

    /**
     * Plays a sound cue when sound effects are enabled.
     *
     * @param cue cue to play; ignored when {@code null}
     */
    public static void play(Cue cue) {
        if (cue == null || !GameSettings.isSoundEffectsEnabled()) {
            return;
        }
        try {
            AudioClip clip = CLIPS.computeIfAbsent(cue, GameAudio::load);
            if (clip != null) {
                clip.play(cue.volume);
            }
        } catch (RuntimeException ignored) {
            CLIPS.remove(cue);
        }
    }

    /**
     * Infers and plays a cue based on keywords in a game-log line.
     *
     * @param line raw or simplified game-log text
     */
    public static void playForGameLog(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String text = line.toLowerCase(java.util.Locale.ROOT);
        if (text.contains("just say no") || text.contains(" blocked ")) {
            play(Cue.ERROR);
        } else if (text.contains("drew 2 cards")) {
            play(Cue.DRAW);
        } else if (text.contains("banked ")) {
            play(Cue.BANK);
        } else if ((text.contains("collected total") && text.contains("rent"))
                || (text.contains("played") && text.contains("0m rent"))) {
            play(Cue.RENT);
        } else if (text.contains("sly deal")
                || text.contains("forced deal")
                || text.contains("deal breaker")
                || text.contains(" stole ")
                || text.contains("swapped properties")) {
            play(Cue.DRAW);
        } else if (text.contains("recolored wild")) {
            play(Cue.PLAY);
        } else if (text.contains("ended turn")
                || text.contains("turn ending")
                || text.contains("ran out of time and was skipped")) {
            play(Cue.TURN);
        } else if (text.contains(" played ") && !text.contains("rent")) {
            play(Cue.PLAY);
        }
    }

    /** Clears cached audio clips and installed scene listeners. */
    public static void clear() {
        CLIPS.clear();
        INSTALLED_SCENES.clear();
    }

    private static AudioClip load(Cue cue) {
        URL resource = GameAudio.class.getResource(cue.resource);
        return resource != null ? new AudioClip(resource.toExternalForm()) : null;
    }
}
