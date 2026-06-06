package controller.gameplay;

import controller.dialog.GameDialogService;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.Card;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.actionCard.ActionCard;
import model.card.actionCard.DealBreaker;
import model.card.actionCard.DebtCollector;
import model.card.actionCard.DoubleTheRent;
import model.card.actionCard.ForcedDeal;
import model.card.actionCard.Hotel;
import model.card.actionCard.House;
import model.card.actionCard.JustSayNo;
import model.card.actionCard.MyBirthday;
import model.card.actionCard.SlyDeal;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Keeps action-card rules out of the UI controller.
 */
public class ActionEffectResolver {
    private final GameDialogService dialogs;
    private final PaymentService payments;
    private final JustSayNoService justSayNo;
    private final Consumer<String> log;
    private final BiConsumer<String, Boolean> status;

    public ActionEffectResolver(GameDialogService dialogs, PaymentService payments, JustSayNoService justSayNo,
                                Consumer<String> log, BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.payments = payments;
        this.justSayNo = justSayNo;
        this.log = log;
        this.status = status;
    }

    public Optional<ActionPlayChoice> promptActionCardChoice(ActionCard card) {
        ButtonType useBtn = new ButtonType("Use Effect");
        ButtonType bankBtn = new ButtonType("Bank (" + card.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = dialogs.showButtonDialog(
                "Action Card",
                card.getName() + " - bank value " + card.getBankValueM() + "M",
                card.getDescription() + "\n\nUse the effect or bank this card?",
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

    public ActionEffectResult resolve(GameEngine gameEngine, Player player, ActionCard actionCard) {
        if (actionCard instanceof JustSayNo) {
            status.accept("Just Say No can only be used in response to an action played against you.", true);
            return ActionEffectResult.CANCELLED;
        }
        if (actionCard instanceof DealBreaker dealBreaker) {
            return resolveDealBreaker(gameEngine, player, dealBreaker);
        }
        if (actionCard instanceof DebtCollector debtCollector) {
            return resolveDebtCollector(gameEngine, player, debtCollector);
        }
        if (actionCard instanceof MyBirthday myBirthday) {
            return resolveMyBirthday(gameEngine, player, myBirthday);
        }
        if (actionCard instanceof DoubleTheRent doubleRent) {
            return resolveDoubleTheRent(gameEngine, player, doubleRent);
        }
        if (actionCard instanceof SlyDeal slyDeal) {
            return resolveSlyDeal(gameEngine, player, slyDeal);
        }
        if (actionCard instanceof ForcedDeal forcedDeal) {
            return resolveForcedDeal(gameEngine, player, forcedDeal);
        }
        if (actionCard instanceof House house) {
            return resolveHouse(player, house);
        }
        if (actionCard instanceof Hotel hotel) {
            return resolveHotel(player, hotel);
        }
        if (actionCard instanceof RentCard rentCard) {
            return resolveRentCard(gameEngine, player, rentCard);
        }

        actionCard.use(player, gameEngine);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveHouse(Player player, House house) {
        Optional<Color> color = promptSelectOwnCompleteSet(player, "Choose a complete set for House");
        if (color.isEmpty()) {
            return hasAnyCompleteSet(player)
                    ? ActionEffectResult.CANCELLED
                    : ActionEffectResult.FAILED;
        }
        return house.addHouseToSet(player, color.get())
                ? ActionEffectResult.SUCCESS
                : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveHotel(Player player, Hotel hotel) {
        Optional<Color> color = promptSelectOwnCompleteSet(player, "Choose a complete set for Hotel");
        if (color.isEmpty()) {
            return hasAnyCompleteSet(player)
                    ? ActionEffectResult.CANCELLED
                    : ActionEffectResult.FAILED;
        }
        return hotel.addHotelToSet(player, color.get())
                ? ActionEffectResult.SUCCESS
                : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveRentCard(GameEngine gameEngine, Player player, RentCard rentCard) {
        List<Color> options = rentCard.getChargeableColors(player);
        if (options.isEmpty()) {
            status.accept("You have no property color that can collect rent.", true);
            return ActionEffectResult.FAILED;
        }

        Optional<Color> selectedColor = chooseRentColor(player, rentCard, options);
        if (selectedColor.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }

        Color chargeColor = selectedColor.get();
        int rent = rentCard.calculateRent(player, chargeColor);
        if (rent <= 0) {
            status.accept("This color has no properties, so rent is 0.", true);
            return ActionEffectResult.FAILED;
        }

        boolean doubled = gameEngine.isRentDoubled();
        int rentPerPlayer = doubled ? rent * 2 : rent;
        int total = collectRentFromOpponents(gameEngine, player, rentCard, chargeColor, rentPerPlayer);
        if (doubled) {
            gameEngine.setRentDoubled(false);
        }

        String rentNote = doubled ? " (double rent)" : "";
        log.accept(player.getName() + " used \"" + rentCard.getName() + "\" to charge " + chargeColor
                + " rent: " + rentPerPlayer + "M per player" + rentNote
                + ", total collected " + total + "M");
        status.accept("Collected " + chargeColor + " rent from all players" + rentNote
                + " (total " + total + "M)", false);
        return ActionEffectResult.SUCCESS;
    }

    private Optional<Color> chooseRentColor(Player player, RentCard rentCard, List<Color> options) {
        if (options.size() == 1) {
            return Optional.of(options.get(0));
        }
        return promptSelectRentColor(player, rentCard, options);
    }

    // Rent is collected one payer at a time so each player can respond and choose assets.
    private int collectRentFromOpponents(GameEngine gameEngine, Player collector, RentCard rentCard,
                                         Color chargeColor, int rentPerPlayer) {
        int total = 0;
        for (Player other : gameEngine.getPlayers()) {
            if (other.equals(collector)) {
                continue;
            }
            if (justSayNo.respond(other, collector,
                    rentCard.getName() + " (" + chargeColor + " rent)", gameEngine)) {
                continue;
            }
            total += payments.collectPaymentByChoice(collector, other, rentPerPlayer,
                    rentCard.getName() + " - " + chargeColor + " rent");
        }
        return total;
    }

    private ActionEffectResult resolveDebtCollector(GameEngine gameEngine, Player player, DebtCollector debtCollector) {
        Optional<Player> target = promptSelectOpponent(gameEngine, player,
                "Debt Collector: choose a player to charge 5M");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (justSayNo.respond(target.get(), player, "Debt Collector (5M)", gameEngine)) {
            return ActionEffectResult.BLOCKED;
        }
        int paid = payments.collectPaymentByChoice(player, target.get(), DebtCollector.DEBT_AMOUNT, "Debt Collector");
        log.accept(player.getName() + " collected " + paid + "M from " + target.get().getName());
        return paid > 0 ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveMyBirthday(GameEngine gameEngine, Player player, MyBirthday myBirthday) {
        int total = 0;
        boolean anyBlocked = false;
        for (Player other : gameEngine.getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            if (justSayNo.respond(other, player,
                    "My Birthday (" + MyBirthday.GIFT_AMOUNT + "M)", gameEngine)) {
                anyBlocked = true;
                continue;
            }
            total += payments.collectPaymentByChoice(player, other, MyBirthday.GIFT_AMOUNT, "My Birthday");
        }
        log.accept(player.getName() + " used My Birthday and collected " + total
                + "M. Each opponent should pay " + MyBirthday.GIFT_AMOUNT + "M if they can.");
        status.accept("All opponents paid a total of " + total + "M", false);
        return total > 0 || anyBlocked ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveDoubleTheRent(GameEngine gameEngine, Player player, DoubleTheRent doubleRent) {
        if (gameEngine.getRemainingPlays() < 2) {
            status.accept("You need 2 plays remaining to use Double the Rent with a Rent card.", true);
            return ActionEffectResult.FAILED;
        }

        List<RentCard> rentOptions = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (card == doubleRent || !(card instanceof RentCard rent)) {
                continue;
            }
            if (rent.canPlay(player)) {
                rentOptions.add(rent);
            }
        }
        if (rentOptions.isEmpty()) {
            status.accept("No playable Rent card in your hand.", true);
            return ActionEffectResult.FAILED;
        }

        Optional<RentCard> rentChoice = dialogs.showChoiceDialog(
                "Choose Rent Card",
                "Double the Rent",
                "Select a Rent card to play at double value (uses 2 plays):",
                rentOptions,
                rent -> rent.getName() + " (bank " + rent.getBankValueM() + "M)",
                rent -> null);
        if (rentChoice.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        RentCard rentCard = rentChoice.get();

        List<Color> colorOptions = rentCard.getChargeableColors(player);
        Optional<Color> selectedColor;
        if (rentCard.isAllColors() && colorOptions.isEmpty()) {
            selectedColor = promptSelectRentColor(player, rentCard, Arrays.asList(Color.values()));
        } else {
            selectedColor = chooseRentColor(player, rentCard, colorOptions);
        }
        if (selectedColor.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }

        Color chargeColor = selectedColor.get();
        int rent = rentCard.calculateRent(player, chargeColor);
        if (rent <= 0) {
            status.accept("This color has no properties, so rent is 0.", true);
            return ActionEffectResult.FAILED;
        }

        int rentPerPlayer = rent * 2;
        int total = collectRentFromOpponents(gameEngine, player, rentCard, chargeColor, rentPerPlayer);
        player.removeFromHand(rentCard);
        gameEngine.getDiscardPile().addCard(rentCard);

        log.accept(player.getName() + " used Double the Rent with \"" + rentCard.getName() + "\" to charge "
                + chargeColor + " rent: " + rentPerPlayer + "M per player (double rent), total collected "
                + total + "M");
        status.accept("Double rent collected " + chargeColor + " from all players (total " + total + "M)", false);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveSlyDeal(GameEngine gameEngine, Player player, SlyDeal slyDeal) {
        Optional<Player> target = promptSelectOpponent(gameEngine, player,
                "Sly Deal: choose a player to steal from");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (justSayNo.respond(target.get(), player, "Sly Deal (steal one property)", gameEngine)) {
            return ActionEffectResult.BLOCKED;
        }
        List<PropertyCard> stealable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (stealable.isEmpty()) {
            status.accept(target.get().getName()
                    + " has no stealable properties. Complete sets are protected.", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> property = promptSelectProperty(stealable,
                "Choose a property to steal", target.get().getName() + "'s stealable properties");
        if (property.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (slyDeal.stealProperty(player, target.get(), property.get())) {
            log.accept(player.getName() + " stole " + property.get().getName()
                    + " from " + target.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveForcedDeal(GameEngine gameEngine, Player player, ForcedDeal forcedDeal) {
        Optional<Player> target = promptSelectOpponent(gameEngine, player,
                "Forced Deal: choose a player to trade with");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (justSayNo.respond(target.get(), player, "Forced Deal (swap properties)", gameEngine)) {
            return ActionEffectResult.BLOCKED;
        }
        List<PropertyCard> myProps = player.getAllProperties();
        if (myProps.isEmpty()) {
            status.accept("You have no properties to trade.", true);
            return ActionEffectResult.FAILED;
        }
        List<PropertyCard> theirSwappable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (theirSwappable.isEmpty()) {
            status.accept(target.get().getName()
                    + " has no swappable properties. Complete sets are protected.", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> mine = promptSelectProperty(myProps,
                "Choose your property to give", "Your properties");
        if (mine.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        Optional<PropertyCard> theirs = promptSelectProperty(theirSwappable,
                "Choose their property to take", target.get().getName() + "'s swappable properties");
        if (theirs.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (forcedDeal.swapProperties(player, mine.get(), target.get(), theirs.get())) {
            log.accept(player.getName() + " swapped properties with " + target.get().getName() + ": "
                    + mine.get().getName() + " <-> " + theirs.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveDealBreaker(GameEngine gameEngine, Player player, DealBreaker dealBreaker) {
        Optional<Player> target = promptSelectOpponentWithCompleteSets(gameEngine, player);
        if (target.isEmpty()) {
            status.accept("No player has a complete set that can be stolen.", true);
            return ActionEffectResult.CANCELLED;
        }
        Player opponent = target.get();
        Optional<Color> color = promptSelectCompleteSetOnPlayer(opponent);
        if (color.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (justSayNo.respond(opponent, player, "Deal Breaker (steal a complete set)", gameEngine)) {
            return ActionEffectResult.BLOCKED;
        }
        if (!dealBreaker.useOnTarget(player, opponent, color.get())) {
            status.accept("Steal failed.", true);
            return ActionEffectResult.FAILED;
        }
        log.accept(player.getName() + " stole the full " + color.get()
                + " set from " + opponent.getName());
        return ActionEffectResult.SUCCESS;
    }

    private Optional<PropertyCard> promptSelectProperty(List<PropertyCard> properties,
                                                        String title, String header) {
        return dialogs.showChoiceDialog(title, header, "Choose one property:", properties,
                p -> p.getName() + " (" + p.getColor() + ", " + p.getPrice() + "M)",
                p -> "-fx-border-color: " + dialogs.cssColorFor(
                        p.getColor() == null ? Color.BROWN : p.getColor()) + ";");
    }

    private Optional<Color> promptSelectRentColor(Player player, RentCard rentCard, List<Color> options) {
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return dialogs.showChoiceDialog("Choose Rent Color", "Choose which property set collects rent",
                "Color (count -> rent):", options,
                color -> color + "  -  " + rentCard.countProperties(player, color) + " card(s) -> "
                        + rentCard.calculateRent(player, color) + "M",
                color -> "-fx-background-color: " + dialogs.cssColorFor(color)
                        + "; -fx-text-fill: " + dialogs.textColorFor(color) + ";");
    }

    private Optional<Player> promptSelectOpponentWithCompleteSets(GameEngine gameEngine, Player current) {
        List<Player> valid = new ArrayList<>();
        for (Player player : gameEngine.getPlayers()) {
            if (!player.equals(current) && hasAnyCompleteSet(player)) {
                valid.add(player);
            }
        }
        return dialogs.showChoiceDialog("Deal Breaker", "Choose a player with a complete set",
                "Only players with complete sets are shown:", valid, Player::getName, player -> null);
    }

    private Optional<Player> promptSelectOpponent(GameEngine gameEngine, Player current, String title) {
        List<Player> opponents = new ArrayList<>();
        for (Player player : gameEngine.getPlayers()) {
            if (!player.equals(current)) {
                opponents.add(player);
            }
        }
        return dialogs.showChoiceDialog(title, title, "Choose a player:", opponents, Player::getName, player -> null);
    }

    private Optional<Color> promptSelectCompleteSetOnPlayer(Player target) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (target.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        return dialogs.showColorChoiceDialog("Choose Set", target.getName() + "'s complete sets",
                "Which color set do you want to take?", options);
    }

    private Optional<Color> promptSelectOwnCompleteSet(Player player, String title) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (player.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        return dialogs.showColorChoiceDialog(title, title, "Choose a color set:", options);
    }

    private boolean hasAnyCompleteSet(Player player) {
        for (Color color : Color.values()) {
            if (player.hasCompleteSet(color)) {
                return true;
            }
        }
        return false;
    }
}
