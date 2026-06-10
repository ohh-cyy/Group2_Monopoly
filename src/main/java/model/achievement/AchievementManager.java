package model.achievement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Singleton-style manager for local achievement progress.
 * Loads and saves unlocked achievement ids to a properties file in the user's home directory.
 */
public final class AchievementManager {
    /** Achievement id: player opened the lobby screen. */
    public static final String WELCOME_LOBBY = "welcome-lobby";
    /** Achievement id: player entered a display name. */
    public static final String SET_NAME = "set-name";
    /** Achievement id: player chose a game mode (host, join, or local). */
    public static final String CHOOSE_MODE = "choose-mode";
    /** Achievement id: player drew cards for the first time in a game. */
    public static final String FIRST_DRAW = "first-draw";
    /** Achievement id: player played, placed, or deposited a card for the first time. */
    public static final String FIRST_PLAY = "first-play";

    /** Full list of achievements available in the game. */
    private static final List<Achievement> CATALOG = List.of(
            new Achievement(WELCOME_LOBBY, "🎲", "First Lobby Visit", "Open the Monopoly Deal lobby and enter the game."),
            new Achievement(SET_NAME, "🧑", "Ready to Play", "Enter a player name to appear on the roster."),
            new Achievement(CHOOSE_MODE, "🚪", "Choose Mode", "Start as host, join a game, or play local hot seat."),
            new Achievement(FIRST_DRAW, "🃏", "First Draw", "Successfully draw 2 cards in a game."),
            new Achievement(FIRST_PLAY, "💰", "First Card Played", "Play, place, or deposit any card in a game.")
    );

    /** Path to the local persistence file for unlocked achievements. */
    private static final Path SAVE_FILE = Path.of(
            System.getProperty("user.home", "."),
            ".monopolydeal-achievements.properties"
    );
    /** In-memory set of achievement ids that have been unlocked. */
    private static final Set<String> unlocked = new LinkedHashSet<>();

    static {
        load();
    }

    private AchievementManager() {
    }

    /** @return unmodifiable view of all defined achievements */
    public static synchronized List<Achievement> getCatalog() {
        return CATALOG;
    }

    /** @return number of achievements currently unlocked */
    public static synchronized int unlockedCount() {
        return unlocked.size();
    }

    /** @return total number of achievements in the catalog */
    public static int totalCount() {
        return CATALOG.size();
    }

    /** @return progress string in the form "unlocked/total", e.g. "3/5" */
    public static synchronized String progressText() {
        return unlockedCount() + "/" + totalCount();
    }

    /**
     * @param id achievement identifier
     * @return {@code true} if the achievement has been unlocked
     */
    public static synchronized boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    /** @return unmodifiable copy of all unlocked achievement ids */
    public static synchronized Set<String> unlockedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(unlocked));
    }

    /**
     * Unlocks an achievement by id if it exists and is not already unlocked.
     * Persists the updated progress to disk.
     *
     * @param id achievement identifier
     * @return the unlocked achievement, or empty if already unlocked or unknown
     */
    public static synchronized Optional<Achievement> unlock(String id) {
        Achievement achievement = findById(id).orElse(null);
        if (achievement == null || unlocked.contains(id)) {
            return Optional.empty();
        }
        unlocked.add(id);
        save();
        return Optional.of(achievement);
    }

    /**
     * Looks up an achievement definition by id.
     *
     * @param id achievement identifier
     * @return matching achievement, or empty if not found
     */
    public static Optional<Achievement> findById(String id) {
        return CATALOG.stream()
                .filter(achievement -> achievement.id().equals(id))
                .findFirst();
    }

    private static void load() {
        if (!Files.exists(SAVE_FILE)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(SAVE_FILE)) {
            properties.load(in);
            for (Achievement achievement : CATALOG) {
                if (Boolean.parseBoolean(properties.getProperty(achievement.id(), "false"))) {
                    unlocked.add(achievement.id());
                }
            }
        } catch (IOException ignored) {
            unlocked.clear();
        }
    }

    private static void save() {
        Properties properties = new Properties();
        for (Achievement achievement : CATALOG) {
            properties.setProperty(achievement.id(), Boolean.toString(unlocked.contains(achievement.id())));
        }
        try {
            Path parent = SAVE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(SAVE_FILE)) {
                properties.store(out, "Monopoly Deal achievements");
            }
        } catch (IOException ignored) {
        }
    }
}
