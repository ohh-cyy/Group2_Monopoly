package sync;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class RoomStorage {

    private RoomStorage() {
    }

    public static RoomFolder createRoom(String hostName) throws IOException {
        String code = generateRoomCode();
        Path root = RoomFolder.defaultRoomsRoot().resolve(code);
        RoomFolder folder = new RoomFolder(root, code);
        folder.ensureExists();

        Properties props = new Properties();
        props.setProperty("started", "false");
        props.setProperty("host", hostName);
        props.setProperty("version", "0");
        folder.saveRoomProperties(props);

        registerPlayer(folder, 0, hostName);
        return folder;
    }

    public static RoomFolder openRoom(String roomCode) throws IOException {
        Path root = RoomFolder.defaultRoomsRoot().resolve(roomCode.trim().toUpperCase());
        if (!Files.isDirectory(root)) {
            throw new IOException("房间不存在: " + roomCode);
        }
        RoomFolder folder = new RoomFolder(root, roomCode.trim().toUpperCase());
        folder.ensureExists();
        return folder;
    }

    public static int registerPlayer(RoomFolder folder, int seat, String name) throws IOException {
        Properties props = folder.loadRoomProperties();
        props.setProperty("player." + seat, name);
        folder.saveRoomProperties(props);
        return seat;
    }

    public static int joinNextSeat(RoomFolder folder, String name) throws IOException {
        Properties props = folder.loadRoomProperties();
        for (int i = 0; i < RoomFolder.MAX_PLAYERS; i++) {
            if (!props.containsKey("player." + i)) {
                props.setProperty("player." + i, name);
                folder.saveRoomProperties(props);
                return i;
            }
        }
        throw new IOException("房间已满");
    }

    public static List<String> listPlayerNames(RoomFolder folder) throws IOException {
        Properties props = folder.loadRoomProperties();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < RoomFolder.MAX_PLAYERS; i++) {
            String key = "player." + i;
            if (props.containsKey(key)) {
                names.add(props.getProperty(key));
            }
        }
        return names;
    }

    public static boolean isStarted(RoomFolder folder) throws IOException {
        return "true".equalsIgnoreCase(folder.loadRoomProperties().getProperty("started", "false"));
    }

    public static void markStarted(RoomFolder folder) throws IOException {
        Properties props = folder.loadRoomProperties();
        props.setProperty("started", "true");
        folder.saveRoomProperties(props);
    }

    public static void writeSnapshots(RoomFolder folder, RoomPublicSnapshot pub,
                                      Map<Integer, PlayerPrivateSnapshot> privateBySeat) throws IOException {
        pub.version = readVersion(folder) + 1;
        writeObject(folder.publicSnapshotFile(), pub);
        for (Map.Entry<Integer, PlayerPrivateSnapshot> e : privateBySeat.entrySet()) {
            writeObject(folder.privateSnapshotFile(e.getKey()), e.getValue());
        }
        Properties props = folder.loadRoomProperties();
        props.setProperty("version", String.valueOf(pub.version));
        folder.saveRoomProperties(props);
    }

    public static long readVersion(RoomFolder folder) throws IOException {
        String v = folder.loadRoomProperties().getProperty("version", "0");
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static RoomPublicSnapshot readPublic(RoomFolder folder) throws IOException, ClassNotFoundException {
        return readObject(folder.publicSnapshotFile());
    }

    public static PlayerPrivateSnapshot readPrivate(RoomFolder folder, int seat)
            throws IOException, ClassNotFoundException {
        return readObject(folder.privateSnapshotFile(seat));
    }

    public static void submitCommand(RoomFolder folder, RoomCommand command) throws IOException {
        folder.ensureExists();
        String fileName = System.currentTimeMillis() + "_" + command.seat + ".cmd";
        writeObject(folder.commandsDir().resolve(fileName), command);
    }

    public static List<RoomCommand> drainCommands(RoomFolder folder) throws IOException, ClassNotFoundException {
        List<RoomCommand> commands = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder.commandsDir(), "*.cmd")) {
            for (Path path : stream) {
                commands.add(readObject(path));
                Files.deleteIfExists(path);
            }
        }
        commands.sort(Comparator.comparingLong(c -> 0));
        return commands;
    }

    private static String generateRoomCode() {
        return String.format("%04d", new Random().nextInt(10000));
    }

    private static void writeObject(Path path, Serializable obj) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temp)))) {
            out.writeObject(obj);
        }
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readObject(Path path) throws IOException, ClassNotFoundException {
        if (!Files.exists(path)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            return (T) in.readObject();
        }
    }
}
