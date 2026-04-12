package utility;

import utility.log.EngineLog;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IdPool is a pooling system designed to increment and dispatch unique value to a pool instance.
 * <p>
 * IdPool also support recycling the value by releasing it back to the pool,
 * which can be dispatch again via {@link #newId()}.
 */
public class IdPool {
    private static final EngineLog Logger = new EngineLog(IdPool.class);
    // Use for when respect floating point limit is enabled
    private static final int FloatPrecisionLimit = 16777216;
    private final int startValue;
    private final AtomicInteger nextId;
    private final ConcurrentLinkedQueue<Integer> discardedIds;
    private final boolean shouldRespectFloatPrecisionLimit;

    /**
     * Create a new {@link IdPool} with the given starting value.
     * @param startValue the number that the pool start incrementing from
     * @param respectFloatPrecisionLimit should the pool warn if the dispatching value pass Float precision limit
     */
    public IdPool (int startValue, boolean respectFloatPrecisionLimit) {
        this.startValue = startValue;
        nextId = new AtomicInteger(startValue);
        discardedIds = new ConcurrentLinkedQueue<>();
        shouldRespectFloatPrecisionLimit = respectFloatPrecisionLimit;
    }

    /**
     * Get a unique id from the pool.
     * If the pool has discarded IDs, they will be polled for dispatch again.
     * This is to prevent the needs to increment the pool to higher value.
     * <p>
     * If the pool has float precision limit respected,
     * there will be warning upon reaching this limit.
     * <p>
     * After a new id is created and used, also consider
     * releasing them back to the pool when done using.
     *
     * @return integer from the sequence.
     * @see IdPool#releaseId(int) Release ID back to the pool
     * @see IdPool#newIdOnly() Get a newly incremented ID
     */
    public int newId() {
        Integer id = discardedIds.poll();
        if (id == null) id = nextId.getAndIncrement();
        if (isFPLViolated(id)) {
            Logger.warning("ID '" + id + "' is beyond float point precision limit. \nCasting this value to float will cause lost of accuracy.");
        }
        return id;
    }

    /**
     * Get a unique id from the pool.
     * This will increment the poll value without recycling the discarded IDs, if there is any.
     * <p>
     * If the pool has float precision limit respected,
     * there will be warning upon reaching this limit.
     * <p>
     * After a new id is created and used, also consider
     * releasing them back to the pool when done using.
     *
     * @return integer from the sequence.
     * @see IdPool#releaseId(int) Release ID back to the pool
     */
    public int newIdOnly() {
        int id = nextId.getAndIncrement();
        if (isFPLViolated(id)) {
            Logger.warning("ID '" + id + "' is beyond float point precision limit. \nCasting this value to float will cause lost of accuracy.");
        }
        return id;
    }

    /**
     * Return an id to the pool for it to be used again.
     * Reduce the need to keep incrementing the counter to higher value.
     * @param id value to be released.
     */
    public void releaseId(int id) {
        if (id >= startValue && !discardedIds.contains(id)) discardedIds.offer(id);
    }

    /**
     * Check if a value had pass the precision limit of Float.
     * @param value the integer to check
     * @return true if {@link #shouldRespectFloatPrecisionLimit} is {@code true} and the value pass {@link #FloatPrecisionLimit}
     */
    private boolean isFPLViolated(int value) {
        if (shouldRespectFloatPrecisionLimit) return value >= FloatPrecisionLimit;
        return false;
    }

    /**
     * Reset tracking data of this ID pool to its original starting value.
     * <p>
     * <b>Warning:</b> this will cause collision if the pool is still in use and IDs are expected to be unique.
     */
    public void reset() {
        nextId.set(startValue);
        discardedIds.clear();
    }
}
