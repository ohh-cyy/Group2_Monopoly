package engine;

import model.card.Card;
import model.card.PayableAsset;
import model.card.PropertyCard;
import model.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 强制扣款
 */
public final class RentPayment {
    private RentPayment() {
    }

    /**
     * 强制扣款，最多扣amountM
     * 先扣银行卡，再扣property
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
            int value = property.getPaymentValueM();
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
    @Deprecated
    public static int collect(Player collector, Player debtor, int amountM) {
        return collectUpTo(collector, debtor, amountM);
    }

    /** Smallest payable unit (bank or property) the player can offer; 0 if none. */
    public static int getMinimumPayableUnit(Player player) {
        int min = Integer.MAX_VALUE;

        for (Card card : player.getBank()) {
            int value = getCardValue(card);
            if (value > 0) {
                min = Math.min(min, value);
            }
        }

        for (PropertyCard property : PropertyRules.getPayableProperties(player)) {
            int value = property.getPaymentValueM();
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
        list.sort(Comparator.comparingInt(PropertyCard::getPaymentValueM));
        return list.get(0);
    }

    private static int getCardValue(Card card) {
        if (card instanceof PayableAsset payableAsset) {
            return payableAsset.getPaymentValueM();
        }
        return 0;
    }
}
