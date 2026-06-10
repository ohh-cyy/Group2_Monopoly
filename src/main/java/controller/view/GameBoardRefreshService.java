package controller.view;

import javafx.scene.layout.VBox;
import model.card.Card;
import ui.CardView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Abstraction for synchronizing shared board widgets with game state.
 * <p>
 * Implemented by {@link LocalBoardRefreshService} and {@link NetworkBoardRefreshService}
 * so controllers can refresh the UI without mode-specific branching.
 */
public interface GameBoardRefreshService {
    /** Remembers the highlighted hand card for renderers and button enablement. */
    void setSelectedCard(Card selectedCard);

    /** Registers a callback to capture the {@link CardView} when selection is reapplied. */
    void applySelectionCallback(BiConsumer<Card, CardView> callback);

    /**
     * Full board repaint: player list, properties, hand, bank, labels, and buttons.
     *
     * @param rowHeightConsumer optional callback with the tallest property row height
     */
    void refreshAll(VBox playersList, Consumer<Double> rowHeightConsumer);

    /** Recomputes draw/discard/end-turn button disabled state only. */
    void refreshButtons();

    /** Disables all turn action buttons (e.g. after game over). */
    default void disableActionButtons() {
        // Local games keep buttons managed by turn rules during refresh.
    }
}
