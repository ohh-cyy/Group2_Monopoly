package ui;

import model.card.WildpropertyCard;
import model.enums.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 测试卡牌图片加载与万能地产旋转显示 {@link ui.CardImageLoader}。 */
class CardImageLoaderTest {

    @Test
    void bottomColorOnWildPropertyRotatesImageUpsideDown() {
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild property", 2, List.of(Color.ORANGE, Color.PINK), true);

        assertEquals(0, CardImageLoader.resolveRotationDegrees(wild));

        wild.setChosenColor(Color.ORANGE);
        assertEquals(0, CardImageLoader.resolveRotationDegrees(wild));

        wild.setChosenColor(Color.PINK);
        assertEquals(180, CardImageLoader.resolveRotationDegrees(wild));
    }

    @Test
    void darkBlueGreenUsesActualImageOrientation() {
        WildpropertyCard greenSideUp = new WildpropertyCard(
                "Dark Blue/Green", "Wild property", 4, List.of(Color.DARK_BLUE, Color.GREEN), true);
        greenSideUp.setChosenColor(Color.GREEN);

        WildpropertyCard darkBlueSideUp = new WildpropertyCard(
                "Dark Blue/Green", "Wild property", 4, List.of(Color.DARK_BLUE, Color.GREEN), true);
        darkBlueSideUp.setChosenColor(Color.DARK_BLUE);

        assertEquals(0, CardImageLoader.resolveRotationDegrees(greenSideUp));
        assertEquals(180, CardImageLoader.resolveRotationDegrees(darkBlueSideUp));
    }

    @Test
    void allColorWildPropertyDoesNotRotate() {
        WildpropertyCard wild = new WildpropertyCard(
                "All Color", "Wild property", 0, List.of(Color.values()), false);
        wild.setChosenColor(Color.RED);

        assertEquals(0, CardImageLoader.resolveRotationDegrees(wild));
    }
}
