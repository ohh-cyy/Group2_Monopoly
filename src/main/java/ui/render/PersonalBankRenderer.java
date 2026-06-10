package ui.render;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import model.card.Card;
import ui.CardView;

import java.util.List;

/**
 * Renders the current player's personal bank row and total value label.
 * <p>
 * Bank cards use {@link CardView#COMPACT} metrics; an empty bank shows a hint label.
 */
public final class PersonalBankRenderer {
    private static final String EMPTY_HINT =
            "(Money cards / action cards played, or rent collected, go into bank)";

    /**
     * Renders the personal bank card row and updates the total label.
     *
     * @param playerBank      flow pane that receives compact bank card slots
     * @param bankTotalLabel  label updated with the total bank value
     * @param totalM          total bank value in millions
     * @param bankCards       cards currently in the player's bank
     */
    public void render(FlowPane playerBank, Label bankTotalLabel, int totalM, List<Card> bankCards) {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();
        if (bankTotalLabel != null) {
            bankTotalLabel.setText(totalM + "M");
        }
        if (bankCards == null || bankCards.isEmpty()) {
            Label hint = new Label(EMPTY_HINT);
            hint.setStyle("-fx-text-fill: #476272; -fx-font-size: 12px; -fx-wrap-text: true;");
            playerBank.getChildren().add(hint);
            return;
        }
        for (Card card : bankCards) {
            StackPane slot = CardView.wrapInSlot(card, false, CardView.COMPACT);
            slot.getStyleClass().add("bank-card-slot");
            playerBank.getChildren().add(slot);
        }
    }
}
