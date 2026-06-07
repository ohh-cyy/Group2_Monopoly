package ui;

import engine.PropertyRules;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import model.card.Card;
import model.enums.Color;

import java.util.List;

/** Compact vertical property-set tile for the public board. */
public final class PublicPropertySetView {
    private static final double BOX_PADDING = 6;

    private PublicPropertySetView() {
    }

    public static VBox build(Color color, List<Card> cards, CardView.CardMetrics metrics) {
        CardView.CardMetrics setMetrics = compactMetrics(metrics);
        int billableCount = PropertyRules.countBillableProperties(cards);
        boolean complete = billableCount >= color.getSetSize();
        String borderColor = PropertyColorStyles.borderHex(color);
        String backgroundColor = PropertyColorStyles.backgroundHex(color, complete);

        VBox box = new VBox(3);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().add("property-set");
        if (complete) {
            box.getStyleClass().add("property-set-complete");
        }
        double borderWidth = complete ? 3.5 : 2.5;
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; "
                        + "-fx-border-radius: 10; -fx-background-radius: 10;",
                backgroundColor, borderColor, borderWidth));

        Label title = new Label(PropertyColorStyles.setTitleText(color, billableCount, color.getSetSize()));
        title.getStyleClass().add("property-set-title");
        title.setStyle("-fx-text-fill: " + PropertyColorStyles.titleTextHex(color) + ";");

        Pane cardRow = createOverlappedRow(cards, setMetrics);
        double titleWidth = measureTitleWidth(title.getText());
        double contentWidth = Math.max(cardRow.getPrefWidth(), titleWidth);
        double boxWidth = contentWidth + BOX_PADDING * 2 + PropertyColorStyles.minBoxWidthBonus(color);
        boxWidth = Math.max(boxWidth, setMetrics.slotW() + BOX_PADDING * 2);
        box.setMinWidth(boxWidth);
        box.setPrefWidth(boxWidth);
        box.setMaxWidth(boxWidth);
        title.setMaxWidth(boxWidth - BOX_PADDING * 2);

        int rent = PropertyRules.calculateRent(color, cards);
        Label rentLabel = new Label("Rent: " + rent + "M");
        rentLabel.getStyleClass().add("property-set-rent");

        box.getChildren().addAll(title, cardRow, rentLabel);
        return box;
    }

    private static CardView.CardMetrics compactMetrics(CardView.CardMetrics metrics) {
        double factor = Math.min(0.82, metrics.slotW() / CardView.PUBLIC.slotW());
        return metrics.scaled(Math.max(0.32, factor));
    }

    private static Pane createOverlappedRow(List<Card> cards, CardView.CardMetrics metrics) {
        Pane row = new Pane();
        row.setMinHeight(metrics.slotH() + 6);
        row.setPrefHeight(metrics.slotH() + 6);
        row.setMaxHeight(metrics.slotH() + 6);

        if (cards.isEmpty()) {
            row.setMinWidth(metrics.slotW());
            row.setPrefWidth(metrics.slotW());
            row.setMaxWidth(metrics.slotW());
            return row;
        }

        double scale = metrics.slotH() / CardView.PUBLIC.slotH();
        double fanDrop = 4 * scale;
        double offset = cards.size() <= 1 ? 0 : Math.min(16 * scale, metrics.slotW() * 0.34);
        double width = metrics.slotW() + Math.max(0, cards.size() - 1) * offset;
        row.setMinWidth(width);
        row.setPrefWidth(width);
        row.setMaxWidth(width);

        double middle = (cards.size() - 1) / 2.0;
        for (int i = 0; i < cards.size(); i++) {
            StackPane slot = CardView.wrapInSlot(cards.get(i), false, metrics);
            slot.setLayoutX(i * offset);
            slot.setLayoutY(i % 2 == 0 ? 0 : fanDrop);
            slot.setRotate((i - middle) * 2.5);
            slot.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> slot.toFront());
            row.getChildren().add(slot);
        }
        return row;
    }

    private static double measureTitleWidth(String titleText) {
        Text measure = new Text(titleText);
        measure.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        return Math.ceil(measure.getLayoutBounds().getWidth()) + 6;
    }
}
