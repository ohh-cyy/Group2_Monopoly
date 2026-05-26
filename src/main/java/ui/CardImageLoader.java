package ui;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.*;
import model.enums.Color;
import network.NetworkCard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将卡牌实例映射到 {@code src/main/resources/image/Card/} 下的 PNG 资源。
 */
public final class CardImageLoader {
    private static final String BASE = "/Card/";

    private CardImageLoader() {
    }

    public static String resolvePath(Card card) {
        if (card == null) {
            return null;
        }
        if (card instanceof NetworkCard networkCard) {
            return resolveNetworkCard(networkCard);
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

    private static String resolveNetworkCard(NetworkCard card) {
        return switch (card.getCardKind()) {
            case "MONEY" -> BASE + "moneycard/" + card.getMoney() + "m.png";
            case "PROPERTY" -> {
                Color color = card.getColor();
                yield color != null ? BASE + "propertycard/" + propertyFileName(color) : null;
            }
            case "WILD_PROPERTY" -> resolveNetworkWildPath(card);
            case "RENT" -> resolveNetworkRentPath(card);
            case "PASS_GO" -> BASE + "actioncard/passgo.png";
            case "MY_BIRTHDAY" -> BASE + "actioncard/it'smybirthday.png";
            case "DOUBLE_THE_RENT" -> BASE + "actioncard/doubletherent.png";
            case "DEAL_BREAKER" -> BASE + "actioncard/dealbreaker.png";
            case "JUST_SAY_NO" -> BASE + "actioncard/justsayno.png";
            case "SLY_DEAL" -> BASE + "actioncard/slydeal.png";
            case "FORCED_DEAL" -> BASE + "actioncard/forcedeal.png";
            case "DEBT_COLLECTOR" -> BASE + "actioncard/deptcollector.png";
            case "HOUSE" -> BASE + "actioncard/house.png";
            case "HOTEL" -> BASE + "actioncard/hotel.png";
            default -> null;
        };
    }

    private static String resolveNetworkWildPath(NetworkCard wild) {
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

    private static String resolveNetworkRentPath(NetworkCard rent) {
        if (rent.isAllColorsRent()) {
            return BASE + "rentcard/allcolorrent.png";
        }
        List<Color> colors = rent.getRentColors();
        if (colors.size() == 2) {
            Set<Color> set = Set.of(colors.get(0), colors.get(1));
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