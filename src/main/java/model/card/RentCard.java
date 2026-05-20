package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class RentCard extends Card {
    private final Color[] applicableColors;
    private final int baseRent;

    public RentCard(String name, String description, int baseRent, Color... applicableColors) {
        super(name, description, CardType.ACTION);
        this.baseRent = baseRent;
        this.applicableColors = applicableColors;
    }

    public Color[] getApplicableColors() {
        return applicableColors;
    }

    public int getBaseRent() {
        return baseRent;
    }

    @Override
    public void use(Player player, GameEngine game) {
        Player target = game.getDefaultDefender(player);
        
        int rentAmount = calculateRent(player);
        
        if (target.hasEnoughMoney(rentAmount)) {
            target.removeMoney(rentAmount);
            player.addMoney(rentAmount);
            System.out.println(player.getName() + " used " + getName() + 
                " to collect " + rentAmount + "M from " + target.getName());
        } else {
            int availableMoney = target.getMoney();
            target.removeMoney(availableMoney);
            player.addMoney(availableMoney);
            System.out.println(player.getName() + " used " + getName() + 
                " to collect " + availableMoney + "M from " + target.getName() + 
                " (insufficient funds)");
        }
        
        game.getDiscardPile().addCard(this);
    }
    
    private int calculateRent(Player player) {
        int maxRent = 0;
        
        for (Color color : applicableColors) {
            if (player.hasCompleteSet(color)) {
                int rent = baseRent * 2;
                if (rent > maxRent) {
                    maxRent = rent;
                }
            } else {
                int propertyCount = player.getPropertiesByColor(color).size();
                if (propertyCount > 0) {
                    int rent = baseRent;
                    if (rent > maxRent) {
                        maxRent = rent;
                    }
                }
            }
        }
        
        return maxRent > 0 ? maxRent : baseRent;
    }
}
