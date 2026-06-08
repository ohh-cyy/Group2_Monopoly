package controller.view;

import javafx.scene.layout.VBox;
import model.card.Card;
import ui.CardView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Refreshes shared JavaFX board widgets from current game state. */
public interface GameBoardRefreshService {
    void setSelectedCard(Card selectedCard);

    void applySelectionCallback(BiConsumer<Card, CardView> callback);

    void refreshAll(VBox playersList, Consumer<Double> rowHeightConsumer);

    void refreshButtons();

    default void disableActionButtons() {
        // Local games keep buttons managed by turn rules during refresh.
    }
}
