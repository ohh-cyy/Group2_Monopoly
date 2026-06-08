package ui.render;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.card.Card;
import model.enums.Color;
import ui.CardView;
import ui.PublicPropertyBoardLayout;
import ui.PublicPropertySetView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Renders all players' public property areas on the main board. */
public final class PublicBoardRenderer {
    private final Supplier<Image> avatarSupplier;

    public PublicBoardRenderer(Supplier<Image> avatarSupplier) {
        this.avatarSupplier = avatarSupplier;
    }

    public double render(VBox panel, VBox propertiesPanel, List<PlayerBoardView> views, int currentSeat) {
        if (propertiesPanel == null) {
            return -1;
        }
        propertiesPanel.getChildren().clear();
        if (views == null || views.isEmpty()) {
            Label empty = new Label("(No player properties yet)");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            propertiesPanel.getChildren().add(empty);
            return -1;
        }

        double rowHeight = PublicPropertyBoardLayout.rowHeightFor(propertiesPanel, views.size());
        CardView.CardMetrics propertyMetrics = PublicPropertyBoardLayout.cardMetricsForRow(rowHeight);
        double avatarSize = Math.min(38, Math.max(24, rowHeight - 28));
        double rowWidth = resolvePublicBoardRowWidth(panel, propertiesPanel);

        for (PlayerBoardView view : views) {
            boolean isTurn = view.seat == currentSeat;
            VBox playerBlock = new VBox(6);
            playerBlock.setMaxWidth(Double.MAX_VALUE);
            playerBlock.setMinHeight(0);
            playerBlock.setMaxHeight(Double.MAX_VALUE);
            playerBlock.getStyleClass().add("player-public-block");
            if (isTurn) {
                playerBlock.getStyleClass().add("player-public-block-current");
            }

            HBox titleRow = new HBox(9);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label((isTurn ? "▶ " : "") + view.name
                    + "  |  Hand: " + view.handSize + " cards  |  Bank: " + view.bankTotal + "M");
            title.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #103c2a;");
            titleRow.getChildren().addAll(createAvatarView(avatarSize), title);

            FlowPane props = new FlowPane(10, 10);
            props.setPrefWrapLength(Math.max(320, rowWidth - 40));
            props.setMaxWidth(Double.MAX_VALUE);
            props.setMaxHeight(propertyMetrics.slotH() + 56);
            if (view.properties.isEmpty()) {
                props.getChildren().add(new Label("(No properties)"));
            } else {
                for (Map.Entry<Color, List<Card>> entry : groupPropertiesByColor(view.properties).entrySet()) {
                    props.getChildren().add(PublicPropertySetView.build(entry.getKey(), entry.getValue(), propertyMetrics));
                }
            }

            ScrollPane propsScroll = new ScrollPane(props);
            propsScroll.setFitToHeight(true);
            propsScroll.setMinHeight(0);
            propsScroll.setMaxHeight(Double.MAX_VALUE);
            propsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            propsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            propsScroll.setPannable(true);
            propsScroll.getStyleClass().add("transparent-scroll");
            VBox.setVgrow(propsScroll, Priority.ALWAYS);

            playerBlock.getChildren().addAll(titleRow, propsScroll);
            propertiesPanel.getChildren().add(playerBlock);
        }
        PublicPropertyBoardLayout.applyEqualRows(propertiesPanel, views.size());
        return rowHeight;
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

    private double resolvePublicBoardRowWidth(VBox panel, VBox propertiesPanel) {
        if (propertiesPanel != null && propertiesPanel.getWidth() > 0) {
            return propertiesPanel.getWidth();
        }
        if (panel != null && panel.getWidth() > 0) {
            return panel.getWidth();
        }
        return 640;
    }

    private Map<Color, List<Card>> groupPropertiesByColor(List<Card> properties) {
        Map<Color, List<Card>> byColor = new LinkedHashMap<>();
        for (Card card : properties) {
            Color color = card.getColor() != null ? card.getColor() : Color.BROWN;
            byColor.computeIfAbsent(color, ignored -> new ArrayList<>()).add(card);
        }
        return byColor;
    }
}
