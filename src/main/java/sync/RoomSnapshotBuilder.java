package sync;

import engine.GameEngine;
import model.card.Card;
import model.card.PropertyCard;
import model.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoomSnapshotBuilder {

    private RoomSnapshotBuilder() {
    }

    public static RoomPublicSnapshot buildPublic(GameEngine engine, List<String> logLines) {
        RoomPublicSnapshot pub = new RoomPublicSnapshot();
        pub.currentPlayerIndex = engine.getCurrentPlayerIndex();
        pub.hasDrawnThisTurn = engine.hasDrawnThisTurn();
        pub.remainingPlays = engine.getRemainingPlays();
        pub.gameOver = engine.isGameOver();
        pub.drawPileSize = engine.getDeck().size();
        pub.discardPileSize = engine.getDiscardPile().size();
        pub.logLines = List.copyOf(logLines);

        for (int i = 0; i < engine.getPlayers().size(); i++) {
            Player player = engine.getPlayers().get(i);
            PlayerPublicSnapshot view = new PlayerPublicSnapshot();
            view.seat = i;
            view.name = player.getName();
            view.handSize = player.getHandSize();
            view.bankTotal = player.getBankTotalValue();
            for (PropertyCard property : player.getAllProperties()) {
                view.properties.add(CardSnapshotMapper.toSnapshot(property));
            }
            pub.players.add(view);

            if (pub.gameOver && pub.winnerName == null && engine.checkWin(player)) {
                pub.winnerName = player.getName();
            }
        }
        return pub;
    }

    public static Map<Integer, PlayerPrivateSnapshot> buildAllPrivate(GameEngine engine) {
        Map<Integer, PlayerPrivateSnapshot> map = new HashMap<>();
        List<Player> players = engine.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            map.put(i, buildPrivate(players.get(i), i));
        }
        return map;
    }

    public static PlayerPrivateSnapshot buildPrivate(Player player, int seat) {
        PlayerPrivateSnapshot priv = new PlayerPrivateSnapshot();
        priv.seat = seat;
        for (Card card : player.getHand()) {
            priv.hand.add(CardSnapshotMapper.toSnapshot(card));
        }
        for (Card card : player.getBank()) {
            priv.bank.add(CardSnapshotMapper.toSnapshot(card));
        }
        return priv;
    }
}
