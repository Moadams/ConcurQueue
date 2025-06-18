package com.moadams.model;

import java.time.Instant;
import java.util.UUID;

public class Task implements Comparable<Task> {
    private final UUID id;
    private final String name;
    private final int priority;
    private final Instant createdTimestamp;
    private final String payload;
    private final int retryCount;

    public Task(UUID id, String name, int priority, Instant createdTimestamp, String payload, int retryCount) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = createdTimestamp;
        this.payload = payload;
        this.retryCount = retryCount;
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

    @Override
    public int compareTo(Task otherTask) {
        int priorityCompare = Integer.compare(priority, otherTask.priority);
        if (priorityCompare == 0) {
            return this.createdTimestamp.compareTo(otherTask.createdTimestamp);
        }
        return priorityCompare;
    }
}
