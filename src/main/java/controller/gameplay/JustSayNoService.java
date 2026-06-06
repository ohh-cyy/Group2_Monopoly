package controller.gameplay;

import controller.dialog.GameDialogService;
import engine.GameEngine;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.Card;
import model.card.actionCard.JustSayNo;
import model.player.Player;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles the Just Say No response chain between two players.
 */
public class JustSayNoService {
    private final GameDialogService dialogs;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    public JustSayNoService(GameDialogService dialogs, Consumer<String> log, BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.log = log;
        this.status = status;
    }

    /**
     * Lets both sides answer with Just Say No. Each extra Just Say No cancels
     * the previous one, so the final blocked state flips every time one is played.
     */
    public boolean respond(Player defender, Player attacker, String actionName, GameEngine gameEngine) {
        Player responder = defender;
        Player opponent = attacker;
        boolean blocked = false;
        int responseDepth = 0;

        while (true) {
            JustSayNo justSayNo = findJustSayNoInHand(responder);
            if (justSayNo == null || !promptPlayJustSayNo(responder, opponent, actionName, responseDepth)) {
                return blocked;
            }

            String justSayNoId = justSayNo.getInstanceId();
            if (!responder.removeFromHandById(justSayNoId)) {
                return blocked;
            }
            gameEngine.getDiscardPile().addCard(justSayNo);
            blocked = !blocked;
            logJustSayNoResponse(responder, actionName, responseDepth, blocked);

            Player nextResponder = opponent;
            opponent = responder;
            responder = nextResponder;
            responseDepth++;
        }
    }

    private boolean promptPlayJustSayNo(Player responder, Player opponent, String actionName, int responseDepth) {
        ButtonType noBtn = new ButtonType("Play Just Say No");
        ButtonType allowBtn = new ButtonType("Allow Effect");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        String header = responseDepth == 0
                ? responder.getName() + ": block " + opponent.getName() + "'s action?"
                : responder.getName() + ": counter the previous Just Say No?";
        String content = responseDepth == 0
                ? actionName + "\n\nPlaying Just Say No cancels this effect."
                : actionName + "\n\nPlaying Just Say No cancels the previous Just Say No.";
        Optional<ButtonType> choice = dialogs.showButtonDialog(
                "Just Say No", header, content, noBtn, allowBtn, cancelBtn);
        return choice.isPresent() && choice.get() == noBtn;
    }

    private void logJustSayNoResponse(Player responder, String actionName, int responseDepth, boolean blocked) {
        if (responseDepth == 0) {
            log.accept(responder.getName() + " played Just Say No and cancelled \"" + actionName + "\"");
        } else {
            log.accept(responder.getName() + " played Just Say No and countered the previous Just Say No");
        }
        status.accept(blocked
                ? "Just Say No is currently blocking the action."
                : "The previous Just Say No was cancelled. The action continues.", false);
    }

    private JustSayNo findJustSayNoInHand(Player player) {
        for (Card card : player.getHand()) {
            if (card instanceof JustSayNo js) {
                return js;
            }
        }
        return null;
    }
}
