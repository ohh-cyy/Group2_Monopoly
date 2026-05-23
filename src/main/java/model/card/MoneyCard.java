package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class MoneyCard extends Card {
    private final int money;

    public MoneyCard(String name, String description, int money) {
        super(name, description, CardType.MONEY);
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 只放入银行区；面值由 getBank() 中的牌计算，勿再 addMoney 以免总额翻倍
        player.addBank(this);
    }
}
