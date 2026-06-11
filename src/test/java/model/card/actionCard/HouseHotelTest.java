package model.card.actionCard;

import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测试 House / Hotel 加到完整套上的逻辑。 */
class HouseHotelTest {

    @Test
    void addHouseToSetAddsHouseWhenSetIsComplete() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        House house = new House("House", "Add house", CardType.ACTION);

        boolean added = house.addHouseToSet(player, Color.BROWN);

        assertTrue(added);
        assertTrue(house.hasHouse(player, Color.BROWN));
        assertEquals(Color.BROWN.getSetSize() + 1, player.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void addHouseToSetRejectsIncompleteSetAndDuplicateHouse() {
        Player player = player("Player");
        player.addProperty(property("Old Kent Road", Color.BROWN, 1));
        House house = new House("House", "Add house", CardType.ACTION);

        assertFalse(house.addHouseToSet(player, Color.BROWN));

        player.addProperty(property("Whitechapel Road", Color.BROWN, 1));
        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertFalse(house.addHouseToSet(player, Color.BROWN));
        assertEquals(Color.BROWN.getSetSize() + 1, player.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void addHouseToSetRejectsInvalidArguments() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        House house = new House("House", "Add house", CardType.ACTION);

        assertFalse(house.addHouseToSet(null, Color.BROWN));
        assertFalse(house.addHouseToSet(player, null));
        assertFalse(house.hasHouse(null, Color.BROWN));
        assertFalse(house.hasHouse(player, null));
    }

    @Test
    void addHotelToSetRequiresCompleteSetWithHouse() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        House house = new House("House", "Add house", CardType.ACTION);
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);

        assertFalse(hotel.addHotelToSet(player, Color.BROWN));

        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertTrue(hotel.addHotelToSet(player, Color.BROWN));
        assertTrue(hotel.hasHotel(player, Color.BROWN));
        assertEquals(Color.BROWN.getSetSize() + 2, player.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void addHotelToSetRejectsDuplicateHotelAndInvalidArguments() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        House house = new House("House", "Add house", CardType.ACTION);
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);

        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertTrue(hotel.addHotelToSet(player, Color.BROWN));

        assertFalse(hotel.addHotelToSet(player, Color.BROWN));
        assertFalse(hotel.addHotelToSet(null, Color.BROWN));
        assertFalse(hotel.addHotelToSet(player, null));
        assertFalse(hotel.hasHotel(null, Color.BROWN));
        assertFalse(hotel.hasHotel(player, null));
    }

    @Test
    void proactiveUseMethodsHaveNoEffectBecauseControllerSelectsTheSet() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        House house = new House("House", "Add house", CardType.ACTION);
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);

        house.use(player, game(player, player("Opponent")));
        hotel.use(player, game(player, player("Opponent")));

        assertFalse(house.hasHouse(player, Color.BROWN));
        assertFalse(hotel.hasHotel(player, Color.BROWN));
        assertEquals(Color.BROWN.getSetSize(), player.getPropertiesByColor(Color.BROWN).size());
    }
}
