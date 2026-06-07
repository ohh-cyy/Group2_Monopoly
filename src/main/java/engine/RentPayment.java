package engine;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collects payments from a player's bank or properties.
 */
public final class RentPayment {
    private RentPayment() {
    }

    /**
     * Collects up to amountM from the debtor.
     * Bank cards are paid first, then properties if needed.
     *
     * @return the actual amount collected
     */
    public static int collectUpTo(Player collector, Player debtor, int amountM) {
        if (amountM <= 0 || debtor == null || collector == null || debtor.equals(collector)) {
            return 0;
        }

        int paid = 0;

        while (paid < amountM) {
            Card card = findSmallestBankCard(debtor);
            if (card == null) {
                break;
            }
            int value = getCardValue(card);
            debtor.removeFromBank(card);
            collector.addBank(card);
            paid += value;
        }

        while (paid < amountM) {
            PropertyCard property = findSmallestProperty(debtor);
            if (property == null) {
                break;
            }
            int value = Math.max(1, property.getPrice());
            debtor.removeProperty(property);
            if (!collector.addProperty(property)) {
                debtor.addProperty(property);
                break;
            }
            paid += value;
        }

        return paid;
    }

    /** @deprecated Use {@link #collectUpTo}. */
    public static int collect(Player collector, Player debtor, int amountM) {
        return collectUpTo(collector, debtor, amountM);
    }

    public static int getMinimumPayableUnit(Player player) {
        int min = Integer.MAX_VALUE;

        for (Card card : player.getBank()) {
            int value = getCardValue(card);
            if (value > 0) {
                min = Math.min(min, value);
            }
        }

        for (PropertyCard property : PropertyRules.getPayableProperties(player)) {
            int value = Math.max(1, property.getPrice());
            if (value > 0) {
                min = Math.min(min, value);
            }
        }

        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static Card findSmallestBankCard(Player player) {
        Card best = null;
        int bestVal = Integer.MAX_VALUE;
        for (Card card : player.getBank()) {
            int v = getCardValue(card);
            if (v > 0 && v < bestVal) {
                bestVal = v;
                best = card;
            }
        }
        return best;
    }

    private static PropertyCard findSmallestProperty(Player player) {
        List<PropertyCard> list = new ArrayList<>(PropertyRules.getPayableProperties(player));
        if (list.isEmpty()) {
            return null;
        }
        list.sort(Comparator.comparingInt(p -> Math.max(1, p.getPrice())));
        return list.get(0);
    }

    private static int getCardValue(Card card) {
        if (card instanceof MoneyCard moneyCard) {
            return moneyCard.getMoney();
        }
        if (card instanceof ActionCard actionCard) {
            return actionCard.getBankValueM();
        }
        if (card instanceof WildpropertyCard wildCard) {
            return wildCard.getBankValueM();
        }
        return 0;
    }
}
