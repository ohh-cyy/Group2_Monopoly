package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.card.*;
import model.card.actionCard.SimpleActionCard;
import model.enums.Color;

/**
 * 卡牌UI展示测试类
 * 用于展示所有类型的卡牌效果
 */
public class CardViewTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        // 标题
        Label titleLabel = new Label("Monopoly Deal - 卡牌UI展示");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // 地产卡展示
        VBox propertySection = createSection("Property Cards");
        FlowPane propertyCards = new FlowPane(10, 10);
        propertyCards.setPrefWrapLength(800);
        
        // 添加各种颜色的地产卡
        propertyCards.getChildren().add(new CardView(new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("The Angel Islington", "Light Blue property", Color.LIGHT_BLUE, 1), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Bow Street", "Orange property", Color.ORANGE, 2), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Strand", "Red property", Color.RED, 3), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Leicester Square", "Yellow property", Color.YELLOW, 3), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Regent Street", "Green property", Color.GREEN, 4), true));
        propertyCards.getChildren().add(new CardView(new PropertyCard("Park Lane", "Dark Blue property", Color.DARK_BLUE, 4), true));
        
        propertySection.getChildren().add(propertyCards);
        
        // 金钱卡展示
        VBox moneySection = createSection("Money Cards");
        FlowPane moneyCards = new FlowPane(10, 10);
        moneyCards.setPrefWrapLength(800);
        
        moneyCards.getChildren().add(new CardView(new MoneyCard("1M Banknote", "Worth 1 million", 1), true));
        moneyCards.getChildren().add(new CardView(new MoneyCard("2M Banknote", "Worth 2 million", 2), true));
        moneyCards.getChildren().add(new CardView(new MoneyCard("5M Banknote", "Worth 5 million", 5), true));
        
        moneySection.getChildren().add(moneyCards);
        
        // 行动卡展示
        VBox actionSection = createSection("Action Cards");
        FlowPane actionCards = new FlowPane(10, 10);
        actionCards.setPrefWrapLength(800);
        
        // 使用 SimpleActionCard 创建行动卡牌实例
        actionCards.getChildren().add(new CardView(new SimpleActionCard("Pass Go", "Draw 2 extra cards", 1), true));
        actionCards.getChildren().add(new CardView(new SimpleActionCard("Deal Breaker", "Steal a complete property set", 5), true));
        actionCards.getChildren().add(new CardView(new SimpleActionCard("Just Say No", "Cancel any action card", 4), true));
        
        actionSection.getChildren().add(actionCards);
        
        root.getChildren().addAll(titleLabel, propertySection, moneySection, actionSection);
        
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("Card View Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private VBox createSection(String title) {
        VBox section = new VBox(10);
        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        section.getChildren().add(sectionTitle);
        return section;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
