package controller.gameplay;

import controller.dialog.GameDialogService;
import engine.GameEngine;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.Card;
import model.card.actionCard.JustSayNo;
import model.player.Player;
import ui.GameAudio;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 本地模式中攻击方与防守方之间的 Just Say No 响应链。
 * <p>
 * 双方可交替打出 {@link model.card.actionCard.JustSayNo} 时轮流提示；
 * 每打出一张即切换原行动是否被阻挡。
 */
public class JustSayNoService {
    private final GameDialogService dialogs;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    /**
     * @param dialogs 用于阻挡/允许提示的主题对话框工厂
     * @param log     游戏日志输出
     * @param status  状态栏输出（消息, 是否错误）
     */
    public JustSayNoService(GameDialogService dialogs, Consumer<String> log, BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.log = log;
        this.status = status;
    }

    /**
     * 允许双方以 Just Say No 回应。每多打出一张会抵消上一张，
     * 因此最终阻挡状态每打出一张即翻转。
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
        GameAudio.play(GameAudio.Cue.ERROR);
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
