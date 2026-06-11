package controller.dialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.card.WildpropertyCard;
import model.enums.Color;
import ui.DialogFrameStyle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 本地与联机控制器共用的主题 JavaFX 对话框工厂。
 * <p>
 * 应用 {@code game-theme.css}，相对于所属窗口居中，
 * 并为出牌提示提供可复用的选择与颜色选择器布局。
 */
public class GameDialogService {
    /** 其场景窗口作为对话框模态所有者的标签。 */
    private final Label ownerLabel;

    /**
     * @param ownerLabel 游戏视图中任意屏幕标签（用于对话框所有权）
     */
    public GameDialogService(Label ownerLabel) {
        this.ownerLabel = ownerLabel;
    }

    /**
     * 显示带自定义按钮类型的标题/内容对话框。
     *
     * @return 用户点击的按钮；对话框被关闭时为空
     */
    public Optional<ButtonType> showButtonDialog(String title, String header, String content, ButtonType... buttons) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(buttons);

        VBox body = new VBox(10);
        body.getStyleClass().add("dialog-body");
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("dialog-header-label");
        Label contentLabel = new Label(content == null ? "" : content);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("dialog-content-label");
        body.getChildren().addAll(headerLabel, contentLabel);

        pane.setContent(body);
        dialog.setResultConverter(button -> button);
        styleDialog(dialog);
        return dialog.showAndWait();
    }

    /**
     * 显示纵向选项按钮列表；选择一项即以该值关闭对话框。
     *
     * @param labeler           各选项按钮文本
     * @param colorStyleProvider 各选项可选的内联 CSS（可为 {@code null}）
     * @return 所选选项；取消或无选项时为空
     */
    public <T> Optional<T> showChoiceDialog(String title, String header, String prompt,
                                            List<T> options, Function<T, String> labeler,
                                            Function<T, String> colorStyleProvider) {
        if (options == null || options.isEmpty()) {
            return Optional.empty();
        }

        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(cancel);
        dialog.setResultConverter(button -> null);

        VBox body = new VBox(12);
        body.getStyleClass().add("dialog-body");
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("dialog-header-label");
        Label promptLabel = new Label(prompt == null ? "" : prompt);
        promptLabel.setWrapText(true);
        promptLabel.getStyleClass().add("dialog-content-label");

        VBox choices = new VBox(8);
        choices.getStyleClass().add("dialog-choice-list");
        for (T option : options) {
            Button button = new Button(labeler.apply(option));
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("dialog-choice-button");
            String extraStyle = colorStyleProvider == null ? null : colorStyleProvider.apply(option);
            if (extraStyle != null && !extraStyle.isBlank()) {
                button.setStyle(extraStyle);
            }
            button.setOnAction(e -> {
                dialog.setResult(option);
                dialog.close();
            });
            choices.getChildren().add(button);
        }

        body.getChildren().addAll(headerLabel, promptLabel, choices);
        pane.setContent(body);
        styleDialog(dialog);
        return dialog.showAndWait();
    }

    /**
     * 地产套组颜色选择器，含租金/完成提示与色块样式。
     */
    public Optional<Color> showColorChoiceDialog(String title, String header, String prompt, List<Color> colors) {
        return showChoiceDialog(title, header, prompt, colors,
                color -> color + "  -  " + color.getSetSize() + " cards to complete",
                color -> "-fx-background-color: " + cssColorFor(color) + ";"
                        + "-fx-text-fill: " + textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    /**
     * 专为将 {@link WildpropertyCard} 作为地产打出而定制的颜色选择器。
     */
    public Optional<Color> showWildPropertyColorDialog(WildpropertyCard wild) {
        List<Color> colors = wild.getAvailableColors();
        if (colors.isEmpty()) {
            return Optional.empty();
        }
        int bankValue = wild.getBankValueM();
        String bankHint = wild.isBankable()
                ? "Deposit to bank is always " + bankValue + "M (not affected by color chosen)."
                : "This wild card cannot be deposited to bank.";
        return showChoiceDialog(
                "Wild Property Color",
                wild.getName(),
                "Choose a color to play as property.\n" + bankHint,
                colors,
                color -> color + "  —  play as " + color + " property",
                color -> "-fx-background-color: " + cssColorFor(color) + ";"
                        + "-fx-text-fill: " + textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    /** 对话框按钮表示地产颜色时使用的十六进制背景色。 */
    public String cssColorFor(Color color) {
        return switch (color) {
            case BROWN -> "#8B5A2B";
            case DARK_BLUE -> "#174EA6";
            case GREEN -> "#1B7F43";
            case ORANGE -> "#F2994A";
            case RED -> "#D64545";
            case YELLOW -> "#F2C94C";
            case BLACK -> "#2D3436";
            case LIGHT_BLUE -> "#7EC8E3";
            case LIGHT_GREEN -> "#6FCF97";
            case PINK -> "#E84393";
        };
    }

    /** 浅色或深色地产色块按钮上的对比文字颜色。 */
    public String textColorFor(Color color) {
        return switch (color) {
            case YELLOW, LIGHT_BLUE, LIGHT_GREEN, ORANGE -> "#1f2d2a";
            default -> "white";
        };
    }

    /** 为对话框面板附加游戏 CSS、对话框样式类与窗口所有者。 */
    public void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(getClass().getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().add("game-dialog");
        DialogFrameStyle.hideSystemFrame(dialog);
        if (ownerLabel != null && ownerLabel.getScene() != null) {
            dialog.initOwner(ownerLabel.getScene().getWindow());
        }
    }
}
