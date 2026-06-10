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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Factory for themed JavaFX dialogs shared by local and online controllers.
 * <p>
 * Applies {@code game-theme.css}, centers on the owning window, and provides
 * reusable choice and color-picker layouts for card-play prompts.
 */
public class GameDialogService {
    /** Label whose scene window becomes the dialog owner for modality. */
    private final Label ownerLabel;

    /**
     * @param ownerLabel any on-screen label from the game view (used for dialog ownership)
     */
    public GameDialogService(Label ownerLabel) {
        this.ownerLabel = ownerLabel;
    }

    /**
     * Shows a header/content dialog with custom button types.
     *
     * @return the button the user clicked, or empty if the dialog was dismissed
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
     * Shows a vertical list of choice buttons; selecting one closes the dialog with that value.
     *
     * @param labeler           text for each option button
     * @param colorStyleProvider optional inline CSS per option (may be {@code null})
     * @return the chosen option, or empty if cancelled or no options
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
     * Property-set color picker with rent-completion hints and color swatch styling.
     */
    public Optional<Color> showColorChoiceDialog(String title, String header, String prompt, List<Color> colors) {
        return showChoiceDialog(title, header, prompt, colors,
                color -> color + "  -  " + color.getSetSize() + " cards to complete",
                color -> "-fx-background-color: " + cssColorFor(color) + ";"
                        + "-fx-text-fill: " + textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    /**
     * Color picker tailored for playing a {@link WildpropertyCard} as property.
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

    /** Hex background color for dialog buttons representing a property color. */
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

    /** Contrasting text color for buttons on light or dark property swatches. */
    public String textColorFor(Color color) {
        return switch (color) {
            case YELLOW, LIGHT_BLUE, LIGHT_GREEN, ORANGE -> "#1f2d2a";
            default -> "white";
        };
    }

    /** Attaches game CSS, dialog style class, and window owner to a dialog pane. */
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
        if (ownerLabel != null && ownerLabel.getScene() != null) {
            dialog.initOwner(ownerLabel.getScene().getWindow());
        }
    }
}
