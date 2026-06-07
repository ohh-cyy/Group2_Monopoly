package controller.dialog;

import model.card.Card;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Prepares the shared hand-limit discard prompt for local and network games.
 */
public final class HandDiscardDialogService {
    private HandDiscardDialogService() {
    }

    public static Optional<Card> promptDiscardOne(
            Function<DiscardPrompt, Optional<Card>> promptHandler,
            List<Card> hand,
            int excess,
            boolean endingTurn) {
        Objects.requireNonNull(promptHandler, "promptHandler");
        if (hand == null || hand.isEmpty() || excess <= 0) {
            return Optional.empty();
        }

        List<Card> availableCards = List.copyOf(hand);
        String suffix = endingTurn
                ? " before your turn can end"
                : "";
        DiscardPrompt prompt = new DiscardPrompt(
                "Hand Limit",
                "You have too many cards in hand",
                "Choose a card to discard (" + excess + " more required" + suffix + "):",
                availableCards);

        Optional<Card> selected = promptHandler.apply(prompt);
        if (selected == null || selected.isEmpty() || !availableCards.contains(selected.get())) {
            return Optional.empty();
        }
        return selected;
    }

    public record DiscardPrompt(String title, String header, String prompt, List<Card> hand) {
        public DiscardPrompt {
            hand = List.copyOf(hand);
        }
    }
}
