package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

/**
 * Base class for action cards. An action card can be played for its effect
 * or placed in the player's bank for its money value.
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

    /**
     * Places this action card in the player's bank instead of using its effect.
     */
    public void depositToBank(Player player) {
        if (player != null) {
            player.addBank(this);
        }
    }

    @Override
    public abstract void use(Player player, GameEngine game);
}
