package engine;
import model.card.Card;

import java.util.ArrayList;
import java.util.List;


public class DiscardPile {
    private List<Card> cards;

    public DiscardPile() {
        cards = new ArrayList<>();
    }

    // 添加到弃牌堆
    public void addCard(Card card) {
        cards.add(card);
    }

    // 查看最上面的牌
    public Card peekTop() {

        if (cards.isEmpty()) {
            return null;
        }

        return cards.get(cards.size() - 1);
    }

    // 判断是否为空
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    // 获取数量
    public int size() {
        return cards.size();
    }

    // 显示弃牌堆
    public void showDiscardPile() {

        System.out.println("===== 弃牌堆 =====");

        for (Card card : cards) {
            System.out.println(card.getName());
        }
    }

    // 获取所有牌（后面可能洗回牌库）
    public List<Card> getCards() {
        return cards;
    }

}
