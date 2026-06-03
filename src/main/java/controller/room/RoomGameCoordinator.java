package controller.room;

import engine.GameEngine;
import javafx.application.Platform;
import model.card.Card;
import sync.CardSnapshotMapper;
import sync.PlayerPrivateSnapshot;
import sync.RoomCommand;
import sync.RoomFolder;
import sync.RoomPublicSnapshot;
import sync.RoomSnapshotBuilder;
import sync.RoomStorage;
import sync.RoomSyncWatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Coordinates room files and sync state for host/client games.
 */
public class RoomGameCoordinator {
    @FunctionalInterface
    public interface RoomCommandHandler {
        void handle(RoomCommand command) throws Exception;
    }

    private final BiConsumer<String, Boolean> status;
    private final RoomCommandHandler commandHandler;
    private final Consumer<List<String>> remoteLogHandler;
    private final Runnable updateUi;
    private final List<String> roomLogLines = new ArrayList<>();
    private RoomFolder roomFolder;
    private int localSeat;
    private boolean hostMode;
    private RoomSyncWatcher roomWatcher;
    private List<Card> viewHand = new ArrayList<>();
    private List<Card> viewBank = new ArrayList<>();
    private RoomPublicSnapshot remotePublic;
    private long lastSeenVersion = -1;

    public RoomGameCoordinator(BiConsumer<String, Boolean> status,
                               RoomCommandHandler commandHandler,
                               Consumer<List<String>> remoteLogHandler,
                               Runnable updateUi) {
        this.status = status;
        this.commandHandler = commandHandler;
        this.remoteLogHandler = remoteLogHandler;
        this.updateUi = updateUi;
    }

    public void startHost(RoomFolder folder, int seat, GameEngine gameEngine) throws Exception {
        resetRoomState(folder, seat, true);
        roomFolder = folder;
        localSeat = seat;
        hostMode = true;
        RoomStorage.markStarted(folder);
        publishState(gameEngine);
        startWatch();
        pullRemoteStateQuiet();
    }

    public void startClient(RoomFolder folder, int seat) {
        stopAndClear();
        roomFolder = folder;
        localSeat = seat;
        hostMode = false;
        remotePublic = null;
        viewHand = new ArrayList<>();
        viewBank = new ArrayList<>();
        startWatch();
        pullRemoteStateQuiet();
    }

    public void stopAndClear() {
        stopWatch();
        roomFolder = null;
        remotePublic = null;
        viewHand = new ArrayList<>();
        viewBank = new ArrayList<>();
        lastSeenVersion = -1;
        roomLogLines.clear();
    }

    private void resetRoomState(RoomFolder folder, int seat, boolean host) {
        stopWatch();
        roomFolder = folder;
        localSeat = seat;
        hostMode = host;
        remotePublic = null;
        viewHand = new ArrayList<>();
        viewBank = new ArrayList<>();
        lastSeenVersion = -1;
    }

    public void stopWatch() {
        if (roomWatcher != null) {
            roomWatcher.close();
            roomWatcher = null;
        }
    }

    public void publishState(GameEngine gameEngine) {
        if (gameEngine == null || roomFolder == null) {
            return;
        }
        try {
            RoomPublicSnapshot pub = RoomSnapshotBuilder.buildPublic(gameEngine, roomLogLines);
            RoomStorage.writeSnapshots(roomFolder, pub, RoomSnapshotBuilder.buildAllPrivate(gameEngine));
            lastSeenVersion = pub.version;
        } catch (Exception ex) {
            status.accept("Failed to save room state: " + ex.getMessage(), true);
        }
    }

    public void submitCommand(String action, String cardId, String mode, String color) {
        if (roomFolder == null) {
            return;
        }
        try {
            RoomCommand cmd = new RoomCommand();
            cmd.seat = localSeat;
            cmd.action = action;
            cmd.cardId = cardId;
            cmd.mode = mode;
            cmd.color = color;
            RoomStorage.submitCommand(roomFolder, cmd);
            status.accept("Command submitted. Waiting for host sync...", false);
        } catch (IOException e) {
            status.accept("Submit failed: " + e.getMessage(), true);
        }
    }

    public void addLogLine(String line) {
        roomLogLines.add(line);
    }

    public int getLocalSeat() {
        return localSeat;
    }

    public RoomFolder getRoomFolder() {
        return roomFolder;
    }

    public RoomPublicSnapshot getRemotePublic() {
        return remotePublic;
    }

    public List<Card> getViewHand() {
        return viewHand;
    }

    public List<Card> getViewBank() {
        return viewBank;
    }

    private void startWatch() {
        stopWatch();
        if (roomFolder == null) {
            return;
        }
        try {
            roomWatcher = new RoomSyncWatcher();
            roomWatcher.start(roomFolder, this::onRoomFilesChanged);
        } catch (IOException e) {
            status.accept("Unable to watch room folder: " + e.getMessage(), true);
        }
    }

    private void onRoomFilesChanged() {
        Platform.runLater(() -> {
            if (roomFolder == null) {
                return;
            }
            try {
                if (hostMode) {
                    drainAndProcessCommands();
                } else {
                    pullRemoteStateQuiet();
                }
            } catch (Exception ex) {
                status.accept("Sync failed: " + ex.getMessage(), true);
            }
        });
    }

    private void drainAndProcessCommands() throws Exception {
        for (RoomCommand cmd : RoomStorage.drainCommands(roomFolder)) {
            commandHandler.handle(cmd);
        }
    }

    private void pullRemoteStateQuiet() {
        try {
            long version = RoomStorage.peekVersion(roomFolder);
            if (version <= lastSeenVersion) {
                return;
            }
            RoomPublicSnapshot pub = RoomStorage.readPublic(roomFolder);
            PlayerPrivateSnapshot priv = RoomStorage.readPrivate(roomFolder, localSeat);
            if (pub == null || priv == null) {
                return;
            }
            lastSeenVersion = version;
            remotePublic = pub;
            viewHand = CardSnapshotMapper.fromSnapshots(priv.hand);
            viewBank = CardSnapshotMapper.fromSnapshots(priv.bank);
            mergeRemoteLog(pub.logLines);
            updateUi.run();
        } catch (Exception ignored) {
        }
    }

    private void mergeRemoteLog(List<String> lines) {
        if (lines == null) {
            return;
        }
        int start = Math.min(roomLogLines.size(), lines.size());
        List<String> newLines = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            roomLogLines.add(line);
            newLines.add(line);
        }
        if (!newLines.isEmpty()) {
            remoteLogHandler.accept(newLines);
        }
    }
}
