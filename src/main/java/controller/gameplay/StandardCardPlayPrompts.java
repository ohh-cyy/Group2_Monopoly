package controller.gameplay;

import controller.dialog.GameDialogService;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;

import java.util.Optional;

/**
 * Shared "use effect vs deposit to bank" dialogs for action and wild property cards.
 * <p>
 * Used by {@link LocalCardPlayService}, {@link ActionEffectResolver}, and
 * {@link OnlineCardPlayService} to keep prompt copy consistent across modes.
 */
public final class StandardCardPlayPrompts {
    private final GameDialogService dialogs;

    /**
     * @param dialogs themed dialog factory used to render choice buttons
     */
    public StandardCardPlayPrompts(GameDialogService dialogs) {
        this.dialogs = dialogs;
    }

    /**
     * Asks whether to use an action card's effect or bank it for its M value.
     */
    public Optional<ActionPlayChoice> promptActionCardChoice(ActionCard card) {
        ButtonType useBtn = new ButtonType("Use Effect");
        ButtonType bankBtn = new ButtonType("Deposit to Bank (" + card.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = dialogs.showButtonDialog(
                "Action Card",
                card.getName() + " — Bank value " + card.getBankValueM() + "M",
                card.getDescription() + "\n\nChoose 'Use Effect' or 'Deposit to Bank'?",
                useBtn, bankBtn, cancelBtn);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (result.get() == useBtn) {
            return Optional.of(ActionPlayChoice.USE_EFFECT);
        }
        if (result.get() == bankBtn) {
            return Optional.of(ActionPlayChoice.DEPOSIT_BANK);
        }
        return Optional.empty();
    }

    /**
     * Asks whether to play a wild property as property (choose color) or deposit to bank.
     */
    public Optional<ActionPlayChoice> promptWildPropertyChoice(WildpropertyCard wild) {
        ButtonType useBtn = new ButtonType("Play as Property");
        ButtonType bankBtn = new ButtonType("Deposit to Bank (" + wild.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = dialogs.showButtonDialog(
                "Wild Property Card",
                wild.getName() + " — Bank value " + wild.getBankValueM() + "M",
                "Play as property (choose a color), or deposit to bank for "
                        + wild.getBankValueM() + "M?",
                useBtn, bankBtn, cancelBtn);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (result.get() == useBtn) {
            return Optional.of(ActionPlayChoice.USE_EFFECT);
        }
        if (result.get() == bankBtn) {
            return Optional.of(ActionPlayChoice.DEPOSIT_BANK);
        }
        return Optional.empty();
    }
}
