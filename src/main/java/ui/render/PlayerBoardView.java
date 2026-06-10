package ui.render;

import model.card.Card;
import model.player.Player;
import network.protocol.PlayerViewDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only snapshot of a player's public-board data for rendering.
 * <p>
 * Factory methods convert domain {@link Player} objects or network
 * {@link PlayerViewDto} records into views used by board renderers.
 */
public final class PlayerBoardView {
    /** Zero-based seat index of this player. */
    public int seat;
    /** Display name, optionally annotated for the local player. */
    public String name;
    /** Number of cards in hand (hidden detail). */
    public int handSize;
    /** Total bank value in millions. */
    public int bankTotal;
    /** All property cards owned by this player. */
    public final List<Card> properties = new ArrayList<>();

    /** Converts a list of domain players into board views with sequential seats. */
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

    /** Converts a single domain player into a board view. */
    public static PlayerBoardView fromPlayer(Player player, int seat) {
        PlayerBoardView view = new PlayerBoardView();
        view.seat = seat;
        view.name = player.getName();
        view.handSize = player.getHandSize();
        view.bankTotal = player.getBankTotalValue();
        view.properties.addAll(player.getAllProperties());
        return view;
    }

    /** Converts network DTOs into board views, marking the local seat as {@code (You)}. */
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

    /** Converts a single network DTO into a board view. */
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
