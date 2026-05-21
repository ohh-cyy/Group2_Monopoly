package ui;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.card.*;
import model.enums.Color;

/**
 * Unified Card UI Component
 * Automatically adjusts display style based on card type and color
 */
public class CardView extends StackPane {
    
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 140;
    
    private Card card;
    private boolean clickable;
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
        
        // Create card content
        VBox content = createCardContent();
        
        // Set card style
        updateCardStyle();
        
        getChildren().add(content);
        
        // Add hover effect
        if (clickable) {
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
    }
    
    private VBox createCardContent() {
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 8;");
        
        // Card name
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-wrap-text: true; -fx-text-alignment: center;");
        nameLabel.setMaxWidth(CARD_WIDTH - 20);
        nameLabel.setWrapText(true);
        
        // Special info (e.g., money value, property price)
        Label specialInfoLabel = createSpecialInfoLabel();
        
        // Card description (if available)
        Label descLabel = null;
        if (card.getDescription() != null && !card.getDescription().isEmpty()) {
            descLabel = new Label(card.getDescription());
            descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 8px; -fx-wrap-text: true; -fx-text-alignment: center;");
            descLabel.setMaxWidth(CARD_WIDTH - 20);
            descLabel.setWrapText(true);
        }
        
        // Card type label
        Label typeLabel = new Label(getTypeDisplayName());
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-opacity: 0.9;");
        
        // Determine layout based on whether special info exists
        if (specialInfoLabel != null) {
            content.getChildren().addAll(nameLabel, specialInfoLabel, typeLabel);
        } else if (descLabel != null) {
            content.getChildren().addAll(nameLabel, descLabel, typeLabel);
        } else {
            content.getChildren().addAll(nameLabel, typeLabel);
        }
        
        return content;
    }
    
    private Label createSpecialInfoLabel() {
        if (card instanceof MoneyCard) {
            MoneyCard moneyCard = (MoneyCard) card;
            Label label = new Label(moneyCard.getMoney() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            return label;
        } else if (card instanceof PropertyCard) {
            PropertyCard propertyCard = (PropertyCard) card;
            Label label = new Label(propertyCard.getPrice() + "M");
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            return label;
        }
        return null;
    }
    
    private String getTypeDisplayName() {
        switch (card.getType()) {
            case PROPERTY:
                return "Property";
            case MONEY:
                return "Money";
            case ACTION:
                return "Action";
            default:
                return "Unknown";
        }
    }
    
    private void updateCardStyle() {
        String backgroundColor = getBackgroundColor();
        String borderColor = selected ? "#f39c12" : "#2c3e50";
        int borderWidth = selected ? 3 : 2;
        
        setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: " + borderWidth + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
    }
    
    private String getBackgroundColor() {
        switch (card.getType()) {
            case PROPERTY:
                if (card instanceof PropertyCard) {
                    Color color = ((PropertyCard) card).getColor();
                    return getPropertyColorHex(color);
                }
                return "#95a5a6";
            case MONEY:
                return "#27ae60"; // Green
            case ACTION:
                return "#e74c3c"; // Red
            default:
                return "#95a5a6";
        }
    }
    
    private String getPropertyColorHex(Color color) {
        if (color == null) return "#95a5a6";
        
        switch (color) {
            case BROWN:
                return "#8B4513"; // Brown
            case LIGHT_BLUE:
                return "#87CEEB"; // Light Blue
            case PINK:
                return "#FF69B4"; // Pink
            case ORANGE:
                return "#FFA500"; // Orange
            case RED:
                return "#DC143C"; // Red
            case YELLOW:
                return "#FFD700"; // Yellow
            case GREEN:
                return "#228B22"; // Green
            case DARK_BLUE:
                return "#00008B"; // Dark Blue
            case BLACK:
                return "#2c3e50"; // Black
            default:
                return "#95a5a6";
        }
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
