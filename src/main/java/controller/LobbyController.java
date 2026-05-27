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
import network.client.NetworkClient;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import network.server.GameSession;

import java.io.IOException;

public class LobbyController {
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField nameField;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> waitingList;
    @FXML
    private Button connectBtn;

    private NetworkClient client;
    private Stage stage;
    private int yourSeat = -1;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onConnectClick() {
        String host = hostField.getText().trim();
        String name = nameField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("端口必须是数字");
            return;
        }
        if (name.isEmpty()) {
            statusLabel.setText("请输入昵称");
            return;
        }
        if (client != null) {
            client.disconnect();
        }

        connectBtn.setDisable(true);
        statusLabel.setText("正在连接 " + host + ":" + port + " ...");

        client = new NetworkClient();
        client.setMessageListener(this::handleServerMessage);

        new Thread(() -> {
            try {
                client.connect(host, port);
                client.join(name);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("连接失败: " + e.getMessage());
                    connectBtn.setDisable(false);
                });
            }
        }, "lobby-connect").start();
    }

    @FXML
    private void onLocalGameClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
            GameController controller = new GameController();
            loader.setController(controller);
            Scene scene = new Scene(loader.load(), 1200, 800);
            stage.setTitle("Monopoly Deal - 单机");
            stage.setScene(scene);
        } catch (IOException e) {
            statusLabel.setText("无法打开游戏: " + e.getMessage());
        }
    }

    private void handleServerMessage(ServerMessage message) {
        Platform.runLater(() -> processMessage(message));
    }

    private void processMessage(ServerMessage message) {
        if (message == null) {
            return;
        }
        switch (message.type) {
            case MessageTypes.JOINED -> {
                yourSeat = message.yourSeat;
                statusLabel.setText(message.message + "（" + message.waitingCount
                        + "/" + GameSession.MAX_PLAYERS + " 人）");
                refreshWaitingList(message.waitingCount);
            }
            case MessageTypes.WAITING -> {
                statusLabel.setText(message.message);
                refreshWaitingList(message.waitingCount);
            }
            case MessageTypes.GAME_STARTED -> {
                statusLabel.setText(message.message);
                openNetworkGame();
            }
            case MessageTypes.ERROR -> {
                statusLabel.setText(message.message);
                connectBtn.setDisable(false);
            }
            default -> {
            }
        }
    }

    private void refreshWaitingList(int count) {
        waitingList.getItems().clear();
        for (int i = 1; i <= count; i++) {
            waitingList.getItems().add("玩家 " + i + (i - 1 == yourSeat ? "（你）" : ""));
        }
        while (waitingList.getItems().size() < GameSession.MAX_PLAYERS) {
            waitingList.getItems().add("等待加入...");
        }
    }

    private void openNetworkGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
            NetworkGameController controller = new NetworkGameController();
            loader.setController(controller);
            Scene scene = new Scene(loader.load(), 1200, 800);
            controller.attach(client, yourSeat);
            stage.setTitle("Monopoly Deal - 联机");
            stage.setScene(scene);
        } catch (IOException e) {
            statusLabel.setText("无法进入游戏: " + e.getMessage());
            connectBtn.setDisable(false);
        }
    }
}
