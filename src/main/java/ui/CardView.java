package ui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.card.*;
import model.card.actionCard.ActionCard;
import model.enums.Color;

/**
 * 统一卡牌 UI：优先显示关联图片，无图时回退为色块+文字。
 */
public class CardView extends StackPane {

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 168;

    private final Card card;
    private final boolean clickable;
    private boolean selected;

    public CardView(Card card, boolean clickable) {
        this.card = card;
        this.clickable = clickable;
        this.selected = false;
        initializeCard();
    }

    private void initializeCard() {
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setCursor(clickable ? Cursor.HAND : Cursor.DEFAULT);

        String imagePath = CardImageLoader.resolvePath(card);
        if (imagePath != null) {
            Image image = new Image(
                    getClass().getResourceAsStream(imagePath),
                    CARD_WIDTH,
                    CARD_HEIGHT,
                    true,
                    true
            );
            if (!image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(CARD_WIDTH);
                imageView.setFitHeight(CARD_HEIGHT);
                imageView.setPreserveRatio(false);
                getChildren().add(imageView);
                updateCardStyle();
                addHoverEffect();
                return;
            }
        }

        VBox content = createFallbackContent();
        updateCardStyle();
        getChildren().add(content);
        addHoverEffect();
    }

    private void addHoverEffect() {
        if (!clickable) {
            return;
        }
        setOnMouseEntered(e -> {
            if (!selected) {
                setStyle(getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;");
            }
        });
        setOnMouseExited(e -> {
            if (!selected) {
                setStyle(getStyle().replace("-fx-scale-x: 1.05; -fx-scale-y: 1.05;", ""));
            }
        });
    }

    private VBox createFallbackContent() {
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 8;");

        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(CARD_WIDTH - 20);
        nameLabel.setWrapText(true);

        Label specialInfoLabel = createSpecialInfoLabel();
        Label typeLabel = new Label(getTypeDisplayName());
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-opacity: 0.9;");

        if (specialInfoLabel != null) {
            content.getChildren().addAll(nameLabel, specialInfoLabel, typeLabel);
        } else {
            content.getChildren().addAll(nameLabel, typeLabel);
        }
        return content;
    }

    private Label createSpecialInfoLabel() {
        if (card instanceof MoneyCard moneyCard) {
            Label label = new Label(moneyCard.getMoney() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof PropertyCard propertyCard) {
            String rent = propertyCard.getRentDisplay();
            String text = propertyCard.getPrice() + "M" + (rent.isEmpty() ? "" : "\nRent " + rent);
            Label label = new Label(text);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            label.setWrapText(true);
            label.setMaxWidth(CARD_WIDTH - 20);
            return label;
        }
        if (card instanceof RentCard rentCard) {
            String colors = rentCard.isAllColors() ? "All" : "Dual color";
            Label label = new Label("Bank " + rentCard.getBankValueM() + "M\n" + colors);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
            label.setWrapText(true);
            return label;
        }
        if (card instanceof ActionCard actionCard) {
            Label label = new Label(actionCard.getBankValueM() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            return label;
        }
        if (card instanceof WildpropertyCard wild) {
            Label label = new Label(wild.isBankable() ? wild.getPrice() + "M" : "No bank");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            return label;
        }
        return null;
    }

    private String getTypeDisplayName() {
        return switch (card.getType()) {
            case PROPERTY -> "Property";
            case MONEY -> "Money";
            case ACTION -> "Action";
            default -> "Card";
        };
    }

    private void updateCardStyle() {
        String borderColor = selected ? "#f39c12" : "#2c3e50";
        int borderWidth = selected ? 3 : 2;
        String background = getChildren().stream().anyMatch(n -> n instanceof ImageView)
                ? "transparent"
                : getBackgroundColor();

        setStyle(
                "-fx-background-color: " + background + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: " + borderWidth + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
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
        updateCardStyle();
    }

    public boolean isSelected() {
        return selected;
    }

    public Card getCard() {
        return card;
    }
}
