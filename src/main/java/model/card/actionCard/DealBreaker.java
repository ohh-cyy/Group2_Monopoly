package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.List;

public class DealBreaker extends ActionCard {

    public DealBreaker(String name, String description, CardType type) {
        super(name, description, type, 5);
    }

    public DealBreaker(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 5);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 由界面选择目标后调用 useOnTarget
    }

    /**
     * 从目标玩家偷走一整套指定颜色的地产。
     * @return 是否成功偷取
     */
    public boolean useOnTarget(Player player, Player target, Color color) {
        if (target == null || color == null || player.equals(target)) {
            return false;
        }
        if (!target.hasCompleteSet(color)) {
            return false;
        }
        List<PropertyCard> stolen = target.removePropertySet(color);
        for (PropertyCard property : stolen) {
            player.addProperty(property);
        }
        return !stolen.isEmpty();
    }
}
