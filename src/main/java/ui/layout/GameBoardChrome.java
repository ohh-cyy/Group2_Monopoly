package ui.layout;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Collapsible sidebars and expandable hand dock shared by local and online game views. */
public final class GameBoardChrome {
    private static final double HAND_DOCK_HEIGHT = 266;
    private static final double HAND_DOCK_PEEK = 54;

    private final VBox leftSidebar;
    private final VBox rightSidebar;
    private final Button leftSidebarToggle;
    private final Button rightSidebarToggle;
    private final Button leftSidebarHandle;
    private final Button rightSidebarHandle;
    private final VBox handDock;
    private final Label handDockHint;
    private final Button handDockToggle;

    private boolean handDockExpanded;

    public GameBoardChrome(VBox leftSidebar,
                           VBox rightSidebar,
                           Button leftSidebarToggle,
                           Button rightSidebarToggle,
                           Button leftSidebarHandle,
                           Button rightSidebarHandle,
                           VBox handDock,
                           Label handDockHint,
                           Button handDockToggle) {
        this.leftSidebar = leftSidebar;
        this.rightSidebar = rightSidebar;
        this.leftSidebarToggle = leftSidebarToggle;
        this.rightSidebarToggle = rightSidebarToggle;
        this.leftSidebarHandle = leftSidebarHandle;
        this.rightSidebarHandle = rightSidebarHandle;
        this.handDock = handDock;
        this.handDockHint = handDockHint;
        this.handDockToggle = handDockToggle;
    }

    public VBox handDock() {
        return handDock;
    }

    public void setup() {
        setupCollapsibleSidebars();
        setupHandDockInteractions();
    }

    private void setupCollapsibleSidebars() {
        setLeftSidebarOpen(false);
        setRightSidebarOpen(false);
        if (leftSidebarToggle != null) {
            leftSidebarToggle.setOnAction(e -> setLeftSidebarOpen(false));
        }
        if (leftSidebarHandle != null) {
            leftSidebarHandle.setOnAction(e -> setLeftSidebarOpen(true));
        }
        if (rightSidebarToggle != null) {
            rightSidebarToggle.setOnAction(e -> setRightSidebarOpen(false));
        }
        if (rightSidebarHandle != null) {
            rightSidebarHandle.setOnAction(e -> setRightSidebarOpen(true));
        }
    }

    private void setLeftSidebarOpen(boolean open) {
        setNodeLayoutVisible(leftSidebar, open);
        setNodeLayoutVisible(leftSidebarHandle, !open);
    }

    private void setRightSidebarOpen(boolean open) {
        setNodeLayoutVisible(rightSidebar, open);
        setNodeLayoutVisible(rightSidebarHandle, !open);
    }

    private static void setNodeLayoutVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setupHandDockInteractions() {
        if (handDock == null) {
            return;
        }
        setHandDockExpanded(false, false);
        if (handDockToggle != null) {
            handDockToggle.setOnAction(e -> setHandDockExpanded(!handDockExpanded, true));
        }
    }

    private void setHandDockExpanded(boolean expanded, boolean animate) {
        if (handDock == null || (handDockExpanded == expanded && animate)) {
            return;
        }
        handDockExpanded = expanded;
        double collapsedY = HAND_DOCK_HEIGHT - HAND_DOCK_PEEK;
        double targetY = expanded ? 0 : collapsedY;
        if (handDockToggle != null) {
            handDockToggle.setText(expanded ? "Hide Hand ▼" : "Show Hand ▲");
        }
        if (handDockHint != null) {
            handDockHint.setText(expanded
                    ? "Double-click a card to play"
                    : "Click Show Hand to view your cards");
        }
        if (expanded) {
            handDock.toFront();
        }
        if (!animate) {
            handDock.setTranslateY(targetY);
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(280), handDock);
        transition.setToY(targetY);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }
}
