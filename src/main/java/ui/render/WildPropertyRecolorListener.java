package ui.render;

import model.card.WildpropertyCard;

/** Handles clicks on played wild property cards to change color. */
@FunctionalInterface
public interface WildPropertyRecolorListener {
    void onWildPropertyRecolorRequested(WildpropertyCard wild, int ownerSeat);
}
