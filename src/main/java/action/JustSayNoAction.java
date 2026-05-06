package action;

import engine.GameEngine;
import model.player.Player;

public class JustSayNoAction implements ActionHandler {
    @Override
    public void execute(Player attacker, Player defender, GameEngine game) {
        System.out.println(defender.getName() + " plays Just Say No, cancels opponent card effect");
    }
}