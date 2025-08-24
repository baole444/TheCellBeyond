package utility;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class IdPool {
    // Use for when respect floating point limit is enabled
    private static final int FLOAT_PRECISION_LIMIT = 16777216;

    private final int startValue;

    private final AtomicInteger nextId;

    private final ConcurrentLinkedQueue<Integer> discardedIds;

    private final boolean shouldRespectFloatPrecisionLimit;

    public IdPool (int startValue, boolean respectFloatPrecisionLimit) {
        this.startValue = startValue;
        nextId = new AtomicInteger(startValue);
        discardedIds = new ConcurrentLinkedQueue<>();
        shouldRespectFloatPrecisionLimit = respectFloatPrecisionLimit;
    }

    /**
     * Get a unique id from the pool.
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
    public int newId() {
        Integer id = discardedIds.poll();

        if (id == null) id = nextId.getAndIncrement();

        if (isFPLViolated(id)) {
            System.err.println("ID '" + id + "' is beyond float point precision limit.");
            System.err.println("Casting this value to float will cause lost of accuracy.");
        }

        return id;
    }

    /**
     * Return an id to the pool for it to be used again.
     * Reduce the need to keep incrementing the counter to higher value.
     * @param id value to be released.
     */
    public void releaseId(int id) {
        if (id > 0) {
            discardedIds.offer(id);
        }
    }

    // Check if
    private boolean isFPLViolated(int value) {
        if (shouldRespectFloatPrecisionLimit) return value >= FLOAT_PRECISION_LIMIT;

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
