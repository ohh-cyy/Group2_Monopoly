package controller.gameplay;

import controller.dialog.GameDialogService;
import engine.WildPropertyRules;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 本地模式的万能地产改色流程（点击棋盘，消耗 1 次出牌）。
 * <p>
 * 通过 {@link engine.WildPropertyRules} 校验所有权与可用颜色，
 * 玩家确认后应用所选颜色。
 */
public final class WildPropertyRecolorService {
    private final GameDialogService dialogs;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    /**
     * @param dialogs 用于颜色选择的主题对话框工厂
     * @param log     游戏日志输出
     * @param status  状态栏输出（消息, 是否错误）
     */
    public WildPropertyRecolorService(GameDialogService dialogs,
                                      Consumer<String> log,
                                      BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.log = log;
        this.status = status;
    }

    /**
     * 提示选择新颜色并在玩家棋盘上应用改色。
     *
     * @return 改色成功且应记录 1 次出牌时为 {@code true}
     */
    public boolean attemptRecolor(Player player, WildpropertyCard wild) {
        WildpropertyCard owned = WildPropertyRules.findOwnedWild(player, wild);
        if (owned == null) {
            status.accept("Wild property not found on your board", true);
            return false;
        }

        List<Color> options = WildPropertyRules.getRecolorOptions(player, owned);
        if (options.isEmpty()) {
            status.accept("No alternate colors available right now", true);
            return false;
        }

        Color current = owned.getChosenColor();
        Optional<Color> chosen = dialogs.showChoiceDialog(
                "Change Wild Property Color",
                owned.getName(),
                "Current color: " + current + "\nChoose a new color (uses 1 play):",
                options,
                color -> color + "  —  change to " + color,
                color -> "-fx-background-color: " + dialogs.cssColorFor(color) + ";"
                        + "-fx-text-fill: " + dialogs.textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
        if (chosen.isEmpty()) {
            return false;
        }

        if (!WildPropertyRules.recolor(player, owned, chosen.get())) {
            status.accept("Cannot change to that color", true);
            return false;
        }

        log.accept(player.getName() + " recolored wild: "
                + current.logKey() + " → " + chosen.get().logKey());
        status.accept("Wild property changed to " + chosen.get() + " (1 play used)", false);
        return true;
    }
}
