package ui.render;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Renders stacked draw and discard pile widgets for the public board header.
 * <p>
 * Each pile shows a layered card silhouette, numeric count, and title label.
 */
public final class DeckPileRenderer {
    private DeckPileRenderer() {
    }

    /**
     * Renders draw and discard pile widgets with current counts.
     *
     * @param container     horizontal row that receives the pile widgets
     * @param drawCount     number of cards remaining in the draw pile
     * @param discardCount  number of cards in the discard pile
     * @param gameOver      when {@code true}, pile titles are shown in a muted style
     */
    public static void render(HBox container, int drawCount, int discardCount, boolean gameOver) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(createPile("Draw", drawCount, "deck-pile-draw", gameOver));
        container.getChildren().add(createPile("Discard", discardCount, "deck-pile-discard", gameOver));
    }

    private static VBox createPile(String title, int count, String pileStyleClass, boolean gameOver) {
        VBox pile = new VBox(5);
        pile.setAlignment(Pos.CENTER);
        pile.getStyleClass().add("deck-pile");

        StackPane stack = new StackPane();
        stack.setMinSize(58, 78);
        stack.setPrefSize(58, 78);
        stack.setMaxSize(58, 78);

        for (int i = 2; i >= 0; i--) {
            Rectangle card = new Rectangle(46, 64);
            card.getStyleClass().addAll("deck-pile-card", pileStyleClass);
            card.setArcWidth(8);
            card.setArcHeight(8);
            card.setTranslateX(i * 2.5);
            card.setTranslateY(-i * 2.5);
            stack.getChildren().add(card);
        }

        Label countLabel = new Label(String.valueOf(Math.max(0, count)));
        countLabel.getStyleClass().add("deck-pile-count");
        stack.getChildren().add(countLabel);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("deck-pile-title");
        if (gameOver) {
            titleLabel.getStyleClass().add("deck-pile-title-muted");
        }

        pile.getChildren().addAll(stack, titleLabel);
        return pile;
    }
}
