package model.card;

import engine.GameEngine;
import engine.RentPayment;
import engine.RentTable;
import model.card.actionCard.ActionCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.Arrays;
import java.util.List;

public class RentCard extends ActionCard {
    private final Color[] applicableColors;
    private final boolean allColors;

    /** 全色租金卡，存银行 3M */
    public static RentCard allColors() {
        return new RentCard(
                "Rent (All)",
                "Charge rent on any color you own",
                3,
                true,
                Color.values()
        );
    }

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

    /** 双色租金卡，存银行 1M */
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

    public Color[] getApplicableColors() {
        return applicableColors.clone();
    }

    public boolean isAllColors() {
        return allColors;
    }

    /** 双色卡：出牌者至少拥有一张适用颜色的地产 */
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

    /** 该颜色下可用于计算租金的地产张数 */
    public int countProperties(Player player, Color color) {
        return player.getPropertiesByColor(color).size();
    }

    public int calculateRent(Player player, Color chargeColor) {
        int count = countProperties(player, chargeColor);
        return RentTable.getRent(chargeColor, count);
    }

    /**
     * 向除出牌者外的所有玩家收租。
     *
     * @return 各玩家实付总额
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
            int paid = RentPayment.collect(collector, other, rentPerPlayer);
            totalCollected += paid;
        }
        return totalCollected;
    }

    /** 出牌者可选择收租的颜色（至少 1 张地产，全色卡任意颜色） */
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
