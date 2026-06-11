package testsupport;

import controller.dialog.GameDialogService;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Scripted {@link GameDialogService} for service-layer unit tests. */
/** 测试工具：为需要 JavaFX 对话框的单元测试提供 Stub 弹窗。 */
public final class StubGameDialogService extends GameDialogService {
    private final Deque<Object> choiceQueue = new ArrayDeque<>();
    private final Deque<String> buttonTextQueue = new ArrayDeque<>();

    public StubGameDialogService() {
        super(new Label());
    }

    public static void initJavaFxOnce() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // Toolkit already started by another test class.
        }
    }

    public <T> void enqueueChoice(T choice) {
        choiceQueue.add(Optional.of(choice));
    }

    public void enqueueEmptyChoice() {
        choiceQueue.add(Optional.empty());
    }

    public void enqueueButtonText(String buttonText) {
        buttonTextQueue.add(buttonText);
    }

    @Override
    public <T> Optional<T> showChoiceDialog(String title,
                                            String header,
                                            String prompt,
                                            List<T> options,
                                            Function<T, String> labeler,
                                            Function<T, String> colorStyleProvider) {
        return popChoice();
    }

    @Override
    public Optional<ButtonType> showButtonDialog(String title,
                                                 String header,
                                                 String content,
                                                 ButtonType... buttons) {
        if (buttonTextQueue.isEmpty()) {
            return Optional.empty();
        }
        String want = buttonTextQueue.removeFirst();
        for (ButtonType button : buttons) {
            if (want.equals(button.getText())) {
                return Optional.of(button);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> popChoice() {
        if (choiceQueue.isEmpty()) {
            return Optional.empty();
        }
        Object next = choiceQueue.removeFirst();
        if (next instanceof Optional<?> optional) {
            return (Optional<T>) optional;
        }
        return Optional.of((T) next);
    }
}
