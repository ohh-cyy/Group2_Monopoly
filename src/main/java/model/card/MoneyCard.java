package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * A money card that is placed directly in the bank when played.
 * Its face value equals its payment value.
 */
public class MoneyCard extends Card implements PayableAsset {
    /** Face value in millions (M). */
    private final int money;

    /**
     * Creates a money card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param money       face value in millions (M)
     */
    public MoneyCard(String name, String description, int money) {
        super(name, description, CardType.MONEY);
        this.money = money;
    }

    /**
     * Creates a money card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param money       face value in millions (M)
     */
    public MoneyCard(String instanceId, String name, String description, int money) {
        super(instanceId, name, description, CardType.MONEY);
        this.money = money;
    }

    /** @return face value in millions (M) */
    public int getMoney() {
        return money;
    }

    /** @return face value in millions (M), equal to {@link #getMoney()} */
    @Override
    public int getPaymentValueM() {
        return money;
    }

    /** 添加到银行 */
    @Override
    public void use(Player player, GameEngine game) {
        // 添加到银行，银行价值从银行卡片计算
        player.addBank(this);
    }
}
