package model.card.actionCard;

import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测试 Sly Deal 偷单张地产的 stealProperty 逻辑。 */
class SlyDealTest {

    @Test
    void stealPropertyMovesTargetPropertyOutsideCompleteSet() {
        Player thief = player("Thief");
        Player target = player("Target");
        PropertyCard property = property("Pall Mall", Color.PINK, 2);
        target.addProperty(property);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        boolean stolen = slyDeal.stealProperty(thief, target, property);

        assertTrue(stolen);
        assertTrue(thief.getAllProperties().contains(property));
        assertFalse(target.getAllProperties().contains(property));
    }

    @Test
    void stealPropertyRejectsPropertyInCompleteSet() {
        Player thief = player("Thief");
        Player target = player("Target");
        List<PropertyCard> completeSet = addCompleteSet(target, Color.BROWN);
        PropertyCard protectedProperty = completeSet.get(0);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        boolean stolen = slyDeal.stealProperty(thief, target, protectedProperty);

        assertFalse(stolen);
        assertFalse(thief.getAllProperties().contains(protectedProperty));
        assertTrue(target.getAllProperties().contains(protectedProperty));
    }

    @Test
    void stealPropertyRejectsPropertyTargetDoesNotOwn() {
        Player thief = player("Thief");
        Player target = player("Target");
        PropertyCard property = property("Pall Mall", Color.PINK, 2);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        boolean stolen = slyDeal.stealProperty(thief, target, property);

        assertFalse(stolen);
        assertTrue(thief.getAllProperties().isEmpty());
        assertTrue(target.getAllProperties().isEmpty());
    }

    @Test
    void stealPropertyRejectsInvalidArguments() {
        Player thief = player("Thief");
        Player target = player("Target");
        PropertyCard property = property("Pall Mall", Color.PINK, 2);
        target.addProperty(property);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        assertFalse(slyDeal.stealProperty(thief, thief, property));
        assertFalse(slyDeal.stealProperty(null, target, property));
        assertFalse(slyDeal.stealProperty(thief, null, property));
        assertFalse(slyDeal.stealProperty(thief, target, null));
        assertTrue(target.getAllProperties().contains(property));
    }

    @Test
    void proactiveUseHasNoEffectBecauseControllerSelectsTheProperty() {
        Player thief = player("Thief");
        Player target = player("Target");
        PropertyCard property = property("Pall Mall", Color.PINK, 2);
        target.addProperty(property);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        slyDeal.use(thief, game(thief, target));

        assertTrue(thief.getAllProperties().isEmpty());
        assertTrue(target.getAllProperties().contains(property));
    }
}
