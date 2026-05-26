package network;

import model.card.Card;
import model.enums.CardType;
import model.enums.Color;
import network.protocol.CardDto;

import java.util.ArrayList;
import java.util.List;

/** 客户端用于显示的卡牌（由服务端 DTO 重建） */
public class NetworkCard extends Card {
    private final CardDto dto;
    private final String cardKind;
    private final int money;
    private final int bankValue;
    private final int price;
    private final boolean bankable;
    private final boolean allColorsRent;
    private final List<Color> wildColors;
    private final List<Color> rentColors;

    public NetworkCard(CardDto dto) {
        super(dto.id, dto.name, dto.description, CardType.valueOf(dto.type));
        this.dto = dto;
        this.cardKind = dto.cardKind != null ? dto.cardKind : "UNKNOWN";
        this.money = dto.money != null ? dto.money : 0;
        this.bankValue = dto.bankValue != null ? dto.bankValue : 0;
        this.price = dto.price != null ? dto.price : 0;
        this.bankable = dto.bankable != null && dto.bankable;
        this.allColorsRent = dto.allColorsRent != null && dto.allColorsRent;
        this.wildColors = parseColors(dto.wildColors);
        this.rentColors = parseColors(dto.rentColors);
    }

    public CardDto getDto() {
        return dto;
    }

    public String getCardKind() {
        return cardKind;
    }

    public int getMoney() {
        return money;
    }

    public int getBankValueM() {
        return bankValue;
    }

    public int getPrice() {
        return price;
    }

    public boolean isBankable() {
        return bankable;
    }

    public boolean isAllColorsRent() {
        return allColorsRent;
    }

    public List<Color> getAvailableColors() {
        return new ArrayList<>(wildColors);
    }

    public List<Color> getRentColors() {
        return new ArrayList<>(rentColors);
    }

    @Override
    public Color getColor() {
        return CardMapper.parseColor(dto.color);
    }

    @Override
    public void use(model.player.Player player, engine.GameEngine game) {
        throw new UnsupportedOperationException("NetworkCard is display-only on client");
    }

    private static List<Color> parseColors(List<String> names) {
        List<Color> colors = new ArrayList<>();
        if (names == null) {
            return colors;
        }
        for (String name : names) {
            Color color = CardMapper.parseColor(name);
            if (color != null) {
                colors.add(color);
            }
        }
        return colors;
    }
}
