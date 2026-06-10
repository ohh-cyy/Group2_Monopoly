package controller.network;

import controller.dialog.GameDialogService;
import engine.PaymentTransfer;
import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import network.CardMapper;
import network.client.NetworkClient;
import network.protocol.InteractionPromptDto;
import network.server.PendingActionResolution;
import ui.StatusMessageDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Handles server-driven interaction prompts during online play.
 * <p>
 * Responds to {@link network.protocol.InteractionPromptDto} messages for Just Say No
 * chains and rent/payment asset selection, sending answers back via {@link NetworkClient}.
 */
public final class NetworkPromptResponder {
    private final GameDialogService dialogs;
    private final StatusMessageDisplay statusDisplay;
    private final BiConsumer<String, Boolean> statusFallback;

    /**
     * @param dialogs        themed dialog factory for prompt UI
     * @param statusDisplay  primary status message presenter
     * @param statusFallback used when {@code statusDisplay} is unavailable
     */
    public NetworkPromptResponder(GameDialogService dialogs,
                                  StatusMessageDisplay statusDisplay,
                                  BiConsumer<String, Boolean> statusFallback) {
        this.dialogs = dialogs;
        this.statusDisplay = statusDisplay;
        this.statusFallback = statusFallback;
    }

    /**
     * Dispatches a server prompt to the appropriate dialog and response handler.
     *
     * @param client connected client used to send the player's answer
     * @param prompt prompt metadata and payable options from the server
     */
    public void handle(NetworkClient client, InteractionPromptDto prompt) {
        if (client == null || prompt == null || prompt.promptId == null) {
            return;
        }
        if (PendingActionResolution.PROMPT_JUST_SAY_NO.equals(prompt.promptType)) {
            respondJustSayNo(client, prompt);
        } else if (PendingActionResolution.PROMPT_PAYMENT.equals(prompt.promptType)) {
            respondPayment(client, prompt);
        }
    }

    private void respondJustSayNo(NetworkClient client, InteractionPromptDto prompt) {
        var playBtn = new javafx.scene.control.ButtonType("Play Just Say No");
        var allowBtn = new javafx.scene.control.ButtonType("Allow Effect");
        var cancelBtn = new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        String header = prompt.responseDepth == 0
                ? "Block " + prompt.attackerName + "'s action?"
                : "Counter the previous Just Say No?";
        String content = prompt.responseDepth == 0
                ? prompt.actionName + "\n\nPlaying Just Say No cancels this effect."
                : prompt.actionName + "\n\nPlaying Just Say No cancels the previous Just Say No.";
        Optional<javafx.scene.control.ButtonType> choice = dialogs.showButtonDialog(
                "Just Say No", header, content, playBtn, allowBtn, cancelBtn);
        boolean useJustSayNo = choice.isPresent() && choice.get() == playBtn;
        client.respond(prompt.promptId, useJustSayNo, null);
        showStatus(useJustSayNo ? "You played Just Say No." : "You allowed the effect.", false);
    }

    private void respondPayment(NetworkClient client, InteractionPromptDto prompt) {
        List<Card> options = new ArrayList<>();
        if (prompt.payableCards != null) {
            for (var dto : prompt.payableCards) {
                Card card = CardMapper.fromDto(dto);
                if (card != null) {
                    options.add(card);
                }
            }
        }
        if (options.isEmpty()) {
            showStatus("No assets available to pay.", true);
            return;
        }
        Optional<Card> chosen = dialogs.showChoiceDialog(
                "Choose Payment Asset",
                "You must pay " + prompt.remainingDue + "M",
                prompt.actionName + "\nChoose one bank card or property to pay. Properties in complete sets cannot be used.",
                options,
                this::describePaymentCard,
                this::paymentCardStyle);
        if (chosen.isEmpty()) {
            respondPayment(client, prompt);
            return;
        }
        client.respond(prompt.promptId, null, chosen.get().getInstanceId());
        showStatus("Paid with " + chosen.get().getName(), false);
    }

    private String describePaymentCard(Card card) {
        if (card instanceof PropertyCard propertyCard) {
            Color color = propertyCard.getColor() != null ? propertyCard.getColor() : Color.BROWN;
            return card.getName() + " (" + color + ", " + PaymentTransfer.getPaymentValue(card) + "M)";
        }
        return card.getName() + " (" + PaymentTransfer.getPaymentValue(card) + "M)";
    }

    private String paymentCardStyle(Card card) {
        if (card instanceof PropertyCard propertyCard && propertyCard.getColor() != null) {
            return "-fx-border-color: " + dialogs.cssColorFor(propertyCard.getColor()) + ";";
        }
        if (card instanceof model.card.MoneyCard) {
            return "-fx-border-color: #27ae60;";
        }
        return "-fx-border-color: #d64545;";
    }

    private void showStatus(String text, boolean error) {
        if (statusDisplay != null) {
            statusDisplay.show(text, error);
        } else if (statusFallback != null) {
            statusFallback.accept(text, error);
        }
    }
}
