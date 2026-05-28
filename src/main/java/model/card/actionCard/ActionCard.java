package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

/**
 * 行动牌可存入银行（面值 {@link #bankValueM}）或打出使用效果。
 */
public abstract class ActionCard extends Card {
    private final int bankValueM;

    public ActionCard(String name, String description, CardType type, int bankValueM) {
        super(name, description, type);
        this.bankValueM = bankValueM;
    }

    public ActionCard(String instanceId, String name, String description, CardType type, int bankValueM) {
        super(instanceId, name, description, type);
        this.bankValueM = bankValueM;
    }

    public int getBankValueM() {
        return bankValueM;
    }

    /** 作为现金存入银行（牌保留在银行区，不进入弃牌堆） */
    public void depositToBank(Player player) {
        player.addBank(this);
    }

    @Override
    public abstract void use(Player player, GameEngine game);
}
