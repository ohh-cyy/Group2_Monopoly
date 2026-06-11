package model.card.actionCard;

import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测试 Forced Deal 交换地产的 swapProperties 逻辑。 */
class ForcedDealTest {

    @Test
    void swapPropertiesExchangesOnePropertyFromEachPlayer() {
        Player player = player("Player");
        Player target = player("Target");
        PropertyCard playerProperty = property("Old Kent Road", Color.BROWN, 1);
        PropertyCard targetProperty = property("Pall Mall", Color.PINK, 2);
        player.addProperty(playerProperty);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean swapped = forcedDeal.swapProperties(player, playerProperty, target, targetProperty);

        assertTrue(swapped);
        assertTrue(player.getAllProperties().contains(targetProperty));
        assertFalse(player.getAllProperties().contains(playerProperty));
        assertTrue(target.getAllProperties().contains(playerProperty));
        assertFalse(target.getAllProperties().contains(targetProperty));
    }

    @Test
    void swapPropertiesRejectsPlayerPropertyInCompleteSet() {
        Player player = player("Player");
        Player target = player("Target");
        List<PropertyCard> completeSet = addCompleteSet(player, Color.PINK);
        PropertyCard protectedProperty = completeSet.get(0);
        PropertyCard targetProperty = property("Old Kent Road", Color.BROWN, 1);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean swapped = forcedDeal.swapProperties(player, protectedProperty, target, targetProperty);

        assertFalse(swapped);
        assertTrue(player.getAllProperties().contains(protectedProperty));
        assertTrue(target.getAllProperties().contains(targetProperty));
    }

    @Test
    void swapPropertiesRejectsTargetPropertyInCompleteSet() {
        Player player = player("Player");
        Player target = player("Target");
        PropertyCard playerProperty = property("Old Kent Road", Color.BROWN, 1);
        player.addProperty(playerProperty);
        List<PropertyCard> completeSet = addCompleteSet(target, Color.BROWN);
        PropertyCard protectedProperty = completeSet.get(0);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean swapped = forcedDeal.swapProperties(player, playerProperty, target, protectedProperty);

        assertFalse(swapped);
        assertTrue(player.getAllProperties().contains(playerProperty));
        assertTrue(target.getAllProperties().contains(protectedProperty));
    }

    @Test
    void swapPropertiesRejectsCardsNotOwnedBySelectedPlayers() {
        Player player = player("Player");
        Player target = player("Target");
        PropertyCard playerProperty = property("Old Kent Road", Color.BROWN, 1);
        PropertyCard targetProperty = property("Pall Mall", Color.PINK, 2);
        PropertyCard unowned = property("Unowned", Color.RED, 3);
        player.addProperty(playerProperty);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        assertFalse(forcedDeal.swapProperties(player, unowned, target, targetProperty));
        assertFalse(forcedDeal.swapProperties(player, playerProperty, target, unowned));
        assertTrue(player.getAllProperties().contains(playerProperty));
        assertTrue(target.getAllProperties().contains(targetProperty));
    }

    @Test
    void swapPropertiesRejectsInvalidArguments() {
        Player player = player("Player");
        Player target = player("Target");
        PropertyCard playerProperty = property("Old Kent Road", Color.BROWN, 1);
        PropertyCard targetProperty = property("Pall Mall", Color.PINK, 2);
        player.addProperty(playerProperty);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        assertFalse(forcedDeal.swapProperties(player, playerProperty, player, targetProperty));
        assertFalse(forcedDeal.swapProperties(null, playerProperty, target, targetProperty));
        assertFalse(forcedDeal.swapProperties(player, null, target, targetProperty));
        assertFalse(forcedDeal.swapProperties(player, playerProperty, null, targetProperty));
        assertFalse(forcedDeal.swapProperties(player, playerProperty, target, null));
    }

    @Test
    void proactiveUseHasNoEffectBecauseControllerSelectsTheProperties() {
        Player player = player("Player");
        Player target = player("Target");
        PropertyCard playerProperty = property("Old Kent Road", Color.BROWN, 1);
        PropertyCard targetProperty = property("Pall Mall", Color.PINK, 2);
        player.addProperty(playerProperty);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        forcedDeal.use(player, game(player, target));

        assertTrue(player.getAllProperties().contains(playerProperty));
        assertTrue(target.getAllProperties().contains(targetProperty));
    }
}
