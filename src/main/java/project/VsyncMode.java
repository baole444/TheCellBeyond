package project;

/**
 * Vsync mode allow the engine to handle its event poll rate and render frame rate according to the host's display refresh rate.
 */
public enum VsyncMode {
    /**
     * Disable vsync entirely, no synchronization, buffer swaps happen immediately, uncapped by the display refresh rate.
     */
    Disabled,
    /**
     * Synchronize to the display refresh rate when the frame rate is at or above it, reduce screen tearing.
     * Synchronization is released to avoid stuttering when the frame rate dropped below the display refresh rate.
     * <p>
     * This will fall back to {@link #Enabled} if  the driver does not support adaptive sync.
     */
    Adaptive,
    /**
     * Synchronization happen on every buffer swap follow the display refresh rate, reduce screen tearing.
     */
    Enabled
}
