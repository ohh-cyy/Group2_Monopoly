package controller.gameplay;

import controller.dialog.GameDialogService;
import controller.session.LocalGameSession;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.Card;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;
import model.card.actionCard.DoubleTheRent;
import model.enums.Color;
import model.player.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles local card play flow: action effects, wild properties, and simple cards.
 */
public final class LocalCardPlayService {
    private final GameDialogService dialogs;
    private final ActionEffectResolver actionResolver;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    public LocalCardPlayService(GameDialogService dialogs,
                                  ActionEffectResolver actionResolver,
                                  Consumer<String> log,
                                  BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.actionResolver = actionResolver;
        this.log = log;
        this.status = status;
    }

    public Optional<ActionPlayChoice> promptActionCardChoice(ActionCard card) {
        return actionResolver.promptActionCardChoice(card);
    }

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

    public CardPlayOutcome playActionCard(LocalGameSession session, Player player, ActionCard actionCard) {
        GameEngine engine = session.getEngine();
        Optional<ActionPlayChoice> choice = promptActionCardChoice(actionCard);
        if (choice.isEmpty()) {
            return CardPlayOutcome.cancelled();
        }
        if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
            player.removeFromHand(actionCard);
            actionCard.depositToBank(player);
            log.accept(player.getName() + " deposit「" + actionCard.getName()
                    + "」into the bank（" + actionCard.getBankValueM() + "M）");
            status.accept("Already deposited in the bank " + actionCard.getBankValueM() + "M", false);
            return new CardPlayOutcome(ActionEffectResult.SUCCESS, true, false);
        }

        ActionEffectResult result = actionResolver.resolve(engine, player, actionCard);
        if (result == ActionEffectResult.CANCELLED) {
            status.accept("The card has been cancelled, the action card is kept in hand", false);
            return CardPlayOutcome.cancelled();
        }

        player.removeFromHand(actionCard);
        engine.getDiscardPile().addCard(actionCard);
        if (result == ActionEffectResult.SUCCESS) {
            log.accept(player.getName() + " use「" + actionCard.getName() + "」effect");
            status.accept("Effect has been successfully used: " + actionCard.getName(), false);
        } else if (result == ActionEffectResult.BLOCKED) {
            log.accept(player.getName() + " use「" + actionCard.getName() + "」but was blocked by Just Say No");
            status.accept("The effect was blocked by Just Say No", true);
        } else {
            log.accept(player.getName() + " Fail to use「" + actionCard.getName() + "」,the cards enter the discard pile");
            status.accept("The effect did not take effect (invalid target, etc.)", true);
        }

        boolean extraPlay = actionCard instanceof DoubleTheRent && result == ActionEffectResult.SUCCESS;
        return new CardPlayOutcome(result, false, extraPlay);
    }

    public CardPlayOutcome playWildPropertyCard(LocalGameSession session, Player player, WildpropertyCard wild) {
        GameEngine engine = session.getEngine();
        if (wild.isBankable()) {
            Optional<ActionPlayChoice> choice = promptWildPropertyChoice(wild);
            if (choice.isEmpty()) {
                return CardPlayOutcome.cancelled();
            }
            if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
                player.removeFromHand(wild);
                wild.depositToBank(player);
                log.accept(player.getName() + " deposited wild property [" + wild.getName()
                        + "] into bank (" + wild.getBankValueM() + "M)");
                status.accept("Wild card deposited to bank: " + wild.getBankValueM() + "M", false);
                return new CardPlayOutcome(ActionEffectResult.SUCCESS, true, false);
            }
        }

        List<Color> playableColors = wild.getAvailableColors().stream()
                .filter(color -> PropertyRules.canAddBillableProperty(player, color))
                .toList();
        if (playableColors.isEmpty()) {
            status.accept("All available colors are already complete. Deposit to bank if you can.", true);
            return new CardPlayOutcome(ActionEffectResult.FAILED, false, false);
        }

        Optional<Color> color = dialogs.showChoiceDialog(
                "Wild Property Color",
                wild.getName(),
                buildWildColorPrompt(wild),
                playableColors,
                c -> c + "  —  play as " + c + " property",
                c -> "-fx-background-color: " + dialogs.cssColorFor(c) + ";"
                        + "-fx-text-fill: " + dialogs.textColorFor(c) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
        if (color.isEmpty()) {
            status.accept("Cancelled, wild card kept in hand", false);
            return CardPlayOutcome.cancelled();
        }
        if (!PropertyRules.canAddBillableProperty(player, color.get())) {
            status.accept("This color set is already complete. You can only add House or Hotel.", true);
            return new CardPlayOutcome(ActionEffectResult.FAILED, false, false);
        }

        wild.setChosenColor(color.get());
        player.removeFromHand(wild);
        wild.use(player, engine);
        log.accept(player.getName() + " played wild property [" + wild.getName() + "] as " + color.get());
        status.accept("Wild property placed as " + color.get() + " in property area", false);
        return new CardPlayOutcome(ActionEffectResult.SUCCESS, false, false);
    }

    /** Routes a card to the correct play handler (Strategy-like dispatch, kept in one service). */
    public CardPlayOutcome play(LocalGameSession session, Player player, Card card) {
        if (card instanceof ActionCard actionCard) {
            return playActionCard(session, player, actionCard);
        }
        if (card instanceof WildpropertyCard wild) {
            return playWildPropertyCard(session, player, wild);
        }
        return playSimpleCard(session, player, card);
    }

    public CardPlayOutcome playSimpleCard(LocalGameSession session, Player player, Card played) {
        GameEngine engine = session.getEngine();
        if (played instanceof PropertyCard propertyCard
                && !PropertyRules.isSetImprovement(propertyCard)
                && propertyCard.getColor() != null
                && !PropertyRules.canAddBillableProperty(player, propertyCard.getColor())) {
            status.accept("This color set is already complete. You can only add House or Hotel.", true);
            return new CardPlayOutcome(ActionEffectResult.FAILED, false, false);
        }

        log.accept(player.getName() + " played: " + played.getName());
        played.use(player, engine);
        player.removeFromHand(played);
        return new CardPlayOutcome(ActionEffectResult.SUCCESS, false, false);
    }

    private String buildWildColorPrompt(WildpropertyCard wild) {
        if (wild.isBankable()) {
            return "Choose a color to play as property.\nDeposit to bank is always "
                    + wild.getBankValueM() + "M (not affected by color chosen).";
        }
        return "Choose a color to play as property.\nThis wild card cannot be deposited to bank.";
    }
}
