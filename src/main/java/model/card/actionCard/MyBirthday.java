package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class MyBirthday extends ActionCard {

    private static final int COLLECT_EACH = 2;

    public MyBirthday(String name, String description, CardType type) {
        super(name, description, type, 2);
    }

    @Override
    public void use(Player player, GameEngine game) {
        for (Player other : game.getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            int paid = Math.min(COLLECT_EACH, other.getMoney());
            other.removeMoney(paid);
            player.addMoney(paid);
        }
    }
}
