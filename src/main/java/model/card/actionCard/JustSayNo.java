package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/** 可在对方行动时打出；此处仅作占位效果说明。 */
public class JustSayNo extends ActionCard {

    public JustSayNo(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 完整规则需在对方出牌时响应；单机版存入银行或打出后进入弃牌堆
    }
}
