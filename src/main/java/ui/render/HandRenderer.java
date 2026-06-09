package ui.render;

import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import model.card.Card;
import ui.CardView;

import java.util.List;
import java.util.function.BiConsumer;

/** Renders the current player's hand and wires selection / double-click play. */
public final class HandRenderer {
    private static final long DOUBLE_CLICK_WINDOW_MS = 450;

    private Card lastClickCard;
    private long lastClickAtMs;

    public interface SelectionListener {
        void onCardSelected(Card card, CardView cardView);

        /** Fired on double-click or when clicking an already-selected card. */
        void onCardPlayAttempt(Card card, CardView cardView);
    }

    public void render(HBox playerHand,
                       ScrollPane playerHandScroll,
                       List<Card> hand,
                       Card selectedCard,
                       boolean handInteractive,
                       boolean handSelectable,
                       SelectionListener listener) {
        if (playerHand == null) {
            return;
        }
        playerHand.getChildren().clear();
        if (hand == null || hand.isEmpty()) {
            return;
        }

        CardView.CardMetrics metrics = computeHandMetrics(playerHandScroll, hand.size());
        for (Card card : hand) {
            StackPane slot = CardView.wrapInSlot(card, handSelectable, metrics);
            CardView cardView = CardView.getCardView(slot);
            if (selectedCard != null && selectedCard.equals(card) && cardView != null) {
                cardView.setSelected(true);
            }
            if (handInteractive && cardView != null && listener != null) {
                slot.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                    if (event.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    boolean rapidSecondClick = card.equals(lastClickCard)
                            && now - lastClickAtMs <= DOUBLE_CLICK_WINDOW_MS;
                    boolean replaySelected = selectedCard != null && selectedCard.equals(card);

                    if (rapidSecondClick || replaySelected) {
                        lastClickCard = null;
                        lastClickAtMs = 0;
                        event.consume();
                        listener.onCardPlayAttempt(card, cardView);
                        return;
                    }

                    lastClickCard = card;
                    lastClickAtMs = now;
                    listener.onCardSelected(card, cardView);
                });
            }
            playerHand.getChildren().add(slot);
        }
    }

    public CardView.CardMetrics computeHandMetrics(ScrollPane playerHandScroll, int cardCount) {
        if (cardCount <= 0) {
            return CardView.HAND;
        }
        double available = 600;
        if (playerHandScroll != null && playerHandScroll.getViewportBounds().getWidth() > 0) {
            available = playerHandScroll.getViewportBounds().getWidth() - 40;
        }
        double gap = 10;
        double total = cardCount * CardView.HAND.slotW() + (cardCount - 1) * gap;
        if (total <= available) {
            return CardView.HAND;
        }
        double factor = Math.max(0.6, available / total);
        return CardView.HAND.scaled(factor);
    }

    public void clearSelection(HBox playerHand) {
        if (playerHand == null) {
            return;
        }
        for (javafx.scene.Node node : playerHand.getChildren()) {
            CardView cardView = node instanceof StackPane stack ? CardView.getCardView(stack) : null;
            if (cardView != null) {
                cardView.setSelected(false);
            }
        }
    }

    public void applySelection(HBox playerHand, Card selectedCard, BiConsumer<Card, CardView> onFound) {
        if (playerHand == null || selectedCard == null) {
            return;
        }
        for (javafx.scene.Node node : playerHand.getChildren()) {
            if (!(node instanceof StackPane stack)) {
                continue;
            }
            CardView cardView = CardView.getCardView(stack);
            if (cardView != null && selectedCard.equals(cardView.getCard())) {
                cardView.setSelected(true);
                if (onFound != null) {
                    onFound.accept(selectedCard, cardView);
                }
                return;
            }
        }
    }
}
