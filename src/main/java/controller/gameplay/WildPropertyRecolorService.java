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
 * Local-mode wild property recolor flow (click on board, uses one play).
 * <p>
 * Validates ownership and available colors via {@link engine.WildPropertyRules},
 * then applies the chosen color after player confirmation.
 */
public final class WildPropertyRecolorService {
    private final GameDialogService dialogs;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    /**
     * @param dialogs themed dialog factory for color selection
     * @param log     game log sink
     * @param status  status bar sink (message, isError)
     */
    public WildPropertyRecolorService(GameDialogService dialogs,
                                      Consumer<String> log,
                                      BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.log = log;
        this.status = status;
    }

    /**
     * Prompts for a new color and applies recolor on the player's board.
     *
     * @return {@code true} if recolor succeeded and one play should be recorded
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
