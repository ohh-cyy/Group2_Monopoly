package engine;

import model.enums.Color;

import java.util.EnumMap;
import java.util.Map;

/**
 * 各颜色地产按持有张数计算的租金（百万 M）。
 * 索引 0 = 1 张，1 = 2 张，以此类推。
 */
public final class RentTable {
    private static final Map<Color, int[]> RENT_BY_COUNT = new EnumMap<>(Color.class);

    static {
        RENT_BY_COUNT.put(Color.BROWN, new int[]{1, 2});
        RENT_BY_COUNT.put(Color.DARK_BLUE, new int[]{3, 8});
        RENT_BY_COUNT.put(Color.GREEN, new int[]{2, 4, 7});
        RENT_BY_COUNT.put(Color.LIGHT_BLUE, new int[]{1, 2, 3});
        RENT_BY_COUNT.put(Color.ORANGE, new int[]{1, 3, 5});
        RENT_BY_COUNT.put(Color.PINK, new int[]{1, 2, 4});
        RENT_BY_COUNT.put(Color.BLACK, new int[]{1, 2, 3, 4});
        RENT_BY_COUNT.put(Color.RED, new int[]{2, 3, 6});
        RENT_BY_COUNT.put(Color.LIGHT_GREEN, new int[]{1, 2});
        RENT_BY_COUNT.put(Color.YELLOW, new int[]{2, 4, 6});
    }

    private RentTable() {
    }

    public static int getRent(Color color, int propertyCount) {
        if (color == null || propertyCount <= 0) {
            return 0;
        }
        int[] tiers = RENT_BY_COUNT.get(color);
        if (tiers == null || tiers.length == 0) {
            return 0;
        }
        int index = Math.min(propertyCount, tiers.length) - 1;
        return tiers[index];
    }

    /** 用于地产卡牌面展示，如 "1M / 2M" */
    public static String formatRentTiers(Color color) {
        int[] tiers = RENT_BY_COUNT.get(color);
        if (tiers == null || tiers.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tiers.length; i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(tiers[i]).append('M');
        }
        return sb.toString();
    }
}
