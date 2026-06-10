package model.achievement;

/**
 * Immutable definition of a single unlockable achievement.
 *
 * @param id          stable identifier used for persistence and unlock checks
 * @param icon        emoji or symbol shown in the achievement UI
 * @param title       short display title
 * @param description human-readable unlock criteria
 */
public record Achievement(String id, String icon, String title, String description) {
}
