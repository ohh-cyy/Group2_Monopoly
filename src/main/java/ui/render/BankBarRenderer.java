package ui.render;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.player.Player;

/** Renders the bank total bar above the public property board. */
public final class BankBarRenderer {
    public void render(HBox container, Iterable<Player> players, int currentSeat) {
        if (container == null || players == null) {
            return;
        }
        container.getChildren().clear();
        int seat = 0;
        for (Player player : players) {
            container.getChildren().add(createBankPill(player.getName(), player.getBankTotalValue(), seat == currentSeat));
            seat++;
        }
    }

    public void renderBoardViews(HBox container, Iterable<PlayerBoardView> views, int currentSeat) {
        if (container == null || views == null) {
            return;
        }
        container.getChildren().clear();
        for (PlayerBoardView view : views) {
            container.getChildren().add(createBankPill(view.name, view.bankTotal, view.seat == currentSeat));
        }
    }

    private VBox createBankPill(String name, int total, boolean isCurrent) {
        VBox pill = new VBox(2);
        pill.getStyleClass().add("bank-pill");
        if (isCurrent) {
            pill.getStyleClass().add("bank-pill-current");
        }
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("bank-pill-label");
        Label valueLabel = new Label(total + "M");
        valueLabel.getStyleClass().add("bank-pill-value");
        pill.getChildren().addAll(nameLabel, valueLabel);
        return pill;
    }
}
