package model.card.actionCard;

import engine.GameEngine;
import engine.RentPayment;
import model.enums.CardType;
import model.player.Player;

public class MyBirthday extends ActionCard {

    public static final int GIFT_AMOUNT = 2;

    public MyBirthday(String name, String description, CardType type) {
        super(name, description, type, 2);
    }

    public MyBirthday(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 2);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 由 GameController 调用 collectFromEveryone
    }

    /**
     * 每位其他玩家向 birthday 玩家支付 2M（银行不足时用地产抵付，地产进入收礼者地产区）。
     *
     * @return 总共收到的金额
     */
    public int collectFromEveryone(Player birthdayPlayer, GameEngine game) {
        int total = 0;
        for (Player other : game.getPlayers()) {
            if (other.equals(birthdayPlayer)) {
                continue;
            }
            total += RentPayment.collectUpTo(birthdayPlayer, other, GIFT_AMOUNT);
        }
        return total;
    }
}
