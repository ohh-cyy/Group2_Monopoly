package model.card;

import model.enums.CardType;
import model.player.Player;

public abstract class Card {
    protected final String name;
    protected final String description;
    protected final CardType type;

    public Card(String name, String description, CardType type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    // 通用Getter
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CardType getType() { return type; }

    // 抽象方法：子类实现，空框架不写逻辑
    public abstract void execute(Player player);
}