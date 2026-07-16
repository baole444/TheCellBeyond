package utility;

import scripting.API;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

@API
public class RingBuffer<T> {
    private final AtomicReferenceArray<T> buffer;
    private final int capacity;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger size = new AtomicInteger(0);

    public RingBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Invalid buffer capacity of " + capacity + ". Must be at least 1");

        this.capacity = capacity;
        buffer = new AtomicReferenceArray<>(capacity);
    }

    public void add(T element) {
        if (element == null) throw new NullPointerException("Cannot add null element to ring buffer");

        int i = writeIndex.getAndIncrement() % capacity;
        buffer.set(i, element);

        int currentSize;
        do {
            currentSize = size.get();
            if (currentSize >= capacity) break;
        } while (!size.compareAndSet(currentSize, currentSize + 1));
    }

    public List<T> toList() {
        int currentSize = size.get();
        int currentIndex = writeIndex.get();

        if (currentSize == 0) return new ArrayList<>();

        List<T> result = new ArrayList<>(currentSize);
        int start = currentSize < capacity ? 0 : currentIndex % capacity;
        for (int i = 0; i < currentSize; i++) {
            int index = (start + i) % capacity;
            T e = buffer.get(index);
            if (e == null) continue;
            result.add(e);
        }

        return result;
    }

    public T get(int index) {
        int currentSize = size.get();

        if (index < 0 || index >= currentSize) throw new IndexOutOfBoundsException("Accessing index " + index + ". Possible index 0-" + (currentSize - 1));

        int currentIndex = writeIndex.get();
        int start = currentSize < capacity ? 0 : currentIndex % capacity;
        index = (start + index) % capacity;

        return buffer.get(index);
    }

    public int size() {
        return Math.min(size.get(), capacity);
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return size.get() == 0;
    }

    public boolean isFull() {
        return size.get() >= capacity;
    }

    public void clear() {
        size.set(0);
        writeIndex.set(0);
        for (int i = 0; i < capacity; i++) {
            buffer.set(i, null);
        }
    }
}
