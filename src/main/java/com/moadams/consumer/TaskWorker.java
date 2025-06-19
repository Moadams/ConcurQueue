package com.moadams.consumer;

import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
import com.moadams.util.TaskLogger;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskWorker represents a consumer thread that fetches tasks from the shared queue,
 * simulates processing, updates task status, and tracks processed task count.
 * It also includes a retry mechanism for failed tasks.
 * This class now also demonstrates a potential deadlock scenario and its resolution.
 */
public class TaskWorker implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates;
    private final AtomicInteger processedTaskCount;
    private final ConcurrentHashMap<UUID, Integer> retryCounts;
    private final Object lockA;
    private final Object lockB;
    private final boolean introduceDeadlock;
    private static final int MAX_RETRIES = 3;

    /**
     * Constructs a TaskWorker.
     * @param taskQueue The shared blocking queue from which tasks are consumed.
     * @param taskStates A concurrent map to update the status of tasks.
     * @param processedTaskCount An atomic integer to safely track the total number of tasks processed.
     * @param retryCounts A concurrent map to store retry counts for tasks.
     * @param lockA Shared lock A for deadlock demonstration.
     * @param lockB Shared lock B for deadlock demonstration.
     * @param introduceDeadlock If true, workers will use conflicting lock orders (deadlock prone).
     * If false, workers will use a fixed lock order (deadlock resolved).
     */
    public TaskWorker(BlockingQueue<Task> taskQueue,
                      ConcurrentHashMap<UUID, TaskStatus> taskStates,
                      AtomicInteger processedTaskCount,
                      ConcurrentHashMap<UUID, Integer> retryCounts,
                      Object lockA, Object lockB, boolean introduceDeadlock) {
        this.taskQueue = taskQueue;
        this.taskStates = taskStates;
        this.processedTaskCount = processedTaskCount;
        this.retryCounts = retryCounts;
        this.lockA = lockA;
        this.lockB = lockB;
        this.introduceDeadlock = introduceDeadlock;
    }

    @Override
    public void run() {
        TaskLogger.log("Worker " + Thread.currentThread().getName() + " started.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Task task = taskQueue.take();
                TaskLogger.log("Worker " + Thread.currentThread().getName() + " picked up " + task);


                taskStates.put(task.getId(), TaskStatus.PROCESSING);

                try {
                    if (introduceDeadlock) {


                        if (task.getId().getLeastSignificantBits() % 2 == 0) {
                            synchronized (lockA) {
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_A for " + task.getName());
                                Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " attempting to acquire LOCK_B for " + task.getName());
                                synchronized (lockB) {
                                    TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_B for " + task.getName());

                                    simulateTaskProcessing(task);
                                }
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_B for " + task.getName());
                            }
                            TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_A for " + task.getName());
                        } else {
                            synchronized (lockB) {
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_B for " + task.getName());
                                Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " attempting to acquire LOCK_A for " + task.getName());
                                synchronized (lockA) {
                                    TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_A for " + task.getName());

                                    simulateTaskProcessing(task);
                                }
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_A for " + task.getName());
                            }
                            TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_B for " + task.getName());
                        }
                    } else {

                        synchronized (lockA) {
                            TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_A for " + task.getName());
                            Thread.sleep(ThreadLocalRandom.current().nextLong(50, 150));
                            TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " attempting to acquire LOCK_B for " + task.getName());
                            synchronized (lockB) {
                                TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " acquired LOCK_B for " + task.getName());

                                simulateTaskProcessing(task);
                            }
                            TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_B for " + task.getName());
                        }
                        TaskLogger.logLock("Worker " + Thread.currentThread().getName() + " released LOCK_A for " + task.getName());
                    }

                } catch (InterruptedException e) {
                    TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                            " interrupted during processing of " + task.getName() + ": " + e.getMessage());
                    taskStates.put(task.getId(), TaskStatus.FAILED);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                            " encountered an unexpected error processing " + task.getName() + ": " + e.getMessage());
                    taskStates.put(task.getId(), TaskStatus.FAILED);
                }
            }
        } catch (InterruptedException e) {
            TaskLogger.log("Worker " + Thread.currentThread().getName() + " interrupted and shutting down.");
            Thread.currentThread().interrupt();
        }
        TaskLogger.log("Worker " + Thread.currentThread().getName() + " stopped.");
    }

    /**
     * Simulates the actual task processing, including potential failure and retry logic.
     * This logic was extracted to be called after locks are acquired.
     * @param task The task to process.
     * @throws InterruptedException If the thread is interrupted during sleep.
     */
    private void simulateTaskProcessing(Task task) throws InterruptedException {

        long processingTime = ThreadLocalRandom.current().nextLong(200, 2001);
        Thread.sleep(processingTime);


        boolean failed = ThreadLocalRandom.current().nextInt(10) < 1; // 10% chance
        if (failed && task.getRetryCount() < MAX_RETRIES) {
            task.incrementRetryCount();
            retryCounts.put(task.getId(), task.getRetryCount());
            taskStates.put(task.getId(), TaskStatus.FAILED);
            try {
                taskQueue.put(task);
                TaskLogger.logWarning("Worker " + Thread.currentThread().getName() +
                        " failed processing " + task + ". Retrying (" +
                        task.getRetryCount() + "/" + MAX_RETRIES + ").");
            } catch (InterruptedException e) {
                TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                        " interrupted while re-queuing " + task.getName() + ": " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        } else {

            if (failed) {
                taskStates.put(task.getId(), TaskStatus.FAILED);
                TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                        " failed processing " + task + ". Max retries reached. Task abandoned.");
            } else {
                taskStates.put(task.getId(), TaskStatus.COMPLETED);
                processedTaskCount.incrementAndGet();
                TaskLogger.log("Worker " + Thread.currentThread().getName() + " completed " + task +
                        " in " + processingTime + "ms. Total processed: " + processedTaskCount.get());
            }
            retryCounts.remove(task.getId());
        }
    }
}