package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class MoneyCard extends Card implements PayableAsset {
    private final int money;

    public MoneyCard(String name, String description, int money) {
        super(name, description, CardType.MONEY);
        this.money = money;
    }

    public MoneyCard(String instanceId, String name, String description, int money) {
        super(instanceId, name, description, CardType.MONEY);
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    @Override
    public int getPaymentValueM() {
        return money;
    }

    @Override
    public void use(Player player, GameEngine game) {
        // Add to bank only. Bank value is calculated from bank cards.
        player.addBank(this);
    }
}
