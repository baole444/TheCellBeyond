package TheCellBeyond.internal;

import java.util.Objects;

/**
 * Engine holds the {@link EngineHost} that is responsible for driving the engine.
 */
public final class Engine {
    private static EngineHost host;

    private Engine() {}

    /**
     * Register a host to drive the engine.
     * <p>
     * This must be call once before the window runs.
     * @param host the host to register with, cannot be null
     * @throws IllegalStateException if a host already registered
     */
    public static void host (EngineHost host) {
        if (Engine.host != null) throw new IllegalStateException("Cannot replace the engine host while the process is running");
        Engine.host = Objects.requireNonNull(host, "Engine host can't be null");
    }

    /**
     * Get the currently registered host.
     * @return the host this is in control
     * @throws IllegalStateException if no host has been registered
     */
    public static EngineHost host() {
        if (host == null) throw new IllegalStateException("No engine host registered, missing Engine.host(...) call before the window runs");
        return host;
    }
}
