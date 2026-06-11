package model.card.actionCard;

import engine.GameEngine;
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
        GameEngine game = game(thief, target);
        List<PropertyCard> targetSet = addCompleteSet(target, Color.BROWN);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        boolean moved = dealBreaker.useOnTarget(thief, target, Color.BROWN, game);

        assertTrue(moved);
        assertTrue(target.getPropertiesByColor(Color.BROWN).isEmpty());
        assertTrue(thief.getPropertiesByColor(Color.BROWN).containsAll(targetSet));
        assertTrue(game.getDiscardPile().isEmpty());
    }

    @Test
    void useOnTargetDiscardsOverflowWhenThiefAlreadyOwnsSameColor() {
        Player thief = player("Thief");
        Player target = player("Target");
        GameEngine game = game(thief, target);
        PropertyCard existingPink = property("Pall Mall", Color.PINK, 2);
        thief.addProperty(existingPink);
        List<PropertyCard> targetSet = addCompleteSet(target, Color.PINK);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        boolean moved = dealBreaker.useOnTarget(thief, target, Color.PINK, game);

        assertTrue(moved);
        assertTrue(target.getPropertiesByColor(Color.PINK).isEmpty());
        assertEquals(Color.PINK.getSetSize(), thief.getPropertiesByColor(Color.PINK).size());
        assertTrue(thief.getPropertiesByColor(Color.PINK).contains(existingPink));
        assertEquals(1, game.getDiscardPile().size());
        assertTrue(targetSet.contains(game.getDiscardPile().peekTop()));
    }

    @Test
    void useOnTargetRejectsIncompleteSet() {
        Player thief = player("Thief");
        Player target = player("Target");
        GameEngine game = game(thief, target);
        PropertyCard property = property("Old Kent Road", Color.BROWN, 1);
        target.addProperty(property);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        boolean moved = dealBreaker.useOnTarget(thief, target, Color.BROWN, game);

        assertFalse(moved);
        assertTrue(target.getAllProperties().contains(property));
        assertTrue(thief.getAllProperties().isEmpty());
    }

    @Test
    void useOnTargetRejectsInvalidParticipantsOrColor() {
        Player player = player("Player");
        GameEngine game = game(player);
        addCompleteSet(player, Color.BROWN);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a set", CardType.ACTION);

        assertFalse(dealBreaker.useOnTarget(player, player, Color.BROWN, game));
        assertFalse(dealBreaker.useOnTarget(null, player, Color.BROWN, game));
        assertFalse(dealBreaker.useOnTarget(player, null, Color.BROWN, game));
        assertFalse(dealBreaker.useOnTarget(player("Thief"), player, null, game));
        assertFalse(dealBreaker.useOnTarget(player, player("Target"), Color.BROWN, null));
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
