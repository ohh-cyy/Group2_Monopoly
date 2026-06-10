package model.enums;

/**
 * Property color groups in Monopoly Deal.
 * Each color defines how many cards are required to complete a full set.
 */
public enum Color {
    /** Brown property group; 2 cards complete the set. */
    BROWN(2),
    /** Dark blue property group; 2 cards complete the set. */
    DARK_BLUE(2),
    /** Green property group; 3 cards complete the set. */
    GREEN(3),
    /** Orange property group; 3 cards complete the set. */
    ORANGE(3),
    /** Red property group; 3 cards complete the set. */
    RED(3),
    /** Yellow property group; 3 cards complete the set. */
    YELLOW(3),
    /** Black (railroad) property group; 4 cards complete the set. */
    BLACK(4),
    /** Light blue property group; 3 cards complete the set. */
    LIGHT_BLUE(3),
    /** Light green (utility) property group; 2 cards complete the set. */
    LIGHT_GREEN(2),
    /** Pink property group; 3 cards complete the set. */
    PINK(3);

    /** Number of property cards needed to complete a full set of this color. */
    private final int setSize;

    Color(int setSize) {
        this.setSize = setSize;
    }

    /** @return the number of cards required for a complete set */
    public int getSetSize() {
        return setSize;
    }

    /** Lowercase key for game log display, e.g. light_blue. */
    public String logKey() {
        return name().toLowerCase();
    }
}
