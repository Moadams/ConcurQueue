package com.moadams.model;

import java.time.Instant;
import java.util.UUID;

public class Task implements Comparable<Task> {
    private final UUID id;
    private final String name;
    private final int priority;
    private final Instant createdTimestamp;
    private final String payload;
    private int retryCount;

    public Task(String name, int priority, String payload) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = Instant.now();
        this.payload = payload;
        this.retryCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public int compareTo(Task otherTask) {
        int priorityCompare = Integer.compare(priority, otherTask.priority);
        if (priorityCompare == 0) {
            return this.createdTimestamp.compareTo(otherTask.createdTimestamp);
        }
        return priorityCompare;
    }

    @Override
    public String toString() {
        return String.format("Task{id=%s, name='%s', priority=%d, retries=%d}",
                id.toString().substring(0, 8), name, priority, retryCount);
    }
}
