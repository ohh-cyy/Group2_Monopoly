package ui;

import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Fixed equal vertical splits for each player's public property row. */
public final class PublicPropertyBoardLayout {
    private static final double ABSOLUTE_MIN_ROW_HEIGHT = 40;
    private static final double TITLE_OVERHEAD = 62;
    private static final double DEFAULT_HEADER_HEIGHT = 56;
    private static final double BOARD_PADDING = 32;

    private PublicPropertyBoardLayout() {
    }

    public static void applyEqualRows(VBox propertyPanel, int playerCount) {
        if (propertyPanel == null || playerCount <= 0) {
            return;
        }
        propertyPanel.setFillWidth(true);
        propertyPanel.setUserData(playerCount);
        double rowHeight = rowHeightFor(propertyPanel, playerCount);
        double gaps = propertyPanel.getSpacing() * Math.max(0, playerCount - 1);
        double stackHeight = rowHeight * playerCount + gaps;
        propertyPanel.setMinHeight(stackHeight);
        propertyPanel.setPrefHeight(stackHeight);
        propertyPanel.setMaxHeight(stackHeight);

        for (var child : propertyPanel.getChildren()) {
            if (!(child instanceof Region region)) {
                continue;
            }
            region.setMinHeight(rowHeight);
            region.setPrefHeight(rowHeight);
            region.setMaxHeight(rowHeight);
            VBox.setVgrow(region, javafx.scene.layout.Priority.NEVER);
        }
    }

    public static void applyEqualRows(VBox propertyPanel) {
        applyEqualRows(propertyPanel, playerCountFor(propertyPanel));
    }

    public static double rowHeightFor(VBox propertyPanel) {
        return rowHeightFor(propertyPanel, playerCountFor(propertyPanel));
    }

    public static double rowHeightFor(VBox propertyPanel, int playerCount) {
        if (propertyPanel == null || playerCount <= 0) {
            return ABSOLUTE_MIN_ROW_HEIGHT;
        }
        double usable = resolveUsableStackHeight(propertyPanel);
        if (usable <= 0) {
            return estimateRowHeight(propertyPanel, playerCount);
        }
        return divideStackHeight(usable, propertyPanel.getSpacing(), playerCount);
    }

    public static double estimateRowHeight(VBox propertyPanel, int playerCount) {
        if (propertyPanel == null || playerCount <= 0) {
            return ABSOLUTE_MIN_ROW_HEIGHT;
        }
        double usable = resolveUsableStackHeight(propertyPanel);
        if (usable <= 0) {
            return ABSOLUTE_MIN_ROW_HEIGHT;
        }
        return divideStackHeight(usable, propertyPanel.getSpacing(), playerCount);
    }

    public static CardView.CardMetrics cardMetricsForRow(double rowHeight) {
        double cardAreaHeight = Math.max(36, rowHeight - TITLE_OVERHEAD);
        double factor = Math.min(1.0, Math.max(0.22, cardAreaHeight / CardView.PUBLIC.slotH()));
        return CardView.PUBLIC.scaled(factor);
    }

    private static int playerCountFor(VBox propertyPanel) {
        if (propertyPanel == null) {
            return 0;
        }
        if (propertyPanel.getUserData() instanceof Integer count && count > 0) {
            return count;
        }
        return propertyPanel.getChildren().size();
    }

    private static double divideStackHeight(double stackHeight, double rowSpacing, int playerCount) {
        double gaps = rowSpacing * Math.max(0, playerCount - 1);
        double usable = Math.max(0, stackHeight - gaps);
        return usable / playerCount;
    }

    /** Usable height for property rows: table area minus board header/padding. */
    private static double resolveUsableStackHeight(VBox propertyPanel) {
        if (propertyPanel == null) {
            return 0;
        }
        double tableHeight = findTableAreaHeight(propertyPanel);
        if (tableHeight > 0) {
            return Math.max(0, tableHeight - boardOverhead(propertyPanel));
        }
        if (propertyPanel.getHeight() > 0) {
            return propertyPanel.getHeight();
        }
        Parent parent = propertyPanel.getParent();
        if (parent instanceof VBox boardPanel && boardPanel.getHeight() > 0) {
            return Math.max(0, boardPanel.getHeight() - boardOverhead(propertyPanel));
        }
        return 0;
    }

    private static double boardOverhead(VBox propertyPanel) {
        Parent parent = propertyPanel != null ? propertyPanel.getParent() : null;
        if (!(parent instanceof VBox boardPanel)) {
            return DEFAULT_HEADER_HEIGHT + BOARD_PADDING;
        }
        return headerHeight(boardPanel) + boardPanel.getSpacing() + BOARD_PADDING;
    }

    private static double findTableAreaHeight(VBox propertyPanel) {
        Parent node = propertyPanel;
        while (node != null) {
            if (node instanceof Region region
                    && node.getStyleClass().contains("table-area")
                    && region.getHeight() > 0) {
                return region.getHeight();
            }
            node = node.getParent();
        }
        return 0;
    }

    private static double headerHeight(VBox boardPanel) {
        if (boardPanel.getChildren().isEmpty()) {
            return DEFAULT_HEADER_HEIGHT;
        }
        var header = boardPanel.getChildren().get(0);
        if (header instanceof Region region && region.getHeight() > 0) {
            return region.getHeight();
        }
        return DEFAULT_HEADER_HEIGHT;
    }
}
