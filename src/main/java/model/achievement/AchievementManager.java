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

public final class AchievementManager {
    public static final String WELCOME_LOBBY = "welcome-lobby";
    public static final String SET_NAME = "set-name";
    public static final String CHOOSE_MODE = "choose-mode";
    public static final String FIRST_DRAW = "first-draw";
    public static final String FIRST_PLAY = "first-play";

    private static final List<Achievement> CATALOG = List.of(
            new Achievement(WELCOME_LOBBY, "🎲", "初入大厅", "打开 Monopoly Deal 大厅，正式进入游戏 Lobby。"),
            new Achievement(SET_NAME, "🧑", "准备就绪", "输入一个玩家名，让自己出现在牌桌名单中。"),
            new Achievement(CHOOSE_MODE, "🚪", "选择模式", "点击房主开局、加入游戏或本地热座任意入口。"),
            new Achievement(FIRST_DRAW, "🃏", "首次摸牌", "在游戏中成功摸 2 张牌。"),
            new Achievement(FIRST_PLAY, "💰", "打出第一张牌", "在游戏中打出、放置或存入任意一张牌。")
    );

    private static final Path SAVE_FILE = Path.of(
            System.getProperty("user.home", "."),
            ".monopolydeal-achievements.properties"
    );
    private static final Set<String> unlocked = new LinkedHashSet<>();

    static {
        load();
    }

    private AchievementManager() {
    }

    public static synchronized List<Achievement> getCatalog() {
        return CATALOG;
    }

    public static synchronized int unlockedCount() {
        return unlocked.size();
    }

    public static int totalCount() {
        return CATALOG.size();
    }

    public static synchronized String progressText() {
        return unlockedCount() + "/" + totalCount();
    }

    public static synchronized boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public static synchronized Set<String> unlockedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(unlocked));
    }

    public static synchronized Optional<Achievement> unlock(String id) {
        Achievement achievement = findById(id).orElse(null);
        if (achievement == null || unlocked.contains(id)) {
            return Optional.empty();
        }
        unlocked.add(id);
        save();
        return Optional.of(achievement);
    }

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
