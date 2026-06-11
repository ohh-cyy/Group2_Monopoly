package controller.gameplay;

import controller.dialog.GameDialogService;
import controller.session.LocalGameSession;
import engine.GameEngine;
import engine.PropertyRules;
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
 * 本地出牌编排：对话框、效果解析与引擎变更。
 * <p>
 * 将每张 {@link model.card.Card} 路由到行动、万能地产或简单处理器，
 * 并向 UI 层返回 {@link CardPlayOutcome}。
 */
public final class LocalCardPlayService {
    private final GameDialogService dialogs;
    private final ActionEffectResolver actionResolver;
    private final StandardCardPlayPrompts prompts;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    /**
     * @param dialogs        主题对话框工厂
     * @param actionResolver 本地行动卡效果解析器
     * @param prompts        共享的存银行/使用效果提示
     * @param log            游戏日志输出
     * @param status         状态栏输出（消息, 是否错误）
     */
    public LocalCardPlayService(GameDialogService dialogs,
                                  ActionEffectResolver actionResolver,
                                  StandardCardPlayPrompts prompts,
                                  Consumer<String> log,
                                  BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.actionResolver = actionResolver;
        this.prompts = prompts;
        this.log = log;
        this.status = status;
    }

    /**
     * 使用默认 {@link StandardCardPlayPrompts} 实例的便捷构造器。
     */
    public LocalCardPlayService(GameDialogService dialogs,
                                  ActionEffectResolver actionResolver,
                                  Consumer<String> log,
                                  BiConsumer<String, Boolean> status) {
        this(dialogs, actionResolver, new StandardCardPlayPrompts(dialogs), log, status);
    }

    /** 委托给 {@link StandardCardPlayPrompts}。 */
    public Optional<ActionPlayChoice> promptActionCardChoice(ActionCard card) {
        return prompts.promptActionCardChoice(card);
    }

    /** 委托给 {@link StandardCardPlayPrompts}。 */
    public Optional<ActionPlayChoice> promptWildPropertyChoice(WildpropertyCard wild) {
        return prompts.promptWildPropertyChoice(wild);
    }

    /**
     * 完整行动卡流程：存银行/使用效果选择，再经 {@link ActionEffectResolver} 解析。
     */
    public CardPlayOutcome playActionCard(LocalGameSession session, Player player, ActionCard actionCard) {
        GameEngine engine = session.getEngine();
        Optional<ActionPlayChoice> choice = promptActionCardChoice(actionCard);
        if (choice.isEmpty()) {
            return CardPlayOutcome.cancelled();
        }
        if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
            player.removeFromHand(actionCard);
            actionCard.depositToBank(player);
            log.accept(player.getName() + " banked " + actionCard.getName()
                    + " (" + actionCard.getBankValueM() + "M)");
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
            log.accept(player.getName() + " used effect: " + actionCard.getName());
            status.accept("Effect has been successfully used: " + actionCard.getName(), false);
        } else if (result == ActionEffectResult.BLOCKED) {
            log.accept(player.getName() + " used " + actionCard.getName() + " but was blocked by Just Say No");
            status.accept("The effect was blocked by Just Say No", false);
        } else {
            log.accept(player.getName() + " failed to use " + actionCard.getName() + "; card discarded");
            status.accept("The effect did not take effect (invalid target, etc.)", true);
        }

        boolean extraPlay = actionCard instanceof DoubleTheRent && result == ActionEffectResult.SUCCESS;
        return new CardPlayOutcome(result, false, extraPlay);
    }

    /** 万能卡：可选择存入银行或作为地产打出。 */
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
        log.accept(player.getName() + " played: " + color.get().logKey());
        status.accept("Wild property placed as " + color.get()
                + ". Click it on the board to change color (uses 1 play)", false);
        return new CardPlayOutcome(ActionEffectResult.SUCCESS, false, false);
    }

    /**
     * 将卡牌路由到正确的出牌处理器（行动、万能或简单）。
     *
     * @return 描述成功、存银行、额外出牌消耗或取消的结果
     */
    public CardPlayOutcome play(LocalGameSession session, Player player, Card card) {
        if (card instanceof ActionCard actionCard) {
            return playActionCard(session, player, actionCard);
        }
        if (card instanceof WildpropertyCard wild) {
            return playWildPropertyCard(session, player, wild);
        }
        return playSimpleCard(session, player, card);
    }

    /** 地产、金钱等卡牌，直接通过 {@link model.card.Card#use} 打出。 */
    public CardPlayOutcome playSimpleCard(LocalGameSession session, Player player, Card played) {
        GameEngine engine = session.getEngine();
        if (played instanceof PropertyCard propertyCard
                && !PropertyRules.isSetImprovement(propertyCard)
                && propertyCard.getColor() != null
                && !PropertyRules.canAddBillableProperty(player, propertyCard.getColor())) {
            status.accept("This color set is already complete. You can only add House or Hotel.", true);
            return new CardPlayOutcome(ActionEffectResult.FAILED, false, false);
        }

        log.accept(player.getName() + " played: " + propertyPlayDetail(played));
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

    private static String propertyPlayDetail(Card card) {
        if (card instanceof PropertyCard property
                && !PropertyRules.isSetImprovement(property)
                && property.getColor() != null) {
            return property.getColor().logKey();
        }
        return card.getName();
    }
}
