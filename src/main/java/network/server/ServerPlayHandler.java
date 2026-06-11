package network.server;

import engine.GameEngine;
import engine.RentPayment;
import model.card.Card;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.protocol.ClientMessage;

import java.util.List;

/**
 * 在服务端执行即时行动卡效果（无 UI）。
 * 需交互的流程（带支付选择的租金、JSN 链）改用 {@link PendingActionResolution}。
 */
public final class ServerPlayHandler {
    /**
     * 根据出牌消息中的目标/颜色应用单张行动卡效果。
     * 卡牌需要异步交互或输入无效时返回 false。
     */
    public boolean applyEffect(GameEngine engine, Player player, ActionCard card, ClientMessage msg,
                               List<String> log) {
        if (card instanceof RentCard rentCard) {
            return applyRent(engine, player, rentCard, msg, log);
        }
        if (card instanceof DoubleTheRent) {
            return false;
        }
        if (card instanceof MyBirthday) {
            return applyMyBirthday(engine, player, log);
        }
        if (card instanceof DebtCollector debtCollector) {
            return applyDebtCollector(engine, player, debtCollector, msg, log);
        }
        if (card instanceof PassGoCard) {
            card.use(player, engine);
            log.add(player.getName() + " played Pass Go");
            return true;
        }
        if (card instanceof SlyDeal slyDeal) {
            return applySlyDeal(engine, player, slyDeal, msg, log);
        }
        if (card instanceof DealBreaker dealBreaker) {
            return applyDealBreaker(engine, player, dealBreaker, msg, log);
        }
        if (card instanceof ForcedDeal forcedDeal) {
            return applyForcedDeal(engine, player, forcedDeal, msg, log);
        }
        if (card instanceof House house) {
            Color color = CardMapper.parseColor(msg.color);
            if (color == null) {
                return false;
            }
            boolean ok = house.addHouseToSet(player, color);
            if (ok) {
                log.add(player.getName() + " added House to " + color);
            }
            return ok;
        }
        if (card instanceof Hotel hotel) {
            Color color = CardMapper.parseColor(msg.color);
            if (color == null) {
                return false;
            }
            boolean ok = hotel.addHotelToSet(player, color);
            if (ok) {
                log.add(player.getName() + " added Hotel to " + color);
            }
            return ok;
        }
        card.use(player, engine);
        return true;
    }

    /** 自动向每位对手收取租金（仅用于服务端非交互路径）。 */
    private boolean applyRent(GameEngine engine, Player player, RentCard rentCard, ClientMessage msg,
                              List<String> log) {
        Color chargeColor = CardMapper.parseColor(msg.color);
        if (chargeColor == null || !rentCard.getChargeableColors(player).contains(chargeColor)) {
            return false;
        }
        int rent = rentCard.calculateRent(player, chargeColor);
        if (rent <= 0) {
            return false;
        }
        int rentPerPlayer = rent;
        if (engine.isRentDoubled()) {
            rentPerPlayer *= 2;
            engine.setRentDoubled(false);
        }
        int total = 0;
        for (Player other : engine.getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            total += RentPayment.collectUpTo(player, other, rentPerPlayer);
        }
        log.add(player.getName() + " collected " + chargeColor + " rent (" + rentPerPlayer
                + "M/player, total " + total + "M)");
        return true;
    }

    private boolean applyMyBirthday(GameEngine engine, Player player, List<String> log) {
        int total = 0;
        for (Player other : engine.getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            total += RentPayment.collectUpTo(player, other, MyBirthday.GIFT_AMOUNT);
        }
        log.add(player.getName() + " collected " + total + "M from My Birthday");
        return true;
    }

    private boolean applyDebtCollector(GameEngine engine, Player player, DebtCollector debtCollector,
                                       ClientMessage msg, List<String> log) {
        if (msg.targetSeat == null || msg.targetSeat < 0 || msg.targetSeat >= engine.getPlayers().size()) {
            return false;
        }
        Player target = engine.getPlayers().get(msg.targetSeat);
        if (target.equals(player)) {
            return false;
        }
        int paid = debtCollector.collectFrom(player, target);
        log.add(player.getName() + " collected " + paid + "M from " + target.getName());
        return paid > 0;
    }

    private boolean applySlyDeal(GameEngine engine, Player player, SlyDeal slyDeal, ClientMessage msg,
                                 List<String> log) {
        if (msg.targetSeat == null || msg.targetCardId == null) {
            return false;
        }
        Player target = engine.getPlayers().get(msg.targetSeat);
        PropertyCard property = findPropertyById(target, msg.targetCardId);
        if (property == null) {
            return false;
        }
        if (slyDeal.stealProperty(player, target, property)) {
            log.add(player.getName() + " stole " + property.getName() + " from " + target.getName());
            return true;
        }
        return false;
    }

    private boolean applyDealBreaker(GameEngine engine, Player player, DealBreaker dealBreaker, ClientMessage msg,
                                     List<String> log) {
        if (msg.targetSeat == null || msg.color == null) {
            return false;
        }
        Player target = engine.getPlayers().get(msg.targetSeat);
        Color color = CardMapper.parseColor(msg.color);
        if (color == null) {
            return false;
        }
        if (dealBreaker.useOnTarget(player, target, color, engine)) {
            log.add(player.getName() + " stole complete " + color + " set from " + target.getName());
            return true;
        }
        return false;
    }

    private boolean applyForcedDeal(GameEngine engine, Player player, ForcedDeal forcedDeal, ClientMessage msg,
                                    List<String> log) {
        if (msg.targetSeat == null || msg.targetCardId == null || msg.secondCardId == null) {
            return false;
        }
        Player target = engine.getPlayers().get(msg.targetSeat);
        PropertyCard mine = findPropertyById(player, msg.targetCardId);
        PropertyCard theirs = findPropertyById(target, msg.secondCardId);
        if (mine == null || theirs == null) {
            return false;
        }
        if (forcedDeal.swapProperties(player, mine, target, theirs)) {
            log.add(player.getName() + " swapped properties with " + target.getName());
            return true;
        }
        return false;
    }

    private PropertyCard findPropertyById(Player player, String cardId) {
        for (PropertyCard property : player.getAllProperties()) {
            if (property.getInstanceId().equals(cardId)) {
                return property;
            }
        }
        return null;
    }
}
