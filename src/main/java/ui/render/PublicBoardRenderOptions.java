package ui.render;

import engine.WildPropertyRules;
import model.card.WildpropertyCard;
import model.player.Player;

/** Optional interactivity when rendering the public property board. */
public record PublicBoardRenderOptions(
        WildPropertyRecolorListener wildRecolorListener,
        int interactiveSeat,
        boolean allowWildRecolor,
        Player recolorRulesPlayer
) {
    public static PublicBoardRenderOptions none() {
        return new PublicBoardRenderOptions(null, -1, false, null);
    }

    /** Whether a played wild card should respond to clicks on the board. */
    public boolean canClickWildProperty(WildpropertyCard wild, int ownerSeat) {
        if (!allowWildRecolor
                || wildRecolorListener == null
                || recolorRulesPlayer == null
                || ownerSeat != interactiveSeat) {
            return false;
        }
        if (!WildPropertyRules.isRecolorable(wild)) {
            return false;
        }
        return WildPropertyRules.findOwnedWild(recolorRulesPlayer, wild) != null;
    }
}
