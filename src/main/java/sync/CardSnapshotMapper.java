package sync;

import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CardSnapshotMapper {

    private CardSnapshotMapper() {
    }

    public static CardSnapshot toSnapshot(Card card) {
        CardSnapshot s = new CardSnapshot();
        s.id = card.getInstanceId();
        s.name = card.getName();
        s.description = card.getDescription();
        s.type = card.getType().name();

        if (card instanceof MoneyCard money) {
            s.cardKind = "MONEY";
            s.money = money.getMoney();
        } else if (card instanceof WildpropertyCard wild) {
            s.cardKind = "WILD_PROPERTY";
            s.bankValue = wild.getBankValueM();
            s.bankable = wild.isBankable();
            s.price = wild.getPrice();
            if (wild.getColor() != null) {
                s.color = wild.getColor().name();
            }
            s.wildColors = wild.getAvailableColors().stream().map(Enum::name).collect(Collectors.toList());
        } else if (card instanceof PropertyCard property) {
            s.cardKind = "PROPERTY";
            s.color = property.getColor() != null ? property.getColor().name() : null;
            s.price = property.getPrice();
        } else if (card instanceof RentCard rent) {
            s.cardKind = "RENT";
            s.bankValue = rent.getBankValueM();
            s.allColorsRent = rent.isAllColors();
            s.rentColors = Arrays.stream(rent.getApplicableColors()).map(Enum::name).collect(Collectors.toList());
        } else if (card instanceof PassGoCard) {
            s.cardKind = "PASS_GO";
            s.bankValue = 1;
        } else if (card instanceof MyBirthday) {
            s.cardKind = "MY_BIRTHDAY";
            s.bankValue = 2;
        } else if (card instanceof DoubleTheRent) {
            s.cardKind = "DOUBLE_THE_RENT";
            s.bankValue = 1;
        } else if (card instanceof DealBreaker) {
            s.cardKind = "DEAL_BREAKER";
            s.bankValue = 5;
        } else if (card instanceof JustSayNo) {
            s.cardKind = "JUST_SAY_NO";
            s.bankValue = 4;
        } else if (card instanceof SlyDeal) {
            s.cardKind = "SLY_DEAL";
            s.bankValue = 3;
        } else if (card instanceof ForcedDeal) {
            s.cardKind = "FORCED_DEAL";
            s.bankValue = 3;
        } else if (card instanceof DebtCollector) {
            s.cardKind = "DEBT_COLLECTOR";
            s.bankValue = 3;
        } else if (card instanceof Hotel) {
            s.cardKind = "HOTEL";
            s.bankValue = 4;
        } else if (card instanceof House) {
            s.cardKind = "HOUSE";
            s.bankValue = 3;
        } else if (card instanceof ActionCard action) {
            s.cardKind = "ACTION";
            s.bankValue = action.getBankValueM();
        } else {
            s.cardKind = "UNKNOWN";
        }
        return s;
    }

    public static List<CardSnapshot> toSnapshots(List<Card> cards) {
        List<CardSnapshot> list = new ArrayList<>();
        for (Card card : cards) {
            list.add(toSnapshot(card));
        }
        return list;
    }

    public static Card fromSnapshot(CardSnapshot s) {
        if (s == null) {
            return null;
        }
        String id = s.id != null ? s.id : java.util.UUID.randomUUID().toString();
        CardType type = CardType.valueOf(s.type);

        return switch (s.cardKind != null ? s.cardKind : "UNKNOWN") {
            case "MONEY" -> new MoneyCard(id, s.name, s.description, s.money != null ? s.money : 0);
            case "PROPERTY" -> new PropertyCard(id, s.name, s.description,
                    parseColor(s.color), s.price != null ? s.price : 0);
            case "WILD_PROPERTY" -> {
                WildpropertyCard wild = new WildpropertyCard(id, s.name, s.description,
                        s.bankValue != null ? s.bankValue : 0,
                        parseColors(s.wildColors),
                        s.bankable != null && s.bankable);
                Color chosen = parseColor(s.color);
                if (chosen != null) {
                    wild.setChosenColor(chosen);
                }
                yield wild;
            }
            case "RENT" -> {
                if (Boolean.TRUE.equals(s.allColorsRent)) {
                    yield RentCard.allColors(id);
                }
                List<Color> colors = parseColors(s.rentColors);
                if (colors.size() >= 2) {
                    yield RentCard.dual(id, colors.get(0), colors.get(1));
                }
                yield RentCard.allColors(id);
            }
            case "PASS_GO" -> new PassGoCard(id, s.name, s.description, type);
            case "MY_BIRTHDAY" -> new MyBirthday(id, s.name, s.description, type);
            case "DOUBLE_THE_RENT" -> new DoubleTheRent(id, s.name, s.description, type);
            case "DEAL_BREAKER" -> new DealBreaker(id, s.name, s.description, type);
            case "JUST_SAY_NO" -> new JustSayNo(id, s.name, s.description, type);
            case "SLY_DEAL" -> new SlyDeal(id, s.name, s.description, type);
            case "FORCED_DEAL" -> new ForcedDeal(id, s.name, s.description, type);
            case "DEBT_COLLECTOR" -> new DebtCollector(id, s.name, s.description, type);
            case "HOTEL" -> new Hotel(id, s.name, s.description, type);
            case "HOUSE" -> new House(id, s.name, s.description, type);
            default -> new SimpleActionCard(id, s.name, s.description, type,
                    s.bankValue != null ? s.bankValue : 0);
        };
    }

    public static List<Card> fromSnapshots(List<CardSnapshot> snapshots) {
        List<Card> cards = new ArrayList<>();
        if (snapshots == null) {
            return cards;
        }
        for (CardSnapshot snapshot : snapshots) {
            Card card = fromSnapshot(snapshot);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    public static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Color.valueOf(value.trim());
    }

    private static List<Color> parseColors(List<String> names) {
        List<Color> colors = new ArrayList<>();
        if (names == null) {
            return colors;
        }
        for (String name : names) {
            Color color = parseColor(name);
            if (color != null) {
                colors.add(color);
            }
        }
        return colors;
    }
}
