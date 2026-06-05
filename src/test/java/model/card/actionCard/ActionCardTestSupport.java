package model.card.actionCard;

import engine.Deck;
import engine.GameEngine;
import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

final class ActionCardTestSupport {
    private ActionCardTestSupport() {
    }

    static Player player(String name) {
        return new Player(name);
    }

    static MoneyCard money(int value) {
        return new MoneyCard(value + "M", "Money", value);
    }

    static PropertyCard property(String name, Color color, int price) {
        return new PropertyCard(name, color + " property", color, price);
    }

    static List<PropertyCard> addCompleteSet(Player player, Color color) {
        List<PropertyCard> set = new ArrayList<>();
        for (int i = 1; i <= color.getSetSize(); i++) {
            PropertyCard property = property(color + " " + i, color, i);
            player.addProperty(property);
            set.add(property);
        }
        return set;
    }

    static Deck deckWith(Card... cards) {
        return new Deck(List.of(cards));
    }

    static GameEngine game(Player... players) {
        return new GameEngine(List.of(players), deckWith(
                money(1), money(1), money(2), money(2), money(3), money(3), money(5)
        ));
    }
}
