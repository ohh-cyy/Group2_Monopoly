package ui.render;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.player.Player;

import java.util.function.Supplier;

/**
 * Renders left-sidebar player summary cards with avatar, hand size, and bank total.
 * <p>
 * Supports both domain {@link model.player.Player} objects and pre-built
 * {@link PlayerBoardView} snapshots for online play.
 */
public final class PlayerListRenderer {
    private final Supplier<Image> avatarSupplier;

    /**
     * Creates a renderer that supplies avatar images for sidebar player cards.
     *
     * @param avatarSupplier supplier of the default avatar image; may be {@code null}
     */
    public PlayerListRenderer(Supplier<Image> avatarSupplier) {
        this.avatarSupplier = avatarSupplier;
    }

    /** Renders sidebar summary cards for domain players. */
    public void render(VBox container, Iterable<Player> players, Player currentPlayer) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        if (players == null) {
            return;
        }
        for (Player player : players) {
            boolean current = currentPlayer != null && player.equals(currentPlayer);
            container.getChildren().add(createPlayerInfoBox(
                    player.getName(),
                    player.getHand().size(),
                    player.getBankTotalValue(),
                    current));
        }
    }

    /** Renders sidebar summary cards from pre-built board views. */
    public void renderBoardViews(VBox container, Iterable<PlayerBoardView> views, int currentSeat) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        if (views == null) {
            return;
        }
        for (PlayerBoardView view : views) {
            container.getChildren().add(createPlayerInfoBox(
                    view.name,
                    view.handSize,
                    view.bankTotal,
                    view.seat == currentSeat));
        }
    }

    private VBox createPlayerInfoBox(String name, int handSize, int bankTotal, boolean isCurrent) {
        VBox box = new VBox(7);
        box.getStyleClass().add("player-info-card");
        if (isCurrent) {
            box.getStyleClass().add("player-info-current");
        }

        HBox header = new HBox(9);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(createAvatarView(42), new Label(name) {{
            setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        }});

        box.getChildren().addAll(
                header,
                new Label("Hand: " + handSize + " cards"),
                new Label("Bank: " + bankTotal + "M")
        );
        return box;
    }

    private ImageView createAvatarView(double size) {
        ImageView view = new ImageView();
        Image avatar = avatarSupplier != null ? avatarSupplier.get() : null;
        if (avatar != null) {
            view.setImage(avatar);
        }
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        view.getStyleClass().add("player-avatar");
        return view;
    }
}
