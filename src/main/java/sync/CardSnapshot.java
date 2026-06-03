package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Serializable card data used for shared-folder room sync. */
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
