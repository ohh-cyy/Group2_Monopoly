module com.example.zyzzs {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens ui to javafx.fxml;
    opens engine to javafx.base;
    opens model.player to javafx.base;
    opens model.card to javafx.base;
    opens model.enums to javafx.base;
    
    exports ui;
    exports engine;
    exports model.player;
    exports model.card;
    exports model.enums;
    exports action;
    exports controller;
    opens controller to javafx.fxml;
}