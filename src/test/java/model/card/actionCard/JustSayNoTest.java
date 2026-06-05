package model.card.actionCard;

import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class JustSayNoTest {

    @Test
    void proactiveUseDoesNothingBecauseCardIsOnlyAResponse() {
        Player player = player("Player");
        Player opponent = player("Opponent");
        player.addBank(money(1));
        player.addProperty(property("Old Kent Road", Color.BROWN, 1));
        JustSayNo justSayNo = new JustSayNo("Just Say No", "Cancel action", CardType.ACTION);

        justSayNo.use(player, game(player, opponent));

        assertEquals(1, player.getBankTotalValue());
        assertEquals(1, player.getAllProperties().size());
        assertEquals(0, player.getHandSize());
    }

    @Test
    void proactiveUseDoesNotDiscardOrBankTheCard() {
        Player player = player("Player");
        Player opponent = player("Opponent");
        JustSayNo justSayNo = new JustSayNo("Just Say No", "Cancel action", CardType.ACTION);

        justSayNo.use(player, game(player, opponent));

        assertFalse(player.getBank().contains(justSayNo));
    }

    @Test
    void useToleratesNullInputs() {
        JustSayNo justSayNo = new JustSayNo("Just Say No", "Cancel action", CardType.ACTION);

        assertDoesNotThrow(() -> justSayNo.use(null, null));
    }
}
