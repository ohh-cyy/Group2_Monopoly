package model.card;

import engine.GameEngine;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Wild property card that can be assigned to one of several colors before play.
 * May optionally be deposited in the bank instead of forming a property set.
 */
public class WildpropertyCard extends PropertyCard {
    /** Color chosen by the player before this card is played or added to a set. */
    private Color chosenColor;
    /** Colors this wild card may be assigned to. */
    private final List<Color> availableColors;
    /** When true, this card may be placed in the bank for its bank value. */
    private final boolean bankable;
    /** Value in millions (M) when deposited in the bank. */
    private final int bankValueM;

    /**
     * Creates a wild property card with a randomly generated instance id.
     *
     * @param name             display name
     * @param description      rules text
     * @param bankValueM       bank deposit value in millions (M)
     * @param availableColors  colors this card may be assigned to
     * @param bankable         whether the card can be deposited in the bank
     */
    public WildpropertyCard(String name, String description, int bankValueM,
                            List<Color> availableColors, boolean bankable) {
        super(name, description, null, bankValueM);
        this.chosenColor = null;
        this.availableColors = new ArrayList<>(availableColors);
        this.bankable = bankable;
        this.bankValueM = bankValueM;
    }

    /**
     * Creates a wild property card with an explicit instance id.
     *
     * @param instanceId       unique card instance id
     * @param name             display name
     * @param description      rules text
     * @param bankValueM       bank deposit value in millions (M)
     * @param availableColors  colors this card may be assigned to
     * @param bankable         whether the card can be deposited in the bank
     */
    public WildpropertyCard(String instanceId, String name, String description, int bankValueM,
                            List<Color> availableColors, boolean bankable) {
        super(instanceId, name, description, null, bankValueM);
        this.chosenColor = null;
        this.availableColors = new ArrayList<>(availableColors);
        this.bankable = bankable;
        this.bankValueM = bankValueM;
    }

    /** @return defensive copy of assignable colors */
    public List<Color> getAvailableColors() {
        return new ArrayList<>(availableColors);
    }

    /** @return {@code true} if this card may be deposited in the bank */
    public boolean isBankable() {
        return bankable;
    }

    /** @return bank deposit value in millions (M) */
    public int getBankValueM() {
        return bankValueM;
    }

    /** @return bank deposit value in millions (M) when taken as payment */
    @Override
    public int getPaymentValueM() {
        return bankValueM;
    }

    /** @return the color currently assigned to this wild card, or {@code null} if unset */
    public Color getChosenColor() {
        return chosenColor;
    }

    /**
     * Assigns a color to this wild card.
     * Only colors in {@link #availableColors} are accepted.
     *
     * @param color color to assign; ignored if not available
     */
    public void setChosenColor(Color color) {
        if (color != null && availableColors.contains(color)) {
            this.chosenColor = color;
        }
    }

    /** @return the player-assigned color, or {@code null} if not yet chosen */
    @Override
    public Color getColor() {
        return chosenColor;
    }

    /**
     * Deposits this card in the bank if {@link #bankable} is true.
     *
     * @param player player receiving the bank deposit
     */
    public void depositToBank(Player player) {
        if (bankable) {
            player.addBank(this);
        }
    }

    /**
     * Adds this wild property to the player's property table.
     * Requires a color to have been chosen first.
     */
    @Override
    public void use(Player player, GameEngine game) {
        if (chosenColor == null) {
            return;
        }
        player.addProperty(this);
    }
}
