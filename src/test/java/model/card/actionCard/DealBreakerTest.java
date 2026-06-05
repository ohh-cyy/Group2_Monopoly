package model.card.actionCard;

import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class DealBreakerTest {

    @Test
    void useOnTargetStealsCompletePropertySet() {
        Player thief = player("Thief");
        Player target = player("Target");
        List<PropertyCard> targetSet = addCompleteSet(target, Color.BROWN);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        boolean moved = dealBreaker.useOnTarget(thief, target, Color.BROWN);

        assertTrue(moved);
        assertTrue(target.getPropertiesByColor(Color.BROWN).isEmpty());
        assertTrue(thief.getPropertiesByColor(Color.BROWN).containsAll(targetSet));
    }

    @Test
    void useOnTargetRejectsIncompleteSet() {
        Player thief = player("Thief");
        Player target = player("Target");
        PropertyCard property = property("Old Kent Road", Color.BROWN, 1);
        target.addProperty(property);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        boolean moved = dealBreaker.useOnTarget(thief, target, Color.BROWN);

        assertFalse(moved);
        assertTrue(target.getAllProperties().contains(property));
        assertTrue(thief.getAllProperties().isEmpty());
    }

    @Test
    void useOnTargetRejectsInvalidParticipantsOrColor() {
        Player player = player("Player");
        addCompleteSet(player, Color.BROWN);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        assertFalse(dealBreaker.useOnTarget(player, player, Color.BROWN));
        assertFalse(dealBreaker.useOnTarget(null, player, Color.BROWN));
        assertFalse(dealBreaker.useOnTarget(player, null, Color.BROWN));
        assertFalse(dealBreaker.useOnTarget(player("Thief"), player, null));
        assertEquals(Color.BROWN.getSetSize(), player.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void proactiveUseHasNoEffectBecauseControllerSelectsTheTarget() {
        Player thief = player("Thief");
        Player target = player("Target");
        addCompleteSet(target, Color.BROWN);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        dealBreaker.use(thief, game(thief, target));

        assertTrue(thief.getAllProperties().isEmpty());
        assertEquals(Color.BROWN.getSetSize(), target.getPropertiesByColor(Color.BROWN).size());
    }
}
