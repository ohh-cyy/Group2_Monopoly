package model.card;

import engine.GameEngine;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class WildpropertyCard extends PropertyCard {
    private Color chosenColor;
    private final List<Color> availableColors;
    private final boolean bankable;
    private final int bankValueM;

    public WildpropertyCard(String name, String description, int bankValueM,
                            List<Color> availableColors, boolean bankable) {
        super(name, description, null, bankValueM);
        this.chosenColor = null;
        this.availableColors = new ArrayList<>(availableColors);
        this.bankable = bankable;
        this.bankValueM = bankValueM;
    }

    public List<Color> getAvailableColors() {
        return new ArrayList<>(availableColors);
    }

    public boolean isBankable() {
        return bankable;
    }

    public int getBankValueM() {
        return bankValueM;
    }

    public Color getChosenColor() {
        return chosenColor;
    }

    public void setChosenColor(Color color) {
        if (color != null && availableColors.contains(color)) {
            this.chosenColor = color;
        }
    }

    @Override
    public Color getColor() {
        return chosenColor;
    }

    public void depositToBank(Player player) {
        if (bankable) {
            player.addBank(this);
        }
    }

    @Override
    public void use(Player player, GameEngine game) {
        if (chosenColor == null) {
            return;
        }
        player.addProperty(this);
    }
}
