package ui;

import javafx.scene.image.Image;

import java.util.Objects;

/**
 * Loads the default player avatar image from the classpath.
 * <p>
 * Avatar images are shared by sidebar and public-board player renderers.
 */
public final class AvatarResources {
    private AvatarResources() {
    }

    /**
     * Loads {@code /ui/avatar.png} using {@code resourceAnchor} for classpath lookup.
     *
     * @return the avatar image, or {@code null} if loading fails
     */
    public static Image loadDefaultAvatar(Class<?> resourceAnchor) {
        try {
            return new Image(Objects.requireNonNull(
                    resourceAnchor.getResourceAsStream("/ui/avatar.png")));
        } catch (Exception ex) {
            return null;
        }
    }
}
