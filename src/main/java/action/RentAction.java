package action;
import engine.GameEngine;
import model.player.Player;

public class RentAction implements ActionHandler {
    @Override
    public void execute(Player attacker, Player defender, GameEngine game) {
        System.out.println(attacker.getName() + " collects property rent from " + defender.getName());
    }
}