package ui.render;

import model.card.WildpropertyCard;

/**
 * Callback invoked when the local player clicks a recolorable wild property on the board.
 * <p>
 * Wired through {@link PublicBoardRenderOptions} during public board rendering.
 */
@FunctionalInterface
public interface WildPropertyRecolorListener {
    /**
     * Called when the local player clicks a recolorable wild property on the board.
     *
     * @param wild      the wild property card to recolor
     * @param ownerSeat seat index of the player who owns the card
     */
    void onWildPropertyRecolorRequested(WildpropertyCard wild, int ownerSeat);
}
