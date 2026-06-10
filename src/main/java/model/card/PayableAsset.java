package model.card;

/**
 * Contract for cards that can provide monetary value when used as payment
 * or placed in a player's bank.
 */
public interface PayableAsset {
    /**
     * @return payment value in millions (M) when this asset is taken as rent or debt
     */
    int getPaymentValueM();
}
