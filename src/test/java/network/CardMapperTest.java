package network;

import com.google.gson.Gson;
import model.card.WildpropertyCard;
import model.enums.Color;
import network.protocol.CardDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 测试 Card ↔ CardDto 网络序列化映射 {@link CardMapper}。 */
class CardMapperTest {

    private static final Gson GSON = new Gson();

    @Test
    void parseColorReturnsNullForBlankOrInvalidValues() {
        assertNull(CardMapper.parseColor(null));
        assertNull(CardMapper.parseColor(""));
        assertNull(CardMapper.parseColor("NOT_A_COLOR"));
    }

    @Test
    void parseColorReturnsMatchingEnumForValidValue() {
        assertEquals(Color.BROWN, CardMapper.parseColor("BROWN"));
    }

    @Test
    void orangePinkWildRoundTripPreservesTwoMBankValue() {
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild property", 2, List.of(Color.ORANGE, Color.PINK), true);

        CardDto dto = CardMapper.toDto(wild);
        String json = GSON.toJson(dto);
        CardDto restored = GSON.fromJson(json, CardDto.class);
        WildpropertyCard rebuilt = (WildpropertyCard) CardMapper.fromDto(restored);

        assertEquals(2, rebuilt.getBankValueM());
        assertEquals(2, rebuilt.getPrice());
    }

    @Test
    void wildPropertyFallsBackToPriceWhenBankValueMissing() {
        CardDto dto = new CardDto();
        dto.id = "wild-1";
        dto.name = "Orange/Pink";
        dto.description = "Wild property";
        dto.type = "PROPERTY";
        dto.cardKind = "WILD_PROPERTY";
        dto.price = 2;
        dto.wildColors = List.of(Color.ORANGE.name(), Color.PINK.name());
        dto.bankable = true;

        WildpropertyCard rebuilt = (WildpropertyCard) CardMapper.fromDto(dto);

        assertEquals(2, rebuilt.getBankValueM());
    }
}
