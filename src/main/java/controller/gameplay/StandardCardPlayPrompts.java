package controller.gameplay;

import controller.dialog.GameDialogService;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;

import java.util.Optional;

/**
 * 行动卡与万能地产卡共用的「使用效果 vs 存银行」对话框。
 * <p>
 * 由 {@link LocalCardPlayService}、{@link ActionEffectResolver} 与
 * {@link OnlineCardPlayService} 使用，以保持各模式提示文案一致。
 */
public final class StandardCardPlayPrompts {
    private final GameDialogService dialogs;

    /**
     * @param dialogs 用于渲染选项按钮的主题对话框工厂
     */
    public StandardCardPlayPrompts(GameDialogService dialogs) {
        this.dialogs = dialogs;
    }

    /**
     * 询问使用行动卡效果还是按 M 值存入银行。
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
     * 询问将万能地产作为地产打出（选颜色）还是存入银行。
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
