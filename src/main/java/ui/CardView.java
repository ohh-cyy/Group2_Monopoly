package ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.card.*;
import model.card.actionCard.ActionCard;
import model.enums.Color;
import javafx.util.Duration;

import java.util.stream.Collectors;

/**
 * Shared card UI. Cards stay small by default and enlarge on hover.
 */
public class CardView extends StackPane {

    public static final CardMetrics HAND = new CardMetrics(82, 116, 126, 176);
    public static final CardMetrics PUBLIC = new CardMetrics(82, 116, 132, 184);
    public static final CardMetrics COMPACT = new CardMetrics(56, 78, 92, 128);

    private final Card card;
    private final boolean clickable;
    private final CardMetrics metrics;
    private boolean selected;
    private boolean hovered;
    private boolean activating;
    private Timeline visualTransition;

    public CardView(Card card, boolean clickable) {
        this(card, clickable, HAND);
    }

    public CardView(Card card, boolean clickable, CardMetrics metrics) {
        this.card = card;
        this.clickable = clickable;
        this.metrics = metrics;
        this.selected = false;
        this.hovered = false;
        initializeCard();
    }

    public static StackPane wrapInSlot(Card card, boolean clickable) {
        return wrapInSlot(card, clickable, HAND);
    }

    public static StackPane wrapInSlot(Card card, boolean clickable, CardMetrics metrics) {
        StackPane slot = new StackPane();
        slot.setMinSize(metrics.slotW(), metrics.slotH());
        slot.setPrefSize(metrics.slotW(), metrics.slotH());
        slot.setMaxSize(metrics.slotW(), metrics.slotH());
        slot.setAlignment(Pos.CENTER);
        slot.setPickOnBounds(true);

        CardView cardView = new CardView(card, clickable, metrics);
        slot.getChildren().add(cardView);

        slot.setOnMouseEntered(e -> cardView.setHovered(true));
        slot.setOnMouseExited(e -> cardView.setHovered(false));
        if (clickable) {
            slot.setCursor(Cursor.HAND);
        }
        return slot;
    }

    public static CardView getCardView(StackPane slot) {
        if (slot == null || slot.getChildren().isEmpty()) {
            return null;
        }
        return (CardView) slot.getChildren().get(0);
    }

    private void initializeCard() {
        setPrefSize(metrics.normalW(), metrics.normalH());
        setMinSize(metrics.normalW(), metrics.normalH());
        setMaxSize(metrics.normalW(), metrics.normalH());
        setCursor(clickable ? Cursor.HAND : Cursor.DEFAULT);
        Tooltip.install(this, createInfoTooltip());

        String imagePath = CardImageLoader.resolvePath(card);
        if (imagePath != null) {
            Image image = new Image(
                    getClass().getResourceAsStream(imagePath),
                    metrics.hoverW(),
                    metrics.hoverH(),
                    true,
                    true
            );
            if (!image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(metrics.normalW());
                imageView.setFitHeight(metrics.normalH());
                imageView.setPreserveRatio(false);
                imageView.setRotate(CardImageLoader.resolveRotationDegrees(card));
                getChildren().add(imageView);
                applyVisualState();
                addHoverEffect();
                return;
            }
        }

        VBox content = createFallbackContent();
        getChildren().add(content);
        applyVisualState();
        addHoverEffect();
    }

    private void addHoverEffect() {
        setOnMouseEntered(e -> setHovered(true));
        setOnMouseExited(e -> setHovered(false));
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
        if (activating) {
            return;
        }
        animateVisualState();
    }

    private void applyVisualState() {
        boolean enlarged = selected || hovered;
        double scaleX = enlarged ? metrics.hoverW() / metrics.normalW() : 1.0;
        double scaleY = enlarged ? metrics.hoverH() / metrics.normalH() : 1.0;
        setScaleX(scaleX);
        setScaleY(scaleY);
        setTranslateY(enlarged ? -6 : 0);

        if (enlarged) {
            setEffect(new DropShadow(10, 0, 3, javafx.scene.paint.Color.rgb(0, 0, 0, 0.45)));
        } else {
            setEffect(new DropShadow(3, 0, 1, javafx.scene.paint.Color.rgb(0, 0, 0, 0.22)));
        }

        updateCardStyle();
    }

    private void animateVisualState() {
        boolean enlarged = selected || hovered;
        double targetScaleX = enlarged ? metrics.hoverW() / metrics.normalW() : 1.0;
        double targetScaleY = enlarged ? metrics.hoverH() / metrics.normalH() : 1.0;

        if (visualTransition != null) {
            visualTransition.stop();
        }
        visualTransition = new Timeline(
                new KeyFrame(
                        Duration.millis(170),
                        new KeyValue(scaleXProperty(), targetScaleX, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), targetScaleY, Interpolator.EASE_BOTH),
                        new KeyValue(translateYProperty(), enlarged ? -6 : 0, Interpolator.EASE_BOTH)
                )
        );
        visualTransition.play();

        setEffect(enlarged
                ? new DropShadow(12, 0, 4, javafx.scene.paint.Color.rgb(0, 0, 0, 0.46))
                : new DropShadow(3, 0, 1, javafx.scene.paint.Color.rgb(0, 0, 0, 0.22)));
        updateCardStyle();
    }

    public void playActivationAnimation(Runnable onFinished) {
        if (activating) {
            return;
        }
        activating = true;
        if (visualTransition != null) {
            visualTransition.stop();
        }

        double selectedScaleX = metrics.hoverW() / metrics.normalW();
        double selectedScaleY = metrics.hoverH() / metrics.normalH();
        visualTransition = new Timeline(
                new KeyFrame(
                        Duration.millis(85),
                        new KeyValue(scaleXProperty(), selectedScaleX * 0.94, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), selectedScaleY * 0.94, Interpolator.EASE_BOTH),
                        new KeyValue(translateYProperty(), -2, Interpolator.EASE_BOTH),
                        new KeyValue(rotateProperty(), -1.2, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(
                        Duration.millis(180),
                        new KeyValue(scaleXProperty(), selectedScaleX, Interpolator.EASE_OUT),
                        new KeyValue(scaleYProperty(), selectedScaleY, Interpolator.EASE_OUT),
                        new KeyValue(translateYProperty(), -8, Interpolator.EASE_OUT),
                        new KeyValue(rotateProperty(), 0, Interpolator.EASE_OUT)
                )
        );
        visualTransition.setOnFinished(event -> {
            activating = false;
            if (onFinished != null) {
                onFinished.run();
            }
        });
        visualTransition.play();
    }

    private VBox createFallbackContent() {
        VBox content = new VBox(3);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 3;");

        double fontScale = metrics.normalW() / HAND.normalW();
        int nameSize = Math.max(7, (int) (8 * fontScale));
        int subSize = Math.max(6, (int) (7 * fontScale));

        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + nameSize + "px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(metrics.normalW() - 10);
        nameLabel.setWrapText(true);

        Label specialInfoLabel = createSpecialInfoLabel(subSize);
        Label typeLabel = new Label(getTypeDisplayName());
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: " + subSize + "px; -fx-opacity: 0.9;");

        if (specialInfoLabel != null) {
            content.getChildren().addAll(nameLabel, specialInfoLabel, typeLabel);
        } else {
            content.getChildren().addAll(nameLabel, typeLabel);
        }
        return content;
    }

    private Label createSpecialInfoLabel(int subSize) {
        if (card instanceof MoneyCard moneyCard) {
            Label label = new Label(moneyCard.getMoney() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: " + Math.max(9, subSize + 2) + "px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof WildpropertyCard wild) {
            Label label = new Label(wild.isBankable() ? wild.getBankValueM() + "M" : "—");
            label.setStyle("-fx-text-fill: white; -fx-font-size: " + subSize + "px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof PropertyCard propertyCard) {
            Label label = new Label(propertyCard.getPrice() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: " + (subSize + 1) + "px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof RentCard rentCard) {
            Label label = new Label(rentCard.getBankValueM() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: " + subSize + "px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof ActionCard actionCard) {
            Label label = new Label(actionCard.getBankValueM() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: " + (subSize + 2) + "px; -fx-font-weight: bold;");
            return label;
        }
        return null;
    }

    private String getTypeDisplayName() {
        if (card instanceof WildpropertyCard) {
            return "Wild Property";
        }
        if (card instanceof RentCard) {
            return "Rent / Action";
        }
        return switch (card.getType()) {
            case PROPERTY -> "Property";
            case MONEY -> "Money";
            case ACTION -> "Action";
            default -> "Card";
        };
    }

    private Tooltip createInfoTooltip() {
        Tooltip tooltip = new Tooltip(buildInfoText());
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(285);
        tooltip.setShowDelay(Duration.millis(80));
        tooltip.setHideDelay(Duration.millis(80));
        tooltip.setShowDuration(Duration.seconds(20));
        tooltip.setStyle(
                "-fx-background-color: rgba(8, 42, 29, 0.96);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 700;" +
                "-fx-padding: 10 12;" +
                "-fx-background-radius: 10;"
        );
        return tooltip;
    }

    private String buildInfoText() {
        return "Name: " + card.getName()
                + "\nType: " + getTypeDisplayName()
                + "\nEffect: " + buildEffectText();
    }

    private String buildEffectText() {
        String description = card.getDescription() == null || card.getDescription().isBlank()
                ? "No description"
                : card.getDescription();

        if (card instanceof WildpropertyCard wild) {
            String colors = wild.getAvailableColors().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(" / "));
            String bankText = wild.isBankable() ? "; can bank for " + wild.getBankValueM() + "M" : "; cannot bank";
            return description + " — choose " + colors + " as its color" + bankText + ".";
        }
        if (card instanceof PropertyCard propertyCard) {
            return description + " — place into property area; color " + propertyCard.getColor()
                    + "; value " + propertyCard.getPrice() + "M; rent " + propertyCard.getRentDisplay() + ".";
        }
        if (card instanceof MoneyCard moneyCard) {
            return description + " — deposit into bank; value " + moneyCard.getMoney() + "M.";
        }
        if (card instanceof RentCard rentCard) {
            return description + " — bank value " + rentCard.getBankValueM() + "M.";
        }
        if (card instanceof ActionCard actionCard) {
            return description + " — bank value " + actionCard.getBankValueM() + "M.";
        }
        return description;
    }

    private void updateCardStyle() {
        String borderColor = selected ? "#f39c12" : "#ffffff";
        int borderWidth = selected ? 3 : 2;
        String background = getChildren().stream().anyMatch(n -> n instanceof ImageView)
                ? "transparent"
                : getBackgroundColor();

        setStyle(
                "-fx-background-color: " + background + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: " + borderWidth + ";" +
                "-fx-background-radius: 5;" +
                "-fx-border-radius: 5;"
        );
    }

    private String getBackgroundColor() {
        return switch (card.getType()) {
            case PROPERTY -> {
                if (card instanceof PropertyCard pc && pc.getColor() != null) {
                    yield getPropertyColorHex(pc.getColor());
                }
                yield "#95a5a6";
            }
            case MONEY -> "#27ae60";
            case ACTION -> "#e74c3c";
            default -> "#95a5a6";
        };
    }

    private String getPropertyColorHex(Color color) {
        if (color == null) {
            return "#95a5a6";
        }
        return switch (color) {
            case BROWN -> "#8B4513";
            case LIGHT_BLUE -> "#87CEEB";
            case PINK -> "#FF69B4";
            case ORANGE -> "#FFA500";
            case RED -> "#DC143C";
            case YELLOW -> "#FFD700";
            case GREEN -> "#228B22";
            case DARK_BLUE -> "#00008B";
            case BLACK -> "#2c3e50";
            case LIGHT_GREEN -> "#90EE90";
            default -> "#95a5a6";
        };
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (activating) {
            updateCardStyle();
            return;
        }
        animateVisualState();
    }

    public boolean isSelected() {
        return selected;
    }

    public Card getCard() {
        return card;
    }

    /** Card size settings for hand, bank, and public property areas. */
    public record CardMetrics(double normalW, double normalH, double slotW, double slotH) {
        public CardMetrics(double normalW, double normalH, double slotW, double slotH) {
            this.normalW = normalW;
            this.normalH = normalH;
            this.slotW = slotW;
            this.slotH = slotH;
        }

        public double hoverW() {
            return slotW;
        }

        public double hoverH() {
            return slotH;
        }

        public CardMetrics scaled(double factor) {
            if (factor >= 0.999) {
                return this;
            }
            return new CardMetrics(
                    normalW * factor,
                    normalH * factor,
                    slotW * factor,
                    slotH * factor
            );
        }
    }
}
