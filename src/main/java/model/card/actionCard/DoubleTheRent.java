package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * 必须与租金牌在同一回合内紧接着使用：先打本牌，再打 Rent，租金翻倍。
 */
public class DoubleTheRent extends ActionCard {

    public DoubleTheRent(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    @Override
    public void use(Player player, GameEngine game) {
        game.setRentDoubled(true);
    }

    public boolean activateForNextRent(GameEngine game) {
        if (game.isRentDoubled()) {
            return false;
        }
        game.setRentDoubled(true);
        return true;
    }
}
