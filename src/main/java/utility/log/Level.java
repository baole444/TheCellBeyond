package utility.log;

/**
 * Level enums define 4 log level with priority and prefix.
 */
public enum Level {
    /**
     * Debugging log level.
     */
    Debug(0, "DEBUG"),
    /**
     * Information log level.
     */
    Info(1, "INFO"),
    /**
     * Warning log level.
     */
    Warning(2, "WARN"),
    /**
     * Error log level.
     */
    Error(3, "ERROR");

    /**
     * The priority of the level.
     */
    public final int priority;
    /**
     * The prefix string for the level.
     */
    public final String prefix;

    Level(int priority, String prefix) {
        this.priority = priority;
        this.prefix = prefix;
    }
}
