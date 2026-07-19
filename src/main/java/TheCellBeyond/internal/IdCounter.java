package TheCellBeyond.internal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * IdCounter is an incrementing system designed to dispatch unique value to a counter instance.
 * <p>
 * Dispatched value cannot be recycled, use {@link utility.IdPool} to keep dispatched value reclaimable.
 */
final class IdCounter {
    private final AtomicInteger nextId;

    /**
     * Create a new {@link IdCounter} with the given starting value.
     * @param startValue the number that the counter start incrementing from
     */
    IdCounter(int startValue) {
        nextId = new AtomicInteger(startValue);
    }

    /**
     * Get the unique id from the counter.
     * @return integer from the sequence, never one that was dispatched before
     */
    int newId() {
        return nextId.getAndIncrement();
    }
}
