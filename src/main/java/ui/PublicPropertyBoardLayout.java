package ui;

import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Fixed equal vertical splits for each player's public property row. */
public final class PublicPropertyBoardLayout {
    private static final double MIN_ROW_HEIGHT = 72;
    private static final double TITLE_OVERHEAD = 62;

    private PublicPropertyBoardLayout() {
    }

    public static void applyEqualRows(VBox propertyPanel) {
        if (propertyPanel == null) {
            return;
        }
        propertyPanel.setFillWidth(true);
        int rowCount = propertyPanel.getChildren().size();
        if (rowCount <= 0) {
            return;
        }
        double rowHeight = rowHeightFor(propertyPanel, rowCount);
        for (var child : propertyPanel.getChildren()) {
            if (!(child instanceof Region region)) {
                continue;
            }
            region.setMinHeight(rowHeight);
            region.setPrefHeight(rowHeight);
            region.setMaxHeight(rowHeight);
            VBox.setVgrow(region, Priority.NEVER);
        }
    }

    public static double rowHeightFor(VBox propertyPanel) {
        if (propertyPanel == null) {
            return MIN_ROW_HEIGHT;
        }
        return rowHeightFor(propertyPanel, propertyPanel.getChildren().size());
    }

    public static double rowHeightFor(VBox propertyPanel, int rowCount) {
        if (propertyPanel == null || rowCount <= 0) {
            return MIN_ROW_HEIGHT;
        }
        double spacing = propertyPanel.getSpacing() * Math.max(0, rowCount - 1);
        double available = propertyPanel.getHeight() - spacing;
        if (available <= 0) {
            return estimateRowHeight(propertyPanel, rowCount);
        }
        return Math.max(MIN_ROW_HEIGHT, available / rowCount);
    }

    public static double estimateRowHeight(VBox propertyPanel, int rowCount) {
        if (propertyPanel == null || rowCount <= 0) {
            return MIN_ROW_HEIGHT;
        }
        if (propertyPanel.getHeight() > 0) {
            return rowHeightFor(propertyPanel, rowCount);
        }
        Region parent = propertyPanel.getParent() instanceof Region region ? region : null;
        while (parent != null && parent.getHeight() <= 0) {
            parent = parent.getParent() instanceof Region next ? next : null;
        }
        if (parent == null || parent.getHeight() <= 0) {
            return MIN_ROW_HEIGHT;
        }
        double spacing = propertyPanel.getSpacing() * Math.max(0, rowCount - 1);
        return Math.max(MIN_ROW_HEIGHT, (parent.getHeight() - spacing) / rowCount);
    }

    public static CardView.CardMetrics cardMetricsForRow(double rowHeight) {
        double cardAreaHeight = Math.max(36, rowHeight - TITLE_OVERHEAD);
        double factor = Math.min(1.0, Math.max(0.28, cardAreaHeight / CardView.PUBLIC.slotH()));
        return CardView.PUBLIC.scaled(factor);
    }
}
