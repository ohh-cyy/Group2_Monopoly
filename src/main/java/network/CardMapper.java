package network;

import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import network.protocol.CardDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CardMapper {
    private CardMapper() {
    }

    public static CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.id = card.getInstanceId();
        dto.name = card.getName();
        dto.description = card.getDescription();
        dto.type = card.getType().name();

        if (card instanceof MoneyCard money) {
            dto.cardKind = "MONEY";
            dto.money = money.getMoney();
        } else if (card instanceof WildpropertyCard wild) {
            dto.cardKind = "WILD_PROPERTY";
            dto.bankValue = wild.getBankValueM();
            dto.bankable = wild.isBankable();
            dto.price = wild.getPrice();
            if (wild.getColor() != null) {
                dto.color = wild.getColor().name();
            }
            dto.wildColors = wild.getAvailableColors().stream().map(Enum::name).collect(Collectors.toList());
        } else if (card instanceof PropertyCard property) {
            dto.cardKind = "PROPERTY";
            dto.color = property.getColor() != null ? property.getColor().name() : null;
            dto.price = property.getPrice();
        } else if (card instanceof RentCard rent) {
            dto.cardKind = "RENT";
            dto.bankValue = rent.getBankValueM();
            dto.allColorsRent = rent.isAllColors();
            dto.rentColors = Arrays.stream(rent.getApplicableColors()).map(Enum::name).collect(Collectors.toList());
        } else if (card instanceof PassGoCard) {
            dto.cardKind = "PASS_GO";
            dto.bankValue = 1;
        } else if (card instanceof MyBirthday) {
            dto.cardKind = "MY_BIRTHDAY";
            dto.bankValue = 2;
        } else if (card instanceof DoubleTheRent) {
            dto.cardKind = "DOUBLE_THE_RENT";
            dto.bankValue = 1;
        } else if (card instanceof DealBreaker) {
            dto.cardKind = "DEAL_BREAKER";
            dto.bankValue = 5;
        } else if (card instanceof JustSayNo) {
            dto.cardKind = "JUST_SAY_NO";
            dto.bankValue = 4;
        } else if (card instanceof SlyDeal) {
            dto.cardKind = "SLY_DEAL";
            dto.bankValue = 3;
        } else if (card instanceof ForcedDeal) {
            dto.cardKind = "FORCED_DEAL";
            dto.bankValue = 3;
        } else if (card instanceof DebtCollector) {
            dto.cardKind = "DEBT_COLLECTOR";
            dto.bankValue = 3;
        } else if (card instanceof Hotel) {
            dto.cardKind = "HOTEL";
            dto.bankValue = 4;
        } else if (card instanceof House) {
            dto.cardKind = "HOUSE";
            dto.bankValue = 3;
        } else if (card instanceof ActionCard action) {
            dto.cardKind = "ACTION";
            dto.bankValue = action.getBankValueM();
        } else {
            dto.cardKind = "UNKNOWN";
        }
        return dto;
    }

    public static List<CardDto> toDtos(List<Card> cards) {
        List<CardDto> list = new ArrayList<>();
        for (Card card : cards) {
            list.add(toDto(card));
        }
        return list;
    }

    public static Card fromDto(CardDto dto) {
        if (dto == null || dto.type == null || dto.cardKind == null) {
            return null;
        }
        try {
            String id = dto.id != null ? dto.id : java.util.UUID.randomUUID().toString();
            CardType type = CardType.valueOf(dto.type);
            return switch (dto.cardKind) {
            case "MONEY" -> new MoneyCard(id, dto.name, dto.description, dto.money != null ? dto.money : 0);
            case "PROPERTY" -> new PropertyCard(id, dto.name, dto.description,
                    parseColor(dto.color), dto.price != null ? dto.price : 0);
            case "WILD_PROPERTY" -> {
                int bankValue = dto.bankValue != null ? dto.bankValue
                        : (dto.price != null ? dto.price : 0);
                WildpropertyCard wild = new WildpropertyCard(id, dto.name, dto.description,
                        bankValue,
                        parseColors(dto.wildColors),
                        dto.bankable != null && dto.bankable);
                Color chosen = parseColor(dto.color);
                if (chosen != null) {
                    wild.setChosenColor(chosen);
                }
                yield wild;
            }
            case "RENT" -> {
                if (Boolean.TRUE.equals(dto.allColorsRent)) {
                    yield RentCard.allColors(id);
                }
                List<Color> colors = parseColors(dto.rentColors);
                if (colors.size() >= 2) {
                    yield RentCard.dual(id, colors.get(0), colors.get(1));
                }
                yield RentCard.allColors(id);
            }
            case "PASS_GO" -> new PassGoCard(id, dto.name, dto.description, type);
            case "MY_BIRTHDAY" -> new MyBirthday(id, dto.name, dto.description, type);
            case "DOUBLE_THE_RENT" -> new DoubleTheRent(id, dto.name, dto.description, type);
            case "DEAL_BREAKER" -> new DealBreaker(id, dto.name, dto.description, type);
            case "JUST_SAY_NO" -> new JustSayNo(id, dto.name, dto.description, type);
            case "SLY_DEAL" -> new SlyDeal(id, dto.name, dto.description, type);
            case "FORCED_DEAL" -> new ForcedDeal(id, dto.name, dto.description, type);
            case "DEBT_COLLECTOR" -> new DebtCollector(id, dto.name, dto.description, type);
            case "HOTEL" -> new Hotel(id, dto.name, dto.description, type);
            case "HOUSE" -> new House(id, dto.name, dto.description, type);
            default -> new SimpleActionCard(id, dto.name, dto.description, type,
                    dto.bankValue != null ? dto.bankValue : 0);
            };
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static List<Card> fromDtos(List<CardDto> dtos) {
        List<Card> cards = new ArrayList<>();
        if (dtos == null) {
            return cards;
        }
        for (CardDto dto : dtos) {
            Card card = fromDto(dto);
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
        try {
            return Color.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
