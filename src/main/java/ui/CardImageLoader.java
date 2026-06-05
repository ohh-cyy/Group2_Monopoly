package ui;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.*;
import model.enums.Color;

import java.util.HashSet;
import java.util.Set;

/**
 * Maps card objects to PNG resources under {@code src/main/resources/image/Card/}.
 */
public final class CardImageLoader {
    private static final String BASE = "/Card/";

    private CardImageLoader() {
    }

    public static String resolvePath(Card card) {
        if (card == null) {
            return null;
        }
        
        // Check for improvement cards first (House/Hotel added to property sets)
        if (card instanceof PropertyCard && card.getName() != null) {
            if (card.getName().startsWith("House+")) {
                return BASE + "actioncard/house.png";
            }
            if (card.getName().startsWith("Hotel+")) {
                return BASE + "actioncard/hotel.png";
            }
        }
        
        if (card instanceof WildpropertyCard wild) {
            return resolveWildPath(wild);
        }
        if (card instanceof MoneyCard money) {
            return BASE + "moneycard/" + money.getMoney() + "m.png";
        }
        if (card instanceof PropertyCard property && property.getColor() != null) {
            return BASE + "propertycard/" + propertyFileName(property.getColor());
        }
        if (card instanceof RentCard rent) {
            return resolveRentPath(rent);
        }
        if (card instanceof PassGoCard) {
            return BASE + "actioncard/passgo.png";
        }
        if (card instanceof MyBirthday) {
            return BASE + "actioncard/it'smybirthday.png";
        }
        if (card instanceof DoubleTheRent) {
            return BASE + "actioncard/doubletherent.png";
        }
        if (card instanceof DealBreaker) {
            return BASE + "actioncard/dealbreaker.png";
        }
        if (card instanceof JustSayNo) {
            return BASE + "actioncard/justsayno.png";
        }
        if (card instanceof SlyDeal) {
            return BASE + "actioncard/slydeal.png";
        }
        if (card instanceof ForcedDeal) {
            return BASE + "actioncard/forcedeal.png";
        }
        if (card instanceof DebtCollector) {
            return BASE + "actioncard/deptcollector.png";
        }
        if (card instanceof House) {
            return BASE + "actioncard/house.png";
        }
        if (card instanceof Hotel) {
            return BASE + "actioncard/hotel.png";
        }
        return null;
    }

    public static double resolveRotationDegrees(Card card) {
        if (!(card instanceof WildpropertyCard wild)) {
            return 0;
        }
        Color chosen = wild.getChosenColor();
        Color bottomColor = bottomColorForWild(wild);
        if (chosen == null || bottomColor == null) {
            return 0;
        }
        return chosen == bottomColor ? 180 : 0;
    }

    private static String propertyFileName(Color color) {
        return switch (color) {
            case BROWN -> "propertybrown.png";
            case LIGHT_BLUE -> "propertylight_blue.png";
            case PINK -> "propertypink.png";
            case ORANGE -> "propertyorange.png";
            case RED -> "propertyred.png";
            case YELLOW -> "propertyyellow.png";
            case GREEN -> "propertydarkgreen.png";
            case DARK_BLUE -> "propertydark_blue.png";
            case BLACK -> "propertyblack.png";
            case LIGHT_GREEN -> "propertylight_green.png";
        };
    }

    private static String resolveWildPath(WildpropertyCard wild) {
        Set<Color> colors = new HashSet<>(wild.getAvailableColors());
        if (!wild.isBankable() && colors.size() >= Color.values().length - 1) {
            return BASE + "wildpropertycard/wildpropertyallcolor.png";
        }
        if (colors.containsAll(Set.of(Color.DARK_BLUE, Color.GREEN))) {
            return BASE + "wildpropertycard/wildpropertydark_greendark_blue.png";
        }
        if (colors.containsAll(Set.of(Color.LIGHT_BLUE, Color.BROWN))) {
            return BASE + "wildpropertycard/wildpropertylight_bluebrown.png";
        }
        if (colors.containsAll(Set.of(Color.ORANGE, Color.PINK))) {
            return BASE + "wildpropertycard/wildpropertyorangepinl.png";
        }
        if (colors.containsAll(Set.of(Color.GREEN, Color.BLACK))) {
            return BASE + "wildpropertycard/wildpropertydark_greenblack.png";
        }
        if (colors.containsAll(Set.of(Color.LIGHT_BLUE, Color.BLACK))) {
            return BASE + "wildpropertycard/wildpropertylight_blueblack.png";
        }
        if (colors.containsAll(Set.of(Color.LIGHT_GREEN, Color.BLACK))) {
            return BASE + "wildpropertycard/wildpropertylight_greenblack.png";
        }
        if (colors.containsAll(Set.of(Color.YELLOW, Color.RED))) {
            return BASE + "wildpropertycard/wildpropertyyellowred.png";
        }
        return BASE + "wildpropertycard/wildpropertyallcolor.png";
    }

    private static Color bottomColorForWild(WildpropertyCard wild) {
        Set<Color> colors = new HashSet<>(wild.getAvailableColors());
        if (!wild.isBankable() && colors.size() >= Color.values().length - 1) {
            return null;
        }
        if (colors.containsAll(Set.of(Color.DARK_BLUE, Color.GREEN))) {
            return Color.DARK_BLUE;
        }
        if (colors.containsAll(Set.of(Color.LIGHT_BLUE, Color.BROWN))) {
            return Color.BROWN;
        }
        if (colors.containsAll(Set.of(Color.ORANGE, Color.PINK))) {
            return Color.PINK;
        }
        if (colors.containsAll(Set.of(Color.GREEN, Color.BLACK))) {
            return Color.BLACK;
        }
        if (colors.containsAll(Set.of(Color.LIGHT_BLUE, Color.BLACK))) {
            return Color.BLACK;
        }
        if (colors.containsAll(Set.of(Color.LIGHT_GREEN, Color.BLACK))) {
            return Color.BLACK;
        }
        if (colors.containsAll(Set.of(Color.YELLOW, Color.RED))) {
            return Color.RED;
        }
        var available = wild.getAvailableColors();
        return available.size() == 2 ? available.get(1) : null;
    }

    private static String resolveRentPath(RentCard rent) {
        if (rent.isAllColors()) {
            return BASE + "rentcard/allcolorrent.png";
        }
        Color[] c = rent.getApplicableColors();
        if (c.length == 2) {
            Set<Color> set = Set.of(c[0], c[1]);
            if (set.contains(Color.DARK_BLUE) && set.contains(Color.GREEN)) {
                return BASE + "rentcard/dark_greendark_bluerent.png";
            }
            if (set.contains(Color.BROWN) && set.contains(Color.LIGHT_BLUE)) {
                return BASE + "rentcard/brownlight_bluerent.png";
            }
            if (set.contains(Color.PINK) && set.contains(Color.ORANGE)) {
                return BASE + "rentcard/pinkorangerent.png";
            }
            if (set.contains(Color.BLACK) && set.contains(Color.LIGHT_GREEN)) {
                return BASE + "rentcard/blacklight_greenrent.png";
            }
            if (set.contains(Color.RED) && set.contains(Color.YELLOW)) {
                return BASE + "rentcard/redyellowrent.png";
            }
        }
        return BASE + "rentcard/allcolorrent.png";
    }
}
