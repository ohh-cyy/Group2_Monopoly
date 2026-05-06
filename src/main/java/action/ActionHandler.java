package action;
import model.player.Player;
import engine.GameEngine;

public interface ActionHandler {
    void execute(Player attacker, Player defender, GameEngine game);
}
