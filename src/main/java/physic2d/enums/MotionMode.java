package physic2d.enums;

/**
 * Motion mode will affect physic interpretation of wall, floor and celling on the body.
 */
public enum MotionMode {
    /**
     * Apply for when walls, ceiling and floor are relevant.
     * In this mode, slope will affect the body motion (slowdown or accelerate).
     * This mode is suitable for side-scrolling games like platformers.
     */
    Grounded,

    /**
     * Apply for when there is no floor or ceiling. All collisions will be reported as {@code onWall}.
     * In this mode, slope has no effect on body motion (sliding speed is constant).
     * This mode is suitable for top-down games.
     */
    Floating
}
