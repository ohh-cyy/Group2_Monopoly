module com.example.zyzzs {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires javafx.graphics;
    requires com.google.gson;

    opens ui to javafx.fxml;
    opens engine to javafx.base;
    opens model.player to javafx.base;
    opens model.card to javafx.base;
    opens model.enums to javafx.base;
    opens model.achievement to javafx.base;
    opens network to com.google.gson;
    opens network.protocol to com.google.gson;

    exports ui;
    exports engine;
    exports model.player;
    exports model.card;
    exports model.enums;
    exports model.achievement;
    exports controller;
    exports network;
    exports network.protocol;
    exports network.server;
    exports network.client;
    opens controller to javafx.fxml;
}
