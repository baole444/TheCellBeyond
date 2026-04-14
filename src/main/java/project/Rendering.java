package project;

/**
 *
 * @param enableVsync
 * @param targetFrameRate
 */
public record Rendering(boolean enableVsync, int targetFrameRate) {
}
