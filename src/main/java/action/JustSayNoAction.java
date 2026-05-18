package action;

import engine.GameEngine;
import model.player.Player;

public class JustSayNoAction implements ActionHandler {
    @Override
    public void execute(Player attacker, Player defender, GameEngine game) {
        System.out.println(attacker.getName() + " 打出「拒绝」：本简化版中仅作占位，未实现完整反击链。");
    }
}
