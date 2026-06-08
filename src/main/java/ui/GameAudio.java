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

public final class GameAudio {
    public enum Cue {
        BUTTON("/audio/sfx/button.wav", 0.55),
        DRAW("/audio/sfx/draw.wav", 0.75),
        PLAY("/audio/sfx/play.wav", 0.72),
        BANK("/audio/sfx/bank.wav", 0.72),
        RENT("/audio/sfx/rent.wav", 0.78),
        TURN("/audio/sfx/turn.wav", 0.62),
        ERROR("/audio/sfx/error.wav", 0.68),
        VICTORY("/audio/sfx/victory.wav", 0.78),
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
        } else if (text.contains("ended turn")
                || text.contains("turn ending")
                || text.contains("ran out of time and was skipped")) {
            play(Cue.TURN);
        } else if (text.contains(" played ") && !text.contains("rent")) {
            play(Cue.PLAY);
        }
    }

    public static void clear() {
        CLIPS.clear();
        INSTALLED_SCENES.clear();
    }

    private static AudioClip load(Cue cue) {
        URL resource = GameAudio.class.getResource(cue.resource);
        return resource != null ? new AudioClip(resource.toExternalForm()) : null;
    }
}
