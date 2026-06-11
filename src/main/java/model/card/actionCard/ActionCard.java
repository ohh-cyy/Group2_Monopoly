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

    /**
     * Creates an action card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (typically {@link CardType#ACTION})
     * @param bankValueM  bank deposit value in millions (M)
     */
    public ActionCard(String name, String description, CardType type, int bankValueM) {
        super(name, description, type);
        this.bankValueM = bankValueM;
    }

    /**
     * Creates an action card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (typically {@link CardType#ACTION})
     * @param bankValueM  bank deposit value in millions (M)
     */
    public ActionCard(String instanceId, String name, String description, CardType type, int bankValueM) {
        super(instanceId, name, description, type);
        this.bankValueM = bankValueM;
    }

    /** @return bank deposit value in millions (M) */
    public int getBankValueM() {
        return bankValueM;
    }

    /** 返回银行价值 */
    @Override
    public int getPaymentValueM() {
        return bankValueM;
    }

    /**
     * 添加到银行
     */
    public void depositToBank(Player player) {
        if (player != null) {
            player.addBank(this);
        }
    }

    /**
     * 执行卡片效果
     */
    @Override
    public abstract void use(Player player, GameEngine game);
}
