package model.card;

import engine.GameEngine;
import engine.PropertyRules;
import engine.RentPayment;
import model.card.actionCard.ActionCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.Arrays;
import java.util.List;


public class RentCard extends ActionCard {
    /** Colors this card can charge rent on (ignored when {@link #allColors} is true). */
    private final Color[] applicableColors;
    /** When true, rent may be charged on any color the player owns. */
    private final boolean allColors;

    /** All-color rent card with 3M bank value. */
    public static RentCard allColors() {
        return new RentCard(
                "Rent (All)",
                "Charge rent on any color you own",
                3,
                true,
                Color.values()
        );
    }

    /**
     所有颜色租金卡，通过实例ID
     */
    public static RentCard allColors(String instanceId) {
        return new RentCard(
                instanceId,
                "Rent (All)",
                "Charge rent on any color you own",
                3,
                true,
                Color.values()
        );
    }

    /** 双颜色租金卡，1M银行价值 */
    public static RentCard dual(Color c1, Color c2) {
        String label = c1.name() + " / " + c2.name();
        return new RentCard(
                "Rent (" + label + ")",
                "Charge rent on " + c1 + " or " + c2 + " properties you own",
                1,
                false,
                c1, c2
        );
    }

    /**
     * 双颜色租金卡，通过实例ID
     */
    public static RentCard dual(String instanceId, Color c1, Color c2) {
        String label = c1.name() + " / " + c2.name();
        return new RentCard(
                instanceId,
                "Rent (" + label + ")",
                "Charge rent on " + c1 + " or " + c2 + " properties you own",
                1,
                false,
                c1, c2
        );
    }

    private RentCard(String name, String description, int bankValueM, boolean allColors, Color... colors) {
        super(name, description, CardType.ACTION, bankValueM);
        this.allColors = allColors;
        this.applicableColors = colors;
    }

    private RentCard(String instanceId, String name, String description, int bankValueM, boolean allColors,
                       Color... colors) {
        super(instanceId, name, description, CardType.ACTION, bankValueM);
        this.allColors = allColors;
        this.applicableColors = colors;
    }

    /** @return defensive copy of applicable colors */
    public Color[] getApplicableColors() {
        return applicableColors.clone();
    }

    /** @return {@code true} if this card can charge rent on any owned color */
    public boolean isAllColors() {
        return allColors;
    }

    /** Two-color cards need at least one matching property owned by the player. */
    public boolean canPlay(Player player) {
        if (allColors) {
            return true;
        }
        for (Color color : applicableColors) {
            if (countProperties(player, color) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Counts billable properties used to calculate rent for this color. */
    public int countProperties(Player player, Color color) {
        return PropertyRules.countBillableProperties(player, color);
    }

    /**
     * 计算租金，根据玩家property数量
     */
    public int calculateRent(Player player, Color chargeColor) {
        return PropertyRules.calculateRent(player, chargeColor);
    }

    /**
     * 收集租金，除了收集者
     */
    public int collectFromAll(Player collector, GameEngine game, Color chargeColor, int rentPerPlayer) {
        if (rentPerPlayer <= 0) {
            return 0;
        }
        if (game.isRentDoubled()) {
            rentPerPlayer *= 2;
            game.setRentDoubled(false);
        }

        int totalCollected = 0;
        for (Player other : game.getPlayers()) {
            if (other.equals(collector)) {
                continue;
            }
            int paid = RentPayment.collectUpTo(collector, other, rentPerPlayer);
            totalCollected += paid;
        }
        return totalCollected;
    }

    /** Colors the player can charge rent on. */
    public List<Color> getChargeableColors(Player player) {
        if (allColors) {
            return Arrays.stream(Color.values())
                    .filter(c -> countProperties(player, c) > 0)
                    .toList();
        }
        return Arrays.stream(applicableColors)
                .filter(c -> countProperties(player, c) > 0)
                .toList();
    }

    /**
     * Plays this rent card automatically, choosing the highest-rent color
     * and collecting from all opponents.
     */
    @Override
    public void use(Player player, GameEngine game) {
        Color chargeColor = pickBestColor(player);
        if (chargeColor == null) {
            return;
        }
        int rent = calculateRent(player, chargeColor);
        collectFromAll(player, game, chargeColor, rent);
        game.getDiscardPile().addCard(this);
    }

    private Color pickBestColor(Player player) {
        Color best = null;
        int bestRent = 0;
        for (Color color : getChargeableColors(player)) {
            int rent = calculateRent(player, color);
            if (rent > bestRent) {
                bestRent = rent;
                best = color;
            }
        }
        if (best != null) {
            return best;
        }
        if (allColors) {
            return Color.BROWN;
        }
        for (Color color : applicableColors) {
            if (countProperties(player, color) > 0) {
                return color;
            }
        }
        return null;
    }
}
