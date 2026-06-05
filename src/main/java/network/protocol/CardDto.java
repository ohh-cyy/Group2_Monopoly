package network.protocol;

import java.util.ArrayList;
import java.util.List;

public class CardDto {
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
