package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire representation of a {@link model.card.Card}.
 * {@link #cardKind} disambiguates concrete card types for {@link network.CardMapper}.
 */
public class CardDto {
    /** Unique instance id used in play/discard/payment commands. */
    public String id;

    /** Display name shown on the card face. */
    public String name;
    /** Short rules or flavor text for the card. */
    public String description;

    /** {@link model.enums.CardType} name: PROPERTY, MONEY, or ACTION. */
    public String type;

    /** Fine-grained kind, e.g. MONEY, PROPERTY, RENT, SLY_DEAL. */
    public String cardKind;

    /** Current or chosen property color name. */
    public String color;

    /** Face value for money cards. */
    public Integer money;

    /** Property purchase price where applicable. */
    public Integer price;

    /** Bank deposit value for action or wild cards. */
    public Integer bankValue;

    /** Wild property: legal color choices before placement. */
    public List<String> wildColors = new ArrayList<>();

    /** Wild property: whether it may be deposited to the bank. */
    public Boolean bankable;

    /** Rent card: true for the all-colors rent variant. */
    public Boolean allColorsRent;

    /** Rent card: applicable color names for dual-color rents. */
    public List<String> rentColors = new ArrayList<>();
}
