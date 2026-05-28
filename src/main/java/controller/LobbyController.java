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

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LobbyController {
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
    private Timer pollTimer;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        startPollTimer();
    }

    @FXML
    private void onHostClick() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("请输入昵称");
            return;
        }
        try {
            roomFolder = RoomStorage.createRoom(name);
            localSeat = 0;
            isHost = true;
            roomCodeField.setText(roomFolder.getRoomCode());
            roomCodeLabel.setText("房间号: " + roomFolder.getRoomCode());
            statusLabel.setText("房间已创建。把房间号发给其他玩家，等人齐后点「开始游戏」。");
            startBtn.setDisable(false);
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            refreshPlayers();
        } catch (IOException e) {
            statusLabel.setText("创建失败: " + e.getMessage());
        }
    }

    @FXML
    private void onJoinClick() {
        String name = nameField.getText().trim();
        String code = roomCodeField.getText().trim();
        if (name.isEmpty() || code.isEmpty()) {
            statusLabel.setText("请输入昵称和房间号");
            return;
        }
        try {
            roomFolder = RoomStorage.openRoom(code);
            if (RoomStorage.isStarted(roomFolder)) {
                statusLabel.setText("游戏已开始，无法加入");
                return;
            }
            localSeat = RoomStorage.joinNextSeat(roomFolder, name);
            isHost = false;
            roomCodeLabel.setText("已加入房间: " + roomFolder.getRoomCode());
            statusLabel.setText("已加入，等待 Host 开始游戏…");
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            startBtn.setDisable(true);
            refreshPlayers();
        } catch (IOException e) {
            statusLabel.setText("加入失败: " + e.getMessage());
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
                statusLabel.setText("至少需要 " + RoomFolder.MIN_PLAYERS_TO_START + " 名玩家");
                return;
            }
            openGame(true, names);
        } catch (Exception e) {
            statusLabel.setText("开始失败: " + e.getMessage());
        }
    }

    @FXML
    private void onLocalGameClick() {
        stopPollTimer();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
            loader.load();
            GameController controller = loader.getController();
            Scene scene = new Scene(loader.getRoot(), 1200, 800);
            controller.startLocalGame();
            stage.setTitle("Monopoly Deal - 单机");
            stage.setScene(scene);
        } catch (IOException e) {
            statusLabel.setText("无法打开: " + e.getMessage());
        }
    }

    private void openGame(boolean host, List<String> playerNames) throws Exception {
        stopPollTimer();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
        loader.load();
        GameController controller = loader.getController();
        Scene scene = new Scene(loader.getRoot(), 1200, 800);
        if (host) {
            controller.startRoomHost(roomFolder, localSeat, playerNames);
        } else {
            controller.startRoomClient(roomFolder, localSeat);
        }
        stage.setTitle("Monopoly Deal - 房间 " + roomFolder.getRoomCode());
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
                statusLabel.setText("游戏已开始，正在进入…");
                try {
                    openGame(false, names);
                } catch (Exception ex) {
                    statusLabel.setText("进入游戏失败: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            statusLabel.setText("刷新失败: " + e.getMessage());
        }
    }

    private void startPollTimer() {
        pollTimer = new Timer("lobby-poll", true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (roomFolder != null) {
                    Platform.runLater(LobbyController.this::refreshPlayers);
                }
            }
        }, 500, 500);
    }

    private void stopPollTimer() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
        }
    }
}
