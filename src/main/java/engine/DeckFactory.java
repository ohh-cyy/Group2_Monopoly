package engine;

import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建deck
 */
public final class DeckFactory {

    private DeckFactory() {
    }

    /** Builds the full 110-card deck with properties, money, actions, wilds, and rent cards. */
    public static List<Card> createFullDeck() {
        List<Card> cards = new ArrayList<>();

        cards.add(new PropertyCard("Old Kent Road", "Brown property worth 1M", Color.BROWN, 1));
        cards.add(new PropertyCard("Whitechapel", "Brown property worth 1M", Color.BROWN, 1));

        cards.add(new PropertyCard("The Angel Islington", "Light Blue property worth 1M", Color.LIGHT_BLUE, 1));
        cards.add(new PropertyCard("Euston Road", "Light Blue property worth 1M", Color.LIGHT_BLUE, 1));
        cards.add(new PropertyCard("Pentonville Road", "Light Blue property worth 2M", Color.LIGHT_BLUE, 2));

        cards.add(new PropertyCard("Pall Mall", "Pink property worth 2M", Color.PINK, 2));
        cards.add(new PropertyCard("Whitehall", "Pink property worth 2M", Color.PINK, 2));
        cards.add(new PropertyCard("Northumberland Avenue", "Pink property worth 2M", Color.PINK, 2));

        cards.add(new PropertyCard("Bow Street", "Orange property worth 2M", Color.ORANGE, 2));
        cards.add(new PropertyCard("Marlborough Street", "Orange property worth 2M", Color.ORANGE, 2));
        cards.add(new PropertyCard("Vine Street", "Orange property worth 3M", Color.ORANGE, 3));

        cards.add(new PropertyCard("Strand", "Red property worth 3M", Color.RED, 3));
        cards.add(new PropertyCard("Fleet Street", "Red property worth 3M", Color.RED, 3));
        cards.add(new PropertyCard("Trafalgar Square", "Red property worth 3M", Color.RED, 3));

        cards.add(new PropertyCard("Leicester Square", "Yellow property worth 3M", Color.YELLOW, 3));
        cards.add(new PropertyCard("Coventry Street", "Yellow property worth 3M", Color.YELLOW, 3));
        cards.add(new PropertyCard("Piccadilly", "Yellow property worth 4M", Color.YELLOW, 4));

        cards.add(new PropertyCard("Regent Street", "Green property worth 4M", Color.GREEN, 4));
        cards.add(new PropertyCard("Oxford Street", "Green property worth 4M", Color.GREEN, 4));
        cards.add(new PropertyCard("Bond Street", "Green property worth 4M", Color.GREEN, 4));

        cards.add(new PropertyCard("Park Lane", "Dark Blue property worth 4M", Color.DARK_BLUE, 4));
        cards.add(new PropertyCard("Mayfair", "Dark Blue property worth 4M", Color.DARK_BLUE, 4));

        for (int i = 0; i < 6; i++) {
            cards.add(new MoneyCard("1M Banknote", "Worth 1 million", 1));
        }
        for (int i = 0; i < 5; i++) {
            cards.add(new MoneyCard("2M Banknote", "Worth 2 million", 2));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MoneyCard("3M Banknote", "Worth 3 million", 3));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MoneyCard("4M Banknote", "Worth 4 million", 4));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new MoneyCard("5M Banknote", "Worth 5 million", 5));
        }
        cards.add(new MoneyCard("10M Banknote", "Worth 10 million", 10));

        for (int i = 0; i < 10; i++) {
            cards.add(new PassGoCard("Pass Go", "Draw extra two card", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MyBirthday("My Birthday", "Everyone pays you 2M (property if short)", CardType.ACTION));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new DoubleTheRent("Double The Rent", "Next Rent card charges double", CardType.ACTION));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new DealBreaker("Deal Breaker", "Steal a complete property set from any player", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new JustSayNo("Just Say No", "Cancel an action played against you", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new SlyDeal("Sly Deal", "Steal one property (not from a complete set)", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new ForcedDeal("Forced Deal", "Swap one property with a player (not from their complete set)", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new DebtCollector("Debt Collector", "Collect 5M from any player", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new Hotel("Hotel", "Add hotel to a complete property set", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new House("House", "Add house to a complete property set", CardType.ACTION));
        }

        cards.add(new WildpropertyCard("Dark Blue/Green", "Wild property", 4, List.of(Color.DARK_BLUE, Color.GREEN), true));
        cards.add(new WildpropertyCard("Light Blue/Green", "Wild property", 1, List.of(Color.LIGHT_BLUE, Color.BROWN), true));
        for (int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("All Color", "Wild property", 0, List.of(Color.values()), false));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("Orange/Pink", "Wild property", 2, List.of(Color.ORANGE, Color.PINK), true));
        }
        cards.add(new WildpropertyCard("Green/Black", "Wild property", 4, List.of(Color.GREEN, Color.BLACK), true));
        cards.add(new WildpropertyCard("Light_Blue/Black", "Wild property", 4, List.of(Color.LIGHT_BLUE, Color.BLACK), true));
        cards.add(new WildpropertyCard("Light_Green/Black", "Wild property", 2, List.of(Color.LIGHT_GREEN, Color.BLACK), true));
        for (int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("Yellow/Red", "Wild property", 3, List.of(Color.YELLOW, Color.RED), true));
        }

        for (int i = 0; i < 3; i++) {
            cards.add(RentCard.allColors());
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.DARK_BLUE, Color.GREEN));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.BROWN, Color.LIGHT_BLUE));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.PINK, Color.ORANGE));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.BLACK, Color.LIGHT_GREEN));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.RED, Color.YELLOW));
        }

        return cards;
    }
}
