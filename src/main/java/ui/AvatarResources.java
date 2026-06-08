package ui;

import javafx.scene.image.Image;

import java.util.Objects;

public final class AvatarResources {
    private AvatarResources() {
    }

    public static Image loadDefaultAvatar(Class<?> resourceAnchor) {
        try {
            return new Image(Objects.requireNonNull(
                    resourceAnchor.getResourceAsStream("/ui/avatar.png")));
        } catch (Exception ex) {
            return null;
        }
    }
}
