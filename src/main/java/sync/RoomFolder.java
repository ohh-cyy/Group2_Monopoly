package sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** 一个房间在磁盘上的目录结构 */
public class RoomFolder {
    public static final int MAX_PLAYERS = 4;
    public static final int MIN_PLAYERS_TO_START = 2;

    private final Path root;
    private final String roomCode;

    public RoomFolder(Path root, String roomCode) {
        this.root = root;
        this.roomCode = roomCode;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public Path getRoot() {
        return root;
    }

    public Path publicSnapshotFile() {
        return root.resolve("public.ser");
    }

    public Path privateSnapshotFile(int seat) {
        return root.resolve("private_" + seat + ".ser");
    }

    public Path commandsDir() {
        return root.resolve("commands");
    }

    public Path roomPropertiesFile() {
        return root.resolve("room.properties");
    }

    public void ensureExists() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(commandsDir());
    }

    public Properties loadRoomProperties() throws IOException {
        Properties props = new Properties();
        Path file = roomPropertiesFile();
        if (Files.exists(file)) {
            try (var in = Files.newInputStream(file)) {
                props.load(in);
            }
        }
        return props;
    }

    public void saveRoomProperties(Properties props) throws IOException {
        try (var out = Files.newOutputStream(roomPropertiesFile())) {
            props.store(out, "Monopoly Deal Room " + roomCode);
        }
    }

    public static Path defaultRoomsRoot() {
        return Path.of(System.getProperty("user.home"), "MonopolyDealRooms");
    }
}
