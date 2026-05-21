package model.card;

import engine.GameEngine;
import model.enums.Color;
import model.player.Player;

public class WildpropertyCard extends PropertyCard {
    private Color chosenColor;

    public WildpropertyCard(String name, String description, int price) {
        super(name, description, null, price);
        this.chosenColor = null;
    }

    public Color getChosenColor() {
        return chosenColor;
    }

    public void setChosenColor(Color color) {
        this.chosenColor = color;
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
        
        for (Color color : Color.values()) {
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
