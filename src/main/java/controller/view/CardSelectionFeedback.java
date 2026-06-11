package controller.view;

import model.card.Card;
import model.card.WildpropertyCard;
import model.card.RentCard;
import model.card.actionCard.ActionCard;
import model.card.actionCard.DoubleTheRent;

/**
 * 玩家选中手牌时构建状态栏提示。
 * <p>
 * 本地与联机模式的文案略有差异（如 Double the Rent 表述）。
 */
public final class CardSelectionFeedback {
    /** 工具类，禁止实例化。 */
    private CardSelectionFeedback() {
    }

    /** 本地热座模式的提示文本。 */
    public static String messageFor(Card card) {
        return messageFor(card, false);
    }

    /** 联机模式的提示文本（含网络特有行动说明）。 */
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
