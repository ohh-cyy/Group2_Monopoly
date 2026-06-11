package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.protocol.ClientMessage;
import network.protocol.ServerMessage;

/**
 * 基于行的网络协议 JSON 序列化。
 * 每条消息为一个 JSON 对象，通过 TCP 单行传输。
 */
public final class JsonUtil {
    /** 共享 Gson 实例；协议 DTO 使用 public 字段以自动映射。 */
    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {
    }

    /** 将发出的客户端命令序列化为 JSON 字符串。 */
    public static String toJson(ClientMessage message) {
        return GSON.toJson(message);
    }

    /** 将发出的服务端消息序列化为 JSON 字符串。 */
    public static String toJson(ServerMessage message) {
        return GSON.toJson(message);
    }

    /** 将客户端一行 JSON 解析为 {@link ClientMessage}。 */
    public static ClientMessage parseClient(String json) {
        return GSON.fromJson(json, ClientMessage.class);
    }

    /** 将服务端一行 JSON 解析为 {@link ServerMessage}。 */
    public static ServerMessage parseServer(String json) {
        return GSON.fromJson(json, ServerMessage.class);
    }
}
