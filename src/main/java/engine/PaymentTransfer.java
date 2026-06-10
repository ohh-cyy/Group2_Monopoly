package engine;

import model.card.Card;
import model.card.PayableAsset;
import model.card.PropertyCard;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Moves a chosen bank card or property from payer to collector.
 * Only assets outside complete property sets may be paid with.
 */
public final class PaymentTransfer {
    private PaymentTransfer() {
    }

    /** Lists bank cards plus properties outside complete sets that may be paid. */
    public static List<Card> listPayableAssets(Player player) {
        List<Card> options = new ArrayList<>();
        if (player == null) {
            return options;
        }
        options.addAll(player.getBank());
        options.addAll(PropertyRules.getPayableProperties(player));
        return options;
    }

    /** True when the player holds at least one bank card or payable property. */
    public static boolean hasPayableAsset(Player player) {
        return player != null
                && (!player.getBank().isEmpty() || !PropertyRules.getPayableProperties(player).isEmpty());
    }

    /** True when {@code card} is in the payer's bank or payable property list. */
    public static boolean isPayableAsset(Player player, Card card) {
        if (player == null || card == null) {
            return false;
        }
        if (player.getBank().contains(card)) {
            return true;
        }
        return card instanceof PropertyCard property && PropertyRules.canPayWithProperty(player, property);
    }

    /**
     * Transfers one payable card identified by {@code cardId} from payer to collector.
     * @return the payment value in millions, or empty if the transfer failed
     */
    public static OptionalInt payWithCard(Player collector, Player payer, String cardId) {
        if (collector == null || payer == null || cardId == null || cardId.isBlank()) {
            return OptionalInt.empty();
        }
        Card card = findPayableCard(payer, cardId);
        if (card == null || !isPayableAsset(payer, card)) {
            return OptionalInt.empty();
        }
        int value = getPaymentValue(card);
        movePaymentCard(collector, payer, card);
        return OptionalInt.of(value);
    }

    /** Locates a payable bank or property card on the payer by instance id. */
    public static Card findPayableCard(Player payer, String cardId) {
        for (Card card : payer.getBank()) {
            if (cardId.equals(card.getInstanceId())) {
                return card;
            }
        }
        for (PropertyCard property : PropertyRules.getPayableProperties(payer)) {
            if (cardId.equals(property.getInstanceId())) {
                return property;
            }
        }
        return null;
    }

    /** Moves one bank card or payable property from payer to collector. */
    public static void movePaymentCard(Player collector, Player payer, Card card) {
        if (payer.getBank().contains(card)) {
            payer.removeFromBank(card);
            collector.addBank(card);
            return;
        }
        if (card instanceof PropertyCard property && PropertyRules.canPayWithProperty(payer, property)) {
            payer.removeProperty(property);
            if (!collector.addProperty(property)) {
                payer.addProperty(property);
            }
        }
    }

    /** Face value in millions for a bank note or property used as payment. */
    public static int getPaymentValue(Card card) {
        if (card instanceof PayableAsset payableAsset) {
            return payableAsset.getPaymentValueM();
        }
        return 0;
    }
}
