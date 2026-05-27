package ui;

import network.server.GameServer;

/** 联机服务端入口（无 JavaFX） */
public class ServerMain {
    public static void main(String[] args) {
        int port = 47390;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            new GameServer(port).start();
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
