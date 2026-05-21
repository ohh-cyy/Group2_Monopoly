package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/** 须与收租牌一起使用；此处标记效果已激活。 */
public class DoubleTheRent extends ActionCard {

    public DoubleTheRent(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    @Override
    public void use(Player player, GameEngine game) {
        game.setRentDoubled(true);
    }
}
