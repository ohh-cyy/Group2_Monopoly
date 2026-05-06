package model.card;

import model.enums.CardType;
import model.player.Player;

public class ActionCard extends Card {
    private final String actionType;

    public ActionCard(String name, String description, String actionType) {
        super(name, description, CardType.ACTION);
        this.actionType = actionType;
    }

    public String getActionType() { return actionType; }

    @Override
    public void execute(Player player) {
        // 后续action包实现效果逻辑
    }
}