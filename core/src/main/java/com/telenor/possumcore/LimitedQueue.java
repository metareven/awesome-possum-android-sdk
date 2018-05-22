package com.telenor.possumcore;

import java.util.LinkedList;

/**
 * Circular FIFO removing first element when new comes in, conserving the last elements added
 * @param <E>
 */
public class LimitedQueue<E> extends LinkedList<E> {
    private int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
        if (limit <= 0) throw new IllegalArgumentException("Minimum size is 1");
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) { super.remove(0); }
        return true;
    }
}