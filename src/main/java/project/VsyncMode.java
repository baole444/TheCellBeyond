package project;

/**
 * VsyncMode enums provide 3 vsync behaviour for the engine.
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
