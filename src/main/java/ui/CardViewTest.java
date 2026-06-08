package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.DealBreaker;
import model.card.actionCard.DebtCollector;
import model.card.actionCard.DoubleTheRent;
import model.card.actionCard.ForcedDeal;
import model.card.actionCard.House;
import model.card.actionCard.Hotel;
import model.card.actionCard.JustSayNo;
import model.card.actionCard.MyBirthday;
import model.card.actionCard.PassGoCard;
import model.card.actionCard.SlyDeal;
import model.enums.CardType;
import model.enums.Color;

import java.util.List;

/**
 * Small JavaFX window for checking card UI rendering.
 */
public class CardViewTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Monopoly Deal - Card UI Preview");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox propertySection = createSection("Property Cards");
        FlowPane propertyCards = new FlowPane(10, 10);
        propertyCards.setPrefWrapLength(1000);
        propertyCards.getChildren().addAll(
                card(new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1)),
                card(new PropertyCard("The Angel Islington", "Light Blue property", Color.LIGHT_BLUE, 1)),
                card(new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2)),
                card(new PropertyCard("Bow Street", "Orange property", Color.ORANGE, 2)),
                card(new PropertyCard("Strand", "Red property", Color.RED, 3)),
                card(new PropertyCard("Leicester Square", "Yellow property", Color.YELLOW, 3)),
                card(new PropertyCard("Regent Street", "Green property", Color.GREEN, 4)),
                card(new PropertyCard("Park Lane", "Dark Blue property", Color.DARK_BLUE, 4)),
                card(new PropertyCard("Park Lane", "Black property", Color.BLACK, 2)),
                card(new PropertyCard("Park Lane", "Light Green property", Color.LIGHT_GREEN, 2))
        );
        propertySection.getChildren().add(propertyCards);

        VBox wildSection = createSection("Wild Property Cards");
        FlowPane wildCards = new FlowPane(10, 10);
        wildCards.setPrefWrapLength(1000);
        wildCards.getChildren().addAll(
                card(new WildpropertyCard("Dark Blue/Green", "Wild property", 4,
                        List.of(Color.DARK_BLUE, Color.GREEN), true)),
                card(new WildpropertyCard("Light Blue/Green", "Wild property", 1,
                        List.of(Color.LIGHT_BLUE, Color.BROWN), true)),
                card(new WildpropertyCard("All Color", "Wild property", 0,
                        List.of(Color.values()), false)),
                card(new WildpropertyCard("Orange/Pink", "Wild property", 2,
                        List.of(Color.ORANGE, Color.PINK), true)),
                card(new WildpropertyCard("Green/Black", "Wild property", 4,
                        List.of(Color.GREEN, Color.BLACK), true)),
                card(new WildpropertyCard("Light_Blue/Black", "Wild property", 4,
                        List.of(Color.LIGHT_BLUE, Color.BLACK), true)),
                card(new WildpropertyCard("Light_Green/Black", "Wild property", 2,
                        List.of(Color.LIGHT_GREEN, Color.BLACK), true)),
                card(new WildpropertyCard("Yellow/Red", "Wild property", 3,
                        List.of(Color.YELLOW, Color.RED), true))
        );
        wildSection.getChildren().add(wildCards);

        VBox moneySection = createSection("Money Cards");
        FlowPane moneyCards = new FlowPane(10, 10);
        moneyCards.setPrefWrapLength(1000);
        moneyCards.getChildren().addAll(
                card(new MoneyCard("1M Banknote", "Worth 1 million", 1)),
                card(new MoneyCard("2M Banknote", "Worth 2 million", 2)),
                card(new MoneyCard("5M Banknote", "Worth 5 million", 5)),
                card(new MoneyCard("10M Banknote", "Worth 10 million", 10))
        );
        moneySection.getChildren().add(moneyCards);

        VBox actionSection = createSection("Action Cards");
        FlowPane actionCards = new FlowPane(10, 10);
        actionCards.setPrefWrapLength(1000);
        actionCards.getChildren().addAll(
                card(new PassGoCard("Pass Go", "Draw extra two card", CardType.ACTION)),
                card(new MyBirthday("My Birthday", "Everyone pays you 2M", CardType.ACTION)),
                card(new DoubleTheRent("Double The Rent", "Next Rent card charges double", CardType.ACTION)),
                card(new DealBreaker("Deal Breaker", "Steal a complete property set", CardType.ACTION)),
                card(new JustSayNo("Just Say No", "Cancel an action played against you", CardType.ACTION)),
                card(new SlyDeal("Sly Deal", "Steal one property", CardType.ACTION)),
                card(new ForcedDeal("Forced Deal", "Swap one property", CardType.ACTION)),
                card(new DebtCollector("Debt Collector", "Collect 5M from any player", CardType.ACTION)),
                card(new House("House", "Add house to a complete property set", CardType.ACTION)),
                card(new Hotel("Hotel", "Add hotel to a complete property set", CardType.ACTION))
        );
        actionSection.getChildren().add(actionCards);

        VBox rentSection = createSection("Rent Cards");
        FlowPane rentCards = new FlowPane(10, 10);
        rentCards.setPrefWrapLength(1000);
        rentCards.getChildren().addAll(
                card(RentCard.allColors()),
                card(RentCard.dual(Color.DARK_BLUE, Color.GREEN)),
                card(RentCard.dual(Color.BROWN, Color.LIGHT_BLUE)),
                card(RentCard.dual(Color.PINK, Color.ORANGE)),
                card(RentCard.dual(Color.BLACK, Color.LIGHT_GREEN)),
                card(RentCard.dual(Color.RED, Color.YELLOW))
        );
        rentSection.getChildren().add(rentCards);

        root.getChildren().addAll(
                titleLabel,
                propertySection,
                wildSection,
                moneySection,
                actionSection,
                rentSection
        );

        Scene scene = new Scene(root, 1100, 900);
        primaryStage.setTitle("Card View Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private CardView card(model.card.Card card) {
        return new CardView(card, true);
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
