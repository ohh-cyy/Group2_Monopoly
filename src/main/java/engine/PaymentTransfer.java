package engine;

import model.card.Card;
import model.card.PayableAsset;
import model.card.PropertyCard;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 *玩家之间的财产转移，只有不在完整套的财产可以支付
 */
public final class PaymentTransfer {
    private PaymentTransfer() {
    }

    /** 列出玩家手里的银行卡和不在完整套的property卡 */
    public static List<Card> listPayableAssets(Player player) {
        List<Card> options = new ArrayList<>();
        if (player == null) {
            return options;
        }
        options.addAll(player.getBank());
        options.addAll(PropertyRules.getPayableProperties(player));
        return options;
    }

    /** 玩家手里有没有银行卡或者不在完整套的property卡 */
    public static boolean hasPayableAsset(Player player) {
        return player != null
                && (!player.getBank().isEmpty() || !PropertyRules.getPayableProperties(player).isEmpty());
    }

    /** 判断卡片是不是在玩家手里 */
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
     * 转移一张卡片，通过ID
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

    /** 通过ID找到卡片 */
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

    /** 转移一张卡片 */
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

    /** 获取卡片价值 */
    public static int getPaymentValue(Card card) {
        if (card instanceof PayableAsset payableAsset) {
            return payableAsset.getPaymentValueM();
        }
        return 0;
    }
}
