package network;

import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import network.protocol.CardDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 服务端 Card ↔ DTO；客户端 DTO → NetworkCard */
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
            if (wild.getColor() != null) {
                dto.color = wild.getColor().name();
            }
            dto.wildColors = wild.getAvailableColors().stream().map(Enum::name).collect(Collectors.toList());
            dto.price = wild.getPrice();
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

    public static Card fromDto(CardDto dto) {
        return new NetworkCard(dto);
    }

    public static List<Card> fromDtoList(List<CardDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(CardMapper::fromDto).collect(Collectors.toList());
    }

    public static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Color.valueOf(value.trim());
    }
}
