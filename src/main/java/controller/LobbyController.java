package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sync.RoomFolder;
import sync.RoomStorage;
import sync.RoomSyncWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class LobbyController {
    @FXML
    private TextField roomsRootField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField roomCodeField;
    @FXML
    private Label statusLabel;
    @FXML
    private Label roomCodeLabel;
    @FXML
    private ListView<String> playerList;
    @FXML
    private Button hostBtn;
    @FXML
    private Button joinBtn;
    @FXML
    private Button startBtn;

    private Stage stage;
    private RoomFolder roomFolder;
    private int localSeat = -1;
    private boolean isHost;
    private RoomSyncWatcher lobbyWatcher;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        roomsRootField.setText("");
    }

    private Path roomsRootPath() {
        String text = roomsRootField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return Path.of(text.trim());
    }

    @FXML
    private void onHostClick() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter a name.");
            return;
        }
        try {
            Path root = roomsRootPath();
            RoomStorage.setConfiguredRoomsRoot(root);
            roomFolder = RoomStorage.createRoom(name, root);
            localSeat = 0;
            isHost = true;
            roomCodeField.setText(roomFolder.getRoomCode());
            roomCodeLabel.setText("Room code: " + roomFolder.getRoomCode());
            statusLabel.setText("Room path:\n" + roomFolder.getRoot()
                    + "\n\nShare the folder and room code with other players.");
            startBtn.setDisable(false);
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            refreshPlayers();
            startLobbyWatch();
        } catch (IOException e) {
            statusLabel.setText("Create failed: " + e.getMessage());
        }
    }

    @FXML
    private void onJoinClick() {
        String name = nameField.getText().trim();
        String code = roomCodeField.getText().trim();
        if (name.isEmpty() || code.isEmpty()) {
            statusLabel.setText("Please enter a name and room code.");
            return;
        }
        try {
            Path root = roomsRootPath();
            RoomStorage.setConfiguredRoomsRoot(root);
            roomFolder = RoomStorage.openRoom(code, root);
            if (RoomStorage.isStarted(roomFolder)) {
                statusLabel.setText("The game has already started.");
                return;
            }
            localSeat = RoomStorage.joinNextSeat(roomFolder, name);
            isHost = false;
            roomCodeLabel.setText("Joined room: " + roomFolder.getRoomCode());
            statusLabel.setText("Joined. Waiting for the host to start...");
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            startBtn.setDisable(true);
            refreshPlayers();
            startLobbyWatch();
        } catch (IOException e) {
            statusLabel.setText("Join failed: " + e.getMessage());
        }
    }

    @FXML
    private void onStartClick() {
        if (!isHost || roomFolder == null) {
            return;
        }
        try {
            List<String> names = RoomStorage.listPlayerNames(roomFolder);
            if (names.size() < RoomFolder.MIN_PLAYERS_TO_START) {
                statusLabel.setText("At least " + RoomFolder.MIN_PLAYERS_TO_START + " players are required.");
                return;
            }
            openGame(true, names);
        } catch (Exception e) {
            statusLabel.setText("Start failed: " + e.getMessage());
        }
    }

    @FXML
    private void onLocalGameClick() {
        stopLobbyWatch();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
            loader.load();
            GameController controller = loader.getController();
            Scene scene = new Scene(loader.getRoot(), 1200, 800);
            controller.startLocalGame();
            stage.setTitle("Monopoly Deal - Local");
            stage.setScene(scene);
        } catch (IOException e) {
            statusLabel.setText("Unable to open: " + e.getMessage());
        }
    }

    private void openGame(boolean host, List<String> playerNames) throws Exception {
        stopLobbyWatch();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
        loader.load();
        GameController controller = loader.getController();
        Scene scene = new Scene(loader.getRoot(), 1200, 800);
        if (host) {
            controller.startRoomHost(roomFolder, localSeat, playerNames);
        } else {
            controller.startRoomClient(roomFolder, localSeat);
        }
        stage.setTitle("Monopoly Deal - Room " + roomFolder.getRoomCode());
        stage.setScene(scene);
    }

    private void refreshPlayers() {
        if (roomFolder == null) {
            return;
        }
        try {
            List<String> names = RoomStorage.listPlayerNames(roomFolder);
            playerList.getItems().setAll(names);
            if (isHost && !RoomStorage.isStarted(roomFolder)) {
                startBtn.setDisable(names.size() < RoomFolder.MIN_PLAYERS_TO_START);
            }
            if (!isHost && RoomStorage.isStarted(roomFolder)) {
                statusLabel.setText("Game started. Entering...");
                openGame(false, names);
            }
        } catch (Exception e) {
            statusLabel.setText("Refresh failed: " + e.getMessage());
        }
    }

    private void startLobbyWatch() {
        stopLobbyWatch();
        if (roomFolder == null) {
            return;
        }
        try {
            lobbyWatcher = new RoomSyncWatcher();
            lobbyWatcher.start(roomFolder, () -> Platform.runLater(this::refreshPlayers));
        } catch (IOException e) {
            statusLabel.setText("Unable to watch room: " + e.getMessage());
        }
    }

    private void stopLobbyWatch() {
        if (lobbyWatcher != null) {
            lobbyWatcher.close();
            lobbyWatcher = null;
        }
    }
}
