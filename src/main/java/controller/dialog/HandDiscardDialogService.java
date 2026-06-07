package controller.dialog;

import engine.GameEngine;
import javafx.scene.control.Dialog;
import model.card.Card;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Prompts for voluntary discards and end-of-turn hand limit (max 7 cards). */
public final class HandDiscardDialogService {
    private HandDiscardDialogService() {
    }

    public static Optional<Card> promptDiscardOne(
            Function<DialogHelper, Optional<Card>> showChoiceDialog,
            List<Card> hand,
            int excess,
            boolean endingTurn) {
        String prompt = endingTurn
                ? "请选择要丢弃的卡牌（还需丢弃 " + excess + " 张才能结束回合）"
                : "请选择要丢弃的卡牌（当前手牌 " + hand.size() + " 张，上限 "
                        + GameEngine.MAX_HAND_SIZE + " 张）";
        return showChoiceDialog.apply(new DialogHelper(
                "手牌上限",
                "手牌超过 " + GameEngine.MAX_HAND_SIZE + " 张",
                prompt,
                hand));
    }

    public record DialogHelper(String title, String header, String prompt, List<Card> hand) {
    }
}
