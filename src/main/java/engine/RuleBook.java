package engine;

import model.enums.Color;
import java.util.HashMap;
import java.util.Map;

public class RuleBook {
    private static final Map<Color, Integer> PROPERTY_SET_REQUIREMENTS;
    private static final int INITIAL_HAND_SIZE;
    private static final int CARDS_PER_TURN;
    private static final int WINNING_SET_COUNT;

    static {
        PROPERTY_SET_REQUIREMENTS = new HashMap<>();
        PROPERTY_SET_REQUIREMENTS.put(Color.BROWN, 2);
        PROPERTY_SET_REQUIREMENTS.put(Color.LIGHT_BLUE, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.PINK, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.ORANGE, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.RED, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.YELLOW, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.GREEN, 3);
        PROPERTY_SET_REQUIREMENTS.put(Color.DARK_BLUE, 2);
        PROPERTY_SET_REQUIREMENTS.put(Color.BLACK, 4);
        PROPERTY_SET_REQUIREMENTS.put(Color.LIGHT_GREEN, 2);

        INITIAL_HAND_SIZE = 5;
        CARDS_PER_TURN = 2;
        WINNING_SET_COUNT = 3;
    }

    public static int getRequiredCountForColor(Color color) {
        return PROPERTY_SET_REQUIREMENTS.getOrDefault(color, 3);
    }

    public static int getInitialHandSize() {
        return INITIAL_HAND_SIZE;
    }

    public static int getCardsPerTurn() {
        return CARDS_PER_TURN;
    }

    public static int getWinningSetCount() {
        return WINNING_SET_COUNT;
    }

    public static boolean isValidPropertySet(Color color, int count) {
        Integer required = PROPERTY_SET_REQUIREMENTS.get(color);
        return required != null && count >= required;
    }
}
