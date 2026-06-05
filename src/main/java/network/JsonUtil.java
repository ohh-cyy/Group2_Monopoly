package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.protocol.ClientMessage;
import network.protocol.ServerMessage;

public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {
    }

    public static String toJson(ClientMessage message) {
        return GSON.toJson(message);
    }

    public static String toJson(ServerMessage message) {
        return GSON.toJson(message);
    }

    public static ClientMessage parseClient(String json) {
        return GSON.fromJson(json, ClientMessage.class);
    }

    public static ServerMessage parseServer(String json) {
        return GSON.fromJson(json, ServerMessage.class);
    }
}
