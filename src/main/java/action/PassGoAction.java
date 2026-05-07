package action;

import engine.GameEngine;
import model.player.Player;

public class PassGoAction implements ActionHandler {
    @Override
    public void execute(Player attacker, Player defender, GameEngine game) {
        System.out.println(attacker.getName() + " passes Go and receives bonus reward");
    }
}