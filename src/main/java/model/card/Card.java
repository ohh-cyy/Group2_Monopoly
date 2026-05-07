package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.enums.Color;
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

    // 引擎&玩家卡牌信息获取
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CardType getType() { return type; }


    public Color getColor(){
        return null;
    }


    public abstract void use(Player player, GameEngine game);
}