package sync;

import java.io.Serializable;

public class RoomCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    public int seat;
    public String action;
    public String cardId;
    public String mode;
    public String color;
}
