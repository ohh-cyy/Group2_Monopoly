package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 可序列化的卡牌快照（用于房间文件夹同步） */
public class CardSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String name;
    public String description;
    public String type;
    public String cardKind;
    public String color;
    public Integer money;
    public Integer price;
    public Integer bankValue;
    public List<String> wildColors = new ArrayList<>();
    public Boolean bankable;
    public Boolean allColorsRent;
    public List<String> rentColors = new ArrayList<>();
}
