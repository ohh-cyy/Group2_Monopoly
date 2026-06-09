package controller.gameplay;

import controller.dialog.GameDialogService;
import engine.PropertyRules;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.card.Card;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;
import model.card.actionCard.DealBreaker;
import model.card.actionCard.DebtCollector;
import model.card.actionCard.DoubleTheRent;
import model.card.actionCard.ForcedDeal;
import model.card.actionCard.Hotel;
import model.card.actionCard.House;
import model.card.actionCard.SlyDeal;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.protocol.ClientMessage;
import network.protocol.GameStateDto;
import network.protocol.PlayerViewDto;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Builds {@link ClientMessage} payloads for online card play, including dialogs for targets and colors.
 */
public final class OnlineCardPlayService {
    private final GameDialogService dialogs;
    private final StandardCardPlayPrompts prompts;
    private final BiConsumer<String, Boolean> status;

    private GameStateDto state;
    private int localSeat;
    private List<Card> myHand;

    public OnlineCardPlayService(GameDialogService dialogs,
                                 StandardCardPlayPrompts prompts,
                                 BiConsumer<String, Boolean> status) {
        this.dialogs = dialogs;
        this.prompts = prompts;
        this.status = status;
    }

    public OnlineCardPlayService(GameDialogService dialogs, BiConsumer<String, Boolean> status) {
        this(dialogs, new StandardCardPlayPrompts(dialogs), status);
    }

    public Optional<ClientMessage> buildPlayMessage(GameStateDto state, int localSeat, List<Card> myHand, Card card) {
        this.state = state;
        this.localSeat = localSeat;
        this.myHand = myHand;

        ClientMessage msg = new ClientMessage();
        msg.cardId = card.getInstanceId();

        if (card instanceof WildpropertyCard wild) {
            return buildWildMessage(wild, msg);
        }
        if (card instanceof ActionCard action) {
            return buildActionMessage(action, msg);
        }
        return Optional.of(msg);
    }

    private Optional<ClientMessage> buildWildMessage(WildpropertyCard wild, ClientMessage msg) {
        if (wild.isBankable()) {
            Optional<ActionPlayChoice> choice = prompts.promptWildPropertyChoice(wild);
            if (choice.isEmpty()) {
                return Optional.empty();
            }
            if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
                msg.mode = "BANK";
                return Optional.of(msg);
            }
        }
        List<Color> colors = wild.getAvailableColors();
        if (colors.isEmpty()) {
            status.accept("No color available", true);
            return Optional.empty();
        }
        Player localView = playerViewFromSeat(localSeat);
        List<Color> playableColors = colors.stream()
                .filter(color -> PropertyRules.canAddBillableProperty(localView, color))
                .toList();
        if (playableColors.isEmpty()) {
            status.accept("All available colors are already complete. Deposit to bank if you can.", true);
            return Optional.empty();
        }
        Optional<Color> color = promptSelectWildColor(wild, playableColors);
        if (color.isEmpty()) {
            status.accept("Cancelled, wild card kept in hand", false);
            return Optional.empty();
        }
        msg.mode = "PROPERTY";
        msg.color = color.get().name();
        return Optional.of(msg);
    }

    private Optional<ClientMessage> buildActionMessage(ActionCard action, ClientMessage msg) {
        Optional<ActionPlayChoice> choice = prompts.promptActionCardChoice(action);
        if (choice.isEmpty()) {
            return Optional.empty();
        }
        if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
            msg.mode = "BANK";
            return Optional.of(msg);
        }

        msg.mode = "EFFECT";
        if (!fillActionEffectMessage(action, msg)) {
            return Optional.empty();
        }
        return Optional.of(msg);
    }

    private boolean fillActionEffectMessage(ActionCard action, ClientMessage msg) {
        if (action instanceof RentCard rentCard) {
            Optional<Color> color = promptRentColor(rentCard);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof DebtCollector) {
            Optional<Integer> target = promptOpponentSeat("Debt Collector: Select player to collect 5M from");
            if (target.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            return true;
        }
        if (action instanceof House) {
            List<Color> options = getHouseEligibleColors();
            if (options.isEmpty()) {
                status.accept("No complete set available for a House", true);
                return false;
            }
            Optional<Color> color = dialogs.showColorChoiceDialog(
                    "Select a complete set to add House",
                    "Select a complete set to add House",
                    "Select color set:", options);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof Hotel) {
            List<Color> options = getHotelEligibleColors();
            if (options.isEmpty()) {
                status.accept("Need a complete set with a House before adding a Hotel", true);
                return false;
            }
            Optional<Color> color = dialogs.showColorChoiceDialog(
                    "Select a complete set to add Hotel",
                    "Select a complete set to add Hotel",
                    "Select color set:", options);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof SlyDeal) {
            Optional<Integer> target = promptOpponentSeat("Sly Deal: Select player to steal property from");
            if (target.isEmpty()) {
                return false;
            }
            List<PropertyCard> stealable = getStealablePropertiesForSeat(target.get());
            if (stealable.isEmpty()) {
                status.accept(playerNameAt(target.get()) + " has no stealable properties", true);
                return false;
            }
            Optional<PropertyCard> property = promptSelectProperty(
                    stealable,
                    "Select property to steal",
                    playerNameAt(target.get()) + "'s stealable properties (not in complete sets)");
            if (property.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.targetCardId = property.get().getInstanceId();
            return true;
        }
        if (action instanceof ForcedDeal) {
            Optional<Integer> target = promptOpponentSeat("Forced Deal: Select player to exchange properties with");
            if (target.isEmpty()) {
                return false;
            }
            List<PropertyCard> myProps = getStealablePropertiesForSeat(localSeat);
            if (myProps.isEmpty()) {
                status.accept("You have no exchangeable properties. Complete sets are protected.", true);
                return false;
            }
            List<PropertyCard> theirProps = getStealablePropertiesForSeat(target.get());
            if (theirProps.isEmpty()) {
                status.accept(playerNameAt(target.get()) + " has no exchangeable properties", true);
                return false;
            }
            Optional<PropertyCard> mine = promptSelectProperty(
                    myProps, "Select your property to exchange", "Your exchangeable properties");
            if (mine.isEmpty()) {
                return false;
            }
            Optional<PropertyCard> theirs = promptSelectProperty(
                    theirProps,
                    "Select opponent's property to exchange",
                    playerNameAt(target.get()) + "'s exchangeable properties");
            if (theirs.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.targetCardId = mine.get().getInstanceId();
            msg.secondCardId = theirs.get().getInstanceId();
            return true;
        }
        if (action instanceof DealBreaker) {
            Optional<Integer> target = promptOpponentWithCompleteSets();
            if (target.isEmpty()) {
                status.accept("No player currently has a complete property set to steal", true);
                return false;
            }
            List<Color> completeSets = getCompleteSetColorsForSeat(target.get());
            Optional<Color> color = dialogs.showColorChoiceDialog(
                    "Select Set",
                    playerNameAt(target.get()) + "'s complete sets",
                    "Which color to steal?", completeSets);
            if (color.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof DoubleTheRent doubleRent) {
            return fillDoubleRentMessage(doubleRent, msg);
        }
        return true;
    }

    private boolean fillDoubleRentMessage(DoubleTheRent doubleRent, ClientMessage msg) {
        if (state == null || state.remainingPlays < 2) {
            status.accept("You need 2 plays remaining to use Double the Rent with a Rent card", true);
            return false;
        }
        List<RentCard> rentOptions = findPlayableRentCards(doubleRent);
        if (rentOptions.isEmpty()) {
            status.accept("No playable Rent card in your hand", true);
            return false;
        }
        Optional<RentCard> rentChoice = dialogs.showChoiceDialog(
                "Choose Rent Card",
                "Double the Rent",
                "Select a Rent card to play at double value (uses 2 plays):",
                rentOptions,
                rent -> rent.getName() + " (bank " + rent.getBankValueM() + "M)",
                rent -> null);
        if (rentChoice.isEmpty()) {
            return false;
        }
        Optional<Color> color = promptRentColor(rentChoice.get());
        if (color.isEmpty()) {
            return false;
        }
        msg.mode = "DOUBLE_RENT";
        msg.secondCardId = rentChoice.get().getInstanceId();
        msg.color = color.get().name();
        return true;
    }

    private List<RentCard> findPlayableRentCards(ActionCard excluding) {
        List<RentCard> options = new ArrayList<>();
        for (Card card : myHand) {
            if (card == excluding || card == null || !(card instanceof RentCard rent)) {
                continue;
            }
            if (rent.canPlay(playerViewFromSeat(localSeat))) {
                options.add(rent);
            }
        }
        return options;
    }

    private Optional<PropertyCard> promptSelectProperty(List<PropertyCard> properties,
                                                        String title, String header) {
        return dialogs.showChoiceDialog(title, header, "Select a property:", properties,
                p -> p.getName() + " (" + p.getColor() + ", " + p.getPrice() + "M)",
                p -> "-fx-border-color: " + dialogs.cssColorFor(
                        p.getColor() == null ? Color.BROWN : p.getColor()) + ";");
    }

    private Optional<Integer> promptOpponentWithCompleteSets() {
        if (state == null) {
            return Optional.empty();
        }
        List<PlayerViewDto> valid = new ArrayList<>();
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat && hasAnyCompleteSet(p.seat)) {
                valid.add(p);
            }
        }
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerViewDto> picked = dialogs.showChoiceDialog(
                "Deal Breaker",
                "Select player to steal complete set from",
                "Only showing players with complete sets:",
                valid,
                p -> p.name,
                p -> null);
        return picked.map(p -> p.seat);
    }

    private List<PropertyCard> getPropertiesForSeat(int seat) {
        List<PropertyCard> props = new ArrayList<>();
        if (state == null) {
            return props;
        }
        for (PlayerViewDto p : state.players) {
            if (p.seat != seat) {
                continue;
            }
            for (var dto : p.properties) {
                Card card = CardMapper.fromDto(dto);
                if (card instanceof PropertyCard property) {
                    props.add(property);
                }
            }
            break;
        }
        return props;
    }

    private List<PropertyCard> getStealablePropertiesForSeat(int seat) {
        Player view = playerViewFromSeat(seat);
        return PropertyRules.getPropertiesOutsideCompleteSets(view);
    }

    private Player playerViewFromSeat(int seat) {
        Player view = new Player("view");
        for (PropertyCard property : getPropertiesForSeat(seat)) {
            view.addProperty(property);
        }
        return view;
    }

    private boolean hasAnyCompleteSet(int seat) {
        Player view = playerViewFromSeat(seat);
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)) {
                return true;
            }
        }
        return false;
    }

    private List<Color> getCompleteSetColorsForSeat(int seat) {
        Player view = playerViewFromSeat(seat);
        List<Color> complete = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)) {
                complete.add(color);
            }
        }
        return complete;
    }

    private List<Color> getHouseEligibleColors() {
        Player view = playerViewFromSeat(localSeat);
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color) && !hasImprovement(localSeat, "House+", color)) {
                options.add(color);
            }
        }
        return options;
    }

    private List<Color> getHotelEligibleColors() {
        Player view = playerViewFromSeat(localSeat);
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)
                    && hasImprovement(localSeat, "House+", color)
                    && !hasImprovement(localSeat, "Hotel+", color)) {
                options.add(color);
            }
        }
        return options;
    }

    private boolean hasImprovement(int seat, String prefix, Color color) {
        return getPropertiesForSeat(seat).stream()
                .anyMatch(p -> (prefix + color).equals(p.getName()));
    }

    private String playerNameAt(int seat) {
        if (state == null || state.players == null) {
            return "Player";
        }
        for (PlayerViewDto p : state.players) {
            if (p.seat == seat) {
                return p.name;
            }
        }
        return "Player";
    }

    private Optional<Color> promptSelectWildColor(WildpropertyCard wild, List<Color> playableColors) {
        if (playableColors == null || playableColors.isEmpty()) {
            return Optional.empty();
        }
        int bankValue = wild.getBankValueM();
        String bankHint = wild.isBankable()
                ? "Deposit to bank is always " + bankValue + "M (not affected by color chosen)."
                : "This wild card cannot be deposited to bank.";
        return dialogs.showChoiceDialog(
                "Wild Property Color",
                wild.getName(),
                "Choose a color to play as property.\n" + bankHint,
                playableColors,
                color -> color + "  —  play as " + color + " property",
                color -> "-fx-background-color: " + dialogs.cssColorFor(color) + ";"
                        + "-fx-text-fill: " + dialogs.textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    private Optional<Color> promptRentColor(RentCard rentCard) {
        Map<Color, List<Card>> byColor = propertiesByColorForLocalSeat();
        List<Color> options = new ArrayList<>();
        if (rentCard.isAllColors()) {
            for (Color c : Color.values()) {
                if (PropertyRules.countBillableProperties(byColor.get(c)) > 0) {
                    options.add(c);
                }
            }
        } else {
            for (Color c : rentCard.getApplicableColors()) {
                if (PropertyRules.countBillableProperties(byColor.get(c)) > 0) {
                    options.add(c);
                }
            }
        }
        if (options.isEmpty()) {
            status.accept("No valid rent color", true);
            return Optional.empty();
        }
        if (options.size() == 1) {
            return Optional.of(options.get(0));
        }
        return dialogs.showChoiceDialog(
                "Select Rent Color",
                "Select which property set to collect rent from",
                "Color (count → rent):",
                options,
                color -> color + "  ·  " + PropertyRules.countBillableProperties(byColor.get(color)) + " cards → "
                        + PropertyRules.calculateRent(color, byColor.get(color)) + "M",
                color -> "-fx-background-color: " + dialogs.cssColorFor(color)
                        + "; -fx-text-fill: " + dialogs.textColorFor(color) + ";");
    }

    private Map<Color, List<Card>> propertiesByColorForLocalSeat() {
        Map<Color, List<Card>> byColor = new EnumMap<>(Color.class);
        if (state == null) {
            return byColor;
        }
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat) {
                continue;
            }
            for (var dto : p.properties) {
                Color color = CardMapper.parseColor(dto.color);
                if (color == null) {
                    continue;
                }
                byColor.computeIfAbsent(color, ignored -> new ArrayList<>()).add(CardMapper.fromDto(dto));
            }
        }
        return byColor;
    }

    private Optional<Integer> promptOpponentSeat(String title) {
        if (state == null) {
            return Optional.empty();
        }
        List<PlayerViewDto> opponents = new ArrayList<>();
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat) {
                opponents.add(p);
            }
        }
        if (opponents.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerViewDto> picked = dialogs.showChoiceDialog(
                title, title, "Select player:", opponents, p -> p.name, p -> null);
        return picked.map(p -> p.seat);
    }
}
