package engine;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Moves a chosen bank card or property from payer to collector. */
public final class PaymentTransfer {
    private PaymentTransfer() {
    }

    public static List<Card> listPayableAssets(Player player) {
        List<Card> options = new ArrayList<>();
        if (player == null) {
            return options;
        }
        options.addAll(player.getBank());
        options.addAll(player.getAllProperties());
        return options;
    }

    public static boolean hasPayableAsset(Player player) {
        return player != null
                && (!player.getBank().isEmpty() || !player.getAllProperties().isEmpty());
    }

    public static OptionalInt payWithCard(Player collector, Player payer, String cardId) {
        if (collector == null || payer == null || cardId == null || cardId.isBlank()) {
            return OptionalInt.empty();
        }
        Card card = findPayableCard(payer, cardId);
        if (card == null) {
            return OptionalInt.empty();
        }
        int value = getPaymentValue(card);
        movePaymentCard(collector, payer, card);
        return OptionalInt.of(value);
    }

    public static Card findPayableCard(Player payer, String cardId) {
        for (Card card : payer.getBank()) {
            if (cardId.equals(card.getInstanceId())) {
                return card;
            }
        }
        for (PropertyCard property : payer.getAllProperties()) {
            if (cardId.equals(property.getInstanceId())) {
                return property;
            }
        }
        return null;
    }

    public static void movePaymentCard(Player collector, Player payer, Card card) {
        if (payer.getBank().contains(card)) {
            payer.removeFromBank(card);
            collector.addBank(card);
            return;
        }
        if (card instanceof PropertyCard property && payer.getAllProperties().contains(property)) {
            payer.removeProperty(property);
            collector.addProperty(property);
        }
    }

    public static int getPaymentValue(Card card) {
        if (card instanceof MoneyCard moneyCard) {
            return moneyCard.getMoney();
        }
        if (card instanceof ActionCard actionCard) {
            return actionCard.getBankValueM();
        }
        if (card instanceof WildpropertyCard wildCard) {
            return wildCard.getBankValueM();
        }
        if (card instanceof PropertyCard propertyCard) {
            return Math.max(1, propertyCard.getPrice());
        }
        return 0;
    }
}
