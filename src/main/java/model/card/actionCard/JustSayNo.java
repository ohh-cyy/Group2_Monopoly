package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * 响应牌：当其他玩家对你打出行动牌效果时可打出，取消该效果。
 * 实际响应逻辑在 GameController 中处理。
 */
public class JustSayNo extends ActionCard {

    public JustSayNo(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    public JustSayNo(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 4);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 仅作为响应打出，由控制器从手牌移除并放入弃牌堆
    }
}
