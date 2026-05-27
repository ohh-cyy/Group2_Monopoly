package ui;

import controller.LobbyController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MonopolyDealApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/lobby-view.fxml"));
        loader.load();
        LobbyController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(loader.getRoot(), 520, 420);
        primaryStage.setTitle("Monopoly Deal - Lobby");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
