package controller.dialog;

import model.card.Card;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shared hand-limit discard prompt used by local and network controllers.
 * <p>
 * Builds consistent title/header text and delegates UI rendering to a caller-supplied
 * {@link Function} so each mode can use its own {@link controller.dialog.GameDialogService}.
 */
public final class HandDiscardDialogService {
    /** Utility class; do not instantiate. */
    private HandDiscardDialogService() {
    }

    /**
     * Asks the player to discard one card when over the hand-size limit.
     *
     * @param promptHandler renders the dialog and returns the selected card
     * @param hand          current hand (copied defensively)
     * @param excess        number of discards still required after this pick
     * @param endingTurn    whether the prompt copy mentions ending the turn
     * @return the validated selection, or empty if cancelled or invalid
     */
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

    /**
     * Immutable bundle passed to the UI layer for a single discard choice.
     *
     * @param title  dialog window title
     * @param header bold summary line
     * @param prompt instructional body text
     * @param hand   selectable cards (defensively copied)
     */
    public record DiscardPrompt(String title, String header, String prompt, List<Card> hand) {
        public DiscardPrompt {
            hand = List.copyOf(hand);
        }
    }
}
