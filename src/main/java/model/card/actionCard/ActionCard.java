package model.card.actionCard;

import model.card.Card;
import model.enums.CardType;

public abstract class ActionCard extends Card {
    public ActionCard(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

}
