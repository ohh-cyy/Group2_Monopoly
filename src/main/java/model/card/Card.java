package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.UUID;

public abstract class Card {
    private final String instanceId;
    protected final String name;
    protected final String description;
    protected final CardType type;

    public Card(String name, String description, CardType type) {
        this(UUID.randomUUID().toString(), name, description, type);
    }

    public Card(String instanceId, String name, String description, CardType type) {
        this.instanceId = instanceId;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getInstanceId() {
        return instanceId;
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