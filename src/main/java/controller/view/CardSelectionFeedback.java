package controller.view;

import model.card.Card;
import model.card.WildpropertyCard;
import model.card.RentCard;
import model.card.actionCard.ActionCard;
import model.card.actionCard.DoubleTheRent;

/** Builds user-facing hints when a hand card is selected. */
public final class CardSelectionFeedback {
    private CardSelectionFeedback() {
    }

    public static String messageFor(Card card) {
        return messageFor(card, false);
    }

    public static String messageForOnline(Card card) {
        return messageFor(card, true);
    }

    private static String messageFor(Card card, boolean online) {
        if (card instanceof WildpropertyCard wild) {
            return "Selected wild property [" + card.getName() + "]"
                    + (wild.isBankable()
                    ? " (can deposit to bank for " + wild.getBankValueM() + "M)"
                    : " (cannot deposit to bank)")
                    + ". Play card to choose color or deposit to bank";
        }
        if (card instanceof RentCard rentCard) {
            return "Selected rent card [" + card.getName() + "] (bank " + rentCard.getBankValueM()
                    + "M). Play card to collect rent or deposit to bank";
        }
        if (online && card instanceof DoubleTheRent) {
            return "Selected Double the Rent — pick a Rent card and collect double (uses 2 plays)";
        }
        if (card instanceof ActionCard actionCard) {
            return "Selected action card [" + card.getName() + "] (bank " + actionCard.getBankValueM()
                    + "M). Play card to choose: use effect or deposit to bank";
        }
        return "Selected: " + card.getName()
                + ". Double-click to play, or click Discard Selected Card to discard";
    }
}
