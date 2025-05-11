package threading;

import threading.states.GameState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manage the triple buffer system of game state.
 * This class handles rotation of the buffers between writing, ready, and rendering states.
 */
public class TripleBufferManager {
    private final GameState[] buffers = new GameState[3];

    private final AtomicInteger writeBufferIndex = new AtomicInteger(0);
    private final AtomicInteger readyBufferIndex = new AtomicInteger(-1);
    private final AtomicInteger renderBufferIndex = new AtomicInteger(-1);

    // Frame sequence tracking
    private final AtomicLong latestFrameNumber = new AtomicLong(0);

    // Lock for accessing buffers
    private final ReentrantLock bufferLock = new ReentrantLock();

    public TripleBufferManager() {
        for (int i = 0; i < 3; i++) {
            buffers[i] = new GameState();
        }
    }

    /**
     * Get write buffer for the game logic thread to write to.
     * @return the current write buffer.
     */
    public GameState getWriteBuffer() {
        return buffers[writeBufferIndex.get()];
    }

    /**
     * Called when the game logic thread completed a frame.
     * @param frameNumber the completed frame number.
     * @return the next write buffer.
     */
    public GameState completeGameLogicUpdate(long frameNumber) {
        bufferLock.lock();
        try {
            int currentWriteIndex = writeBufferIndex.get();

            latestFrameNumber.set(frameNumber);

            readyBufferIndex.set(currentWriteIndex);

            int nextWriteIndex = findAvailableBufferIndex();
            writeBufferIndex.set(nextWriteIndex);

            return buffers[nextWriteIndex];
        } finally {
            bufferLock.unlock();
        }
    }

    /**
     * Called by the render thread to get the latest completed buffer.
     * @return the buffer to render from.
     */
    public GameState beginRendering() {
        bufferLock.lock();
        try {
            int readyIndex = readyBufferIndex.get();

            // When no buffer available, reuse current render buffer
            if (readyIndex == -1) {
                int currentRenderIndex = renderBufferIndex.get();

                return currentRenderIndex != -1 ? buffers[currentRenderIndex] : null;
            }

            // Swap ready buffer to render buffer
            int oldRenderIndex = renderBufferIndex.getAndSet(readyIndex);

            // Clear ready buffer flag
            readyBufferIndex.compareAndSet(readyIndex, -1);

            return buffers[readyIndex];
        } finally {
            bufferLock.unlock();
        }
    }

    /**
     * Find a buffer not currently in use.
     * @return the index of the available buffer.
     */
    private int findAvailableBufferIndex() {
        int readyIndex = readyBufferIndex.get();
        int renderIndex = renderBufferIndex.get();

        // Find the buffer that is not ready or in render.
        if (0 != readyIndex && 0 != renderIndex) return 0;
        if (1 != readyIndex && 1 != renderIndex) return 1;
        return 2;

        // Technically, there would be 1 buffer available, but if somehow there is a chance
        // for all buffers to be busy, return write buffer index.
        // Then roll the if statement into a for loop.
    }

    /**
     * Get the most recent frame number.
     * @return the latest frame number.
     */
    public long getLatestFrameNumber() {
        return latestFrameNumber.get();
    }
}
