package ui.render;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.player.Player;

/**
 * Renders the bank-total summary bar above the public property board.
 * <p>
 * Each player receives a pill showing name and bank value; the active seat
 * is highlighted for quick identification during play.
 */
public final class BankBarRenderer {
    /**
     * Renders bank total pills for each player in seat order.
     *
     * @param container    horizontal row that receives the pills
     * @param players      players to display; iteration order defines seat order
     * @param currentSeat  seat index of the active player, highlighted in the bar
     */
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

    /**
     * Renders bank total pills from pre-built board views.
     *
     * @param container    horizontal row that receives the pills
     * @param views        read-only player snapshots with bank totals
     * @param currentSeat  seat index of the active player, highlighted in the bar
     */
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
