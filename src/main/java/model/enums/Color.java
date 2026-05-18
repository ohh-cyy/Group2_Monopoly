package model.enums;

public enum Color {
    BROWN(2),
    DARK_BLUE(2),
    GREEN(3),
    ORANGE(3),
    RED(3),
    YELLOW(3),
    BLACK(4),
    LIGHT_BLUE(3),
    WHITE(2),
    PINK(3);

    private final int setSize;

    Color(int setSize) {
        this.setSize = setSize;
    }

    public int getSetSize() {
        return setSize;
    }
}
