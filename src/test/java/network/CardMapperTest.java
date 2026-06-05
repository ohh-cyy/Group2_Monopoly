package network;

import model.enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CardMapperTest {

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
}
