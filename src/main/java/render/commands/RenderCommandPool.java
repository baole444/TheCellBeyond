package render.commands;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public class RenderCommandPool {
    private static final RenderCommandPool instance = new RenderCommandPool();
    private final ConcurrentHashMap<Class<?>, ConcurrentLinkedDeque<RenderCommand>> pools = new ConcurrentHashMap<>();

    private RenderCommandPool() {}

    static RenderCommandPool get() {
        return instance;
    }

    /**
     * Acquire a free command from the pool.
     * This allows reusing command to reduce allocation pressure.
     * @param commandType the class of the command
     * @param factory the function to use if there is no command available,
     *                it should provide a {@link RenderCommand} of some kind
     * @return a {@link RenderCommand} from the pool or a new command given from the factory
     * @param <T> Subclass of {@link RenderCommand}
     * @apiNote Let hopes this does not become {@code AbstractFactoryProviderFactory}.
     */
    <T extends RenderCommand> T acquire(Class<T> commandType, Supplier<T> factory) {
        RenderCommand command = pools.computeIfAbsent(commandType, _ -> new ConcurrentLinkedDeque<>()).pollFirst();
        return command != null ? commandType.cast(command) : factory.get();
    }

    /**
     * Acquire a command from the pool and copy the source's data into it.
     * @param sourced the command to copy from
     * @return a {@link RenderCommand} from the pool with source's data
     */
    RenderCommand acquireCopy(RenderCommand sourced) {
        if (sourced == null) return null;
        RenderCommand clone = sourced.acquireInstance();
        clone.copyFrom(sourced);
        return clone;
    }

    /**
     * Dispose a command back to the pool.
     * This will remove the chain and reset the command ownership.
     * The command is offered at the last entry in pool.
     * @param command the command to return
     */
    void release(RenderCommand command) {
        if (command == null) return;
        command.next = null;
        command.reset();
        pools.computeIfAbsent(command.getClass(), _ -> new ConcurrentLinkedDeque<>()).offerLast(command);
    }
}
