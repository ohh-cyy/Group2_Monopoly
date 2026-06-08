package ui.render;

import model.card.Card;
import model.player.Player;
import network.protocol.PlayerViewDto;

import java.util.ArrayList;
import java.util.List;

/** Read-only snapshot for rendering a player's public board area. */
public final class PlayerBoardView {
    public int seat;
    public String name;
    public int handSize;
    public int bankTotal;
    public final List<Card> properties = new ArrayList<>();

    public static List<PlayerBoardView> fromPlayers(List<Player> players) {
        if (players == null) {
            return List.of();
        }
        List<PlayerBoardView> views = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            views.add(fromPlayer(players.get(i), i));
        }
        return views;
    }

    public static PlayerBoardView fromPlayer(Player player, int seat) {
        PlayerBoardView view = new PlayerBoardView();
        view.seat = seat;
        view.name = player.getName();
        view.handSize = player.getHandSize();
        view.bankTotal = player.getBankTotalValue();
        view.properties.addAll(player.getAllProperties());
        return view;
    }

    public static List<PlayerBoardView> fromDtos(List<PlayerViewDto> players, int localSeat) {
        if (players == null) {
            return List.of();
        }
        List<PlayerBoardView> views = new ArrayList<>();
        for (PlayerViewDto dto : players) {
            views.add(fromDto(dto, localSeat));
        }
        return views;
    }

    public static PlayerBoardView fromDto(PlayerViewDto dto, int localSeat) {
        PlayerBoardView view = new PlayerBoardView();
        view.seat = dto.seat;
        view.name = dto.name + (dto.seat == localSeat ? " (You)" : "");
        view.handSize = dto.handSize;
        view.bankTotal = dto.bankTotal;
        if (dto.properties != null) {
            for (var cardDto : dto.properties) {
                Card card = network.CardMapper.fromDto(cardDto);
                if (card != null) {
                    view.properties.add(card);
                }
            }
        }
        return view;
    }
}
