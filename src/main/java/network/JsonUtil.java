package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.protocol.ClientMessage;
import network.protocol.ServerMessage;

/**
 * JSON serialization for the line-based network protocol.
 * Each message is one JSON object written to a single line over TCP.
 */
public final class JsonUtil {
    /** Shared Gson instance; protocol DTOs use public fields for automatic mapping. */
    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {
    }

    /** Serializes an outbound client command to a JSON string. */
    public static String toJson(ClientMessage message) {
        return GSON.toJson(message);
    }

    /** Serializes an outbound server message to a JSON string. */
    public static String toJson(ServerMessage message) {
        return GSON.toJson(message);
    }

    /** Parses one line of JSON from a client into a {@link ClientMessage}. */
    public static ClientMessage parseClient(String json) {
        return GSON.fromJson(json, ClientMessage.class);
    }

    /** Parses one line of JSON from the server into a {@link ServerMessage}. */
    public static ServerMessage parseServer(String json) {
        return GSON.fromJson(json, ServerMessage.class);
    }
}
