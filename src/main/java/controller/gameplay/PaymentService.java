package controller.gameplay;

import engine.PaymentTransfer;
import engine.PropertyRules;
import controller.dialog.GameDialogService;
import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles required payments and lets the payer choose each asset.
 */
public class PaymentService {
    private final GameDialogService dialogs;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    public PaymentService(GameDialogService dialogs, Consumer<String> log, BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.log = log;
        this.status = status;
    }

    public int collectPaymentByChoice(Player collector, Player payer, int amountM, String reason) {
        if (collector == null || payer == null || collector.equals(payer) || amountM <= 0) {
            return 0;
        }

        int paid = 0;
        while (paid < amountM && hasPayableAsset(payer)) {
            int remaining = amountM - paid;
            Optional<Card> chosen = promptSelectPaymentCard(payer, remaining, reason);
            if (chosen.isEmpty()) {
                status.accept("Payment is required while the player still has assets.", true);
                continue;
            }
            Card card = chosen.get();
            if (!PaymentTransfer.isPayableAsset(payer, card)) {
                status.accept("Properties in complete sets cannot be used to pay rent.", true);
                continue;
            }
            int value = getPaymentValue(card);
            movePaymentCard(collector, payer, card);
            paid += value;
            log.accept(payer.getName() + " paid " + card.getName() + " (" + value + "M) to "
                    + collector.getName());
        }
        return paid;
    }

    private boolean hasPayableAsset(Player player) {
        return PaymentTransfer.hasPayableAsset(player);
    }

    private Optional<Card> promptSelectPaymentCard(Player payer, int remainingM, String reason) {
        List<Card> options = PaymentTransfer.listPayableAssets(payer);
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return dialogs.showChoiceDialog(
                "Choose Payment Asset",
                payer.getName() + " must pay " + remainingM + "M",
                reason + "\nChoose one bank card or property to pay. Properties in complete sets cannot be used.",
                options,
                card -> card.getName() + " (" + describePaymentCard(card) + ")",
                this::paymentCardStyle);
    }

    private void movePaymentCard(Player collector, Player payer, Card card) {
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

    private int getPaymentValue(Card card) {
        return PaymentTransfer.getPaymentValue(card);
    }

    private String describePaymentCard(Card card) {
        if (card instanceof PropertyCard propertyCard) {
            return propertyCard.getColor() + ", " + getPaymentValue(card) + "M";
        }
        return getPaymentValue(card) + "M";
    }

    private String paymentCardStyle(Card card) {
        if (card instanceof PropertyCard propertyCard && propertyCard.getColor() != null) {
            Color color = propertyCard.getColor();
            return "-fx-border-color: " + dialogs.cssColorFor(color) + ";";
        }
        if (card instanceof MoneyCard) {
            return "-fx-border-color: #27ae60;";
        }
        return "-fx-border-color: #d64545;";
    }
}
