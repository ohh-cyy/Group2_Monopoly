package ui.layout;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ui.PublicPropertyBoardLayout;

import java.util.function.IntSupplier;

/**
 * Keeps public property board rows equally sized when the table area resizes.
 * <p>
 * Listens for width and height changes, then invokes a callback when row height
 * shifts enough to require re-rendering card metrics.
 */
public final class PropertyBoardLayoutTracker {
    private final VBox allPlayersPropertiesPanel;
    private final IntSupplier playerCount;
    private final Runnable onRescaleNeeded;

    private double lastPropertyRowHeight = -1;

    /**
     * Creates a tracker for the all-players property panel.
     *
     * @param onRescaleNeeded callback invoked when row height changes enough to re-render cards
     */
    public PropertyBoardLayoutTracker(VBox allPlayersPropertiesPanel,
                                        IntSupplier playerCount,
                                        Runnable onRescaleNeeded) {
        this.allPlayersPropertiesPanel = allPlayersPropertiesPanel;
        this.playerCount = playerCount;
        this.onRescaleNeeded = onRescaleNeeded;
    }

    /** Returns the last measured property row height, or {@code -1} if unset. */
    public double lastRowHeight() {
        return lastPropertyRowHeight;
    }

    /** Stores the latest measured row height when positive. */
    public void setLastRowHeight(double rowHeight) {
        if (rowHeight > 0) {
            lastPropertyRowHeight = rowHeight;
        }
    }

    /** Attaches width and table-area height listeners to trigger rescaling. */
    public void attach() {
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        allPlayersPropertiesPanel.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0 && playerCount.getAsInt() > 0) {
                onRescaleNeeded.run();
            }
        });
        attachTableAreaHeightListener();
    }

    private void attachTableAreaHeightListener() {
        Parent node = allPlayersPropertiesPanel;
        while (node != null && !node.getStyleClass().contains("table-area")) {
            node = node.getParent();
        }
        if (!(node instanceof Region tableArea)) {
            return;
        }
        tableArea.heightProperty().addListener((obs, oldH, newH) -> {
            if (newH.doubleValue() <= 0) {
                return;
            }
            refreshLayout();
        });
    }

    /** Recomputes equal row layout and triggers rescale when row height changes. */
    public void refreshLayout() {
        int count = playerCount.getAsInt();
        if (count <= 0) {
            PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel);
            return;
        }
        PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel, count);
        maybeRescale();
    }

    private void maybeRescale() {
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        int count = playerCount.getAsInt();
        if (count <= 0) {
            return;
        }
        double rowHeight = PublicPropertyBoardLayout.rowHeightFor(allPlayersPropertiesPanel, count);
        if (rowHeight <= 0 || Math.abs(rowHeight - lastPropertyRowHeight) <= 2) {
            return;
        }
        lastPropertyRowHeight = rowHeight;
        onRescaleNeeded.run();
    }
}
