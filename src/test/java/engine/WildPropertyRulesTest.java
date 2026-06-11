package engine;

import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 测试万能地产换色规则 {@link WildPropertyRules}。 */
class WildPropertyRulesTest {

    /** 合法换色后，万能卡应从旧颜色组移到新颜色组。 */
    @Test
    void recolorMovesWildPropertyToAnotherAvailableColor() {
        Player player = new Player("P1");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        wild.setChosenColor(Color.ORANGE);
        player.addProperty(wild);

        assertTrue(WildPropertyRules.recolor(player, wild, Color.PINK));
        assertEquals(Color.PINK, wild.getChosenColor());
        assertTrue(player.getPropertiesByColor(Color.PINK).contains(wild));
        assertTrue(player.getPropertiesByColor(Color.ORANGE).isEmpty());
    }

    @Test
    void recolorRejectsWildAlreadyInCompleteSet() {
        Player player = new Player("P1");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        wild.setChosenColor(Color.ORANGE);
        player.addProperty(wild);
        for (int i = 1; i < Color.ORANGE.getSetSize(); i++) {
            player.addProperty(new PropertyCard("Orange " + i, "Test", Color.ORANGE, 1));
        }

        assertTrue(PropertyRules.isCompleteSet(player, Color.ORANGE));
        assertTrue(WildPropertyRules.getRecolorOptions(player, wild).isEmpty());
        assertFalse(WildPropertyRules.recolor(player, wild, Color.PINK));
        assertEquals(Color.ORANGE, wild.getChosenColor());
        assertTrue(player.getPropertiesByColor(Color.ORANGE).contains(wild));
    }

    @Test
    void recolorRejectsCompleteTargetSet() {
        Player player = new Player("P1");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        wild.setChosenColor(Color.ORANGE);
        player.addProperty(wild);
        addCompleteSet(player, Color.PINK);

        assertFalse(WildPropertyRules.recolor(player, wild, Color.PINK));
        assertEquals(Color.ORANGE, wild.getChosenColor());
    }

    @Test
    void findOwnedWildMatchesByInstanceId() {
        Player player = new Player("P1");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        wild.setChosenColor(Color.ORANGE);
        player.addProperty(wild);

        WildpropertyCard clickedView = new WildpropertyCard(
                wild.getInstanceId(), "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        clickedView.setChosenColor(Color.ORANGE);

        assertNotNull(WildPropertyRules.findOwnedWild(player, clickedView));
        assertFalse(WildPropertyRules.getRecolorOptions(player, clickedView).isEmpty());
    }

    @Test
    void recolorRejectsSameColor() {
        Player player = new Player("P1");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        wild.setChosenColor(Color.ORANGE);
        player.addProperty(wild);

        assertFalse(WildPropertyRules.recolor(player, wild, Color.ORANGE));
    }

    private static void addCompleteSet(Player player, Color color) {
        int needed = color.getSetSize();
        for (int i = 0; i < needed; i++) {
            player.addProperty(new PropertyCard("P" + i, "Test", color, 1));
        }
    }
}
