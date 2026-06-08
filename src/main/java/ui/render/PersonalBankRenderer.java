package ui.render;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import model.card.Card;
import ui.CardView;

import java.util.List;

/** Renders the current player's bank row and total value label. */
public final class PersonalBankRenderer {
    private static final String EMPTY_HINT =
            "(Money cards / action cards played, or rent collected, go into bank)";

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
