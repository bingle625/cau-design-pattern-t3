package com.holub.life;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Retains completed commands; callers share this monitor for related board changes. */
public final class CommandHistory {
    private static final int DEFAULT_CAPACITY = 10;

    private final Deque<Command> completed = new ArrayDeque<>();
    private final int capacity;

    public CommandHistory() {
        this(DEFAULT_CAPACITY);
    }

    public CommandHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized void execute(Command command) {
        Objects.requireNonNull(command, "command").execute();
        completed.addLast(command);
        if (completed.size() > capacity) {
            completed.removeFirst();
        }
    }

    public synchronized int size() {
        return completed.size();
    }

    public synchronized void clear() {
        completed.clear();
    }
}
