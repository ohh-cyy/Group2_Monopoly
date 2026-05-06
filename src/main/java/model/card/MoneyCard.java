package model.card;

import model.enums.CardType;
import model.player.Player;

public class MoneyCard extends Card {

    private final int value;

    // 构造方法
    public MoneyCard(String name, String description, int value) {
        super(name, description, CardType.MONEY);
        this.value = value;
    }


    public int getValue() {
        return value;
    }

    // 实现父类抽象方法（空框架，不写逻辑）
    @Override
    public void execute(Player player) {
        // 后续action包实现效果逻辑
    }
}