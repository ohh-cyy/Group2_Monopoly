package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.card.PayableAsset;
import model.enums.CardType;
import model.player.Player;

/**
 * Base class for action cards. An action card can be played for its effect
 * or placed in the player's bank for its money value.
 */
public abstract class ActionCard extends Card implements PayableAsset {
    /** Monetary value when deposited into the bank as cash (in millions). */
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

    @Override
    public int getPaymentValueM() {
        return bankValueM;
    }

    /**
     * Places this action card in the player's bank as money instead of
     * triggering its effect. The card's bank value is added to the bank total.
     */
    public void depositToBank(Player player) {
        if (player != null) {
            player.addBank(this);
        }
    }

    /**
     * Executes the action card's effect. Subclasses whose effect requires
     * target selection (e.g. SlyDeal, DealBreaker) leave this body empty;
     * the controller calls their specialised methods directly.
     */
    @Override
    public abstract void use(Player player, GameEngine game);
}
