package com.holub.life;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Retains completed commands; callers share this monitor for related board changes. */
public final class CommandHistory {
    private static final int DEFAULT_CAPACITY = 10;

    private final Deque<Command> completed = new ArrayDeque<>();
    private final Deque<Command> undone = new ArrayDeque<>();
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
        undone.clear();
        completed.addLast(command);
        if (completed.size() > capacity) {
            completed.removeFirst();
        }
    }

    public synchronized int size() {
        return completed.size();
    }

    public synchronized boolean undo() {
        Command command = completed.peekLast();
        if (command == null) {
            return false;
        }
        command.undo();
        completed.removeLast();
        undone.addLast(command);
        return true;
    }

    public synchronized boolean redo() {
        Command command = undone.peekLast();
        if (command == null) {
            return false;
        }
        command.redo();
        undone.removeLast();
        completed.addLast(command);
        return true;
    }

    public synchronized void clear() {
        completed.clear();
        undone.clear();
    }
}
