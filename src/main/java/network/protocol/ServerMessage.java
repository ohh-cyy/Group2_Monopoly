package network.protocol;

import java.util.ArrayList;
import java.util.List;

public class ServerMessage {
    public String type;
    public String text;
    public int seat = -1;
    public boolean host;
    public boolean youAreHost;
    public String emoji;
    public GameStateDto state;
    public List<LobbyPlayerDto> lobbyPlayers = new ArrayList<>();
    public InteractionPromptDto prompt;
}
