package model.card;

import engine.GameEngine;
import model.enums.Color;
import model.player.Player;

import java.util.List;

public class WildpropertyCard extends PropertyCard {
    private Color chosenColor;
    private List<Color> availableColors;
    private boolean bankable;

    public WildpropertyCard(String name, String description, int price,List<Color> availableColors, boolean bankable) {
        super(name, description, null, price);
        this.chosenColor = null;
        this.availableColors = availableColors;
        this.bankable = bankable;
    }

    public List<Color> getAvailableColors() {
        return availableColors;
    }

    public boolean isBankable() {
        return bankable;
    }

    public Color getChosenColor() {
        return chosenColor;
    }

    public void setChosenColor(Color color) {
        if (availableColors.contains(color)) {
            this.chosenColor = color;
        } else {
            System.out.println("Invalid color choice for this wildcard");
        }
    }

    @Override
    public Color getColor() {
        return chosenColor;
    }

    @Override
    public void use(Player player, GameEngine game) {
        if (chosenColor == null) {
            chosenColor = selectColor(player);
        }
        
        player.addProperty(this);

        System.out.println(player.getName() + " played " + getName() + 
            " as " + chosenColor + " property");
    }
    
    private Color selectColor(Player player) {
        Color mostNeededColor = null;
        int maxCount = -1;
        
        for (Color color : availableColors) {
            int count = player.getPropertiesByColor(color).size();
            if (count > maxCount && count < color.getSetSize()) {
                maxCount = count;
                mostNeededColor = color;
            }
        }
        
        if (mostNeededColor == null) {
            mostNeededColor = Color.BROWN;
        }
        
        return mostNeededColor;
    }
}
