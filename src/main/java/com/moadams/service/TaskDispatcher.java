package com.moadams.service;

import com.moadams.consumer.TaskWorker;
import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
import com.moadams.producer.TaskProducer;
import com.moadams.util.TaskLogger;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDispatcher {
    private final BlockingQueue<Task> taskQueue;
    private final ExecutorService workerPool;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates;
    private final AtomicInteger processedTaskCount;
    private final ConcurrentHashMap<UUID, Integer> retryCounts;
    private final Object lockA;
    private final Object lockB;
    private final boolean introduceDeadlock;


    /**
     * Constructs a TaskDispatcher.
     * @param workerPoolSize The number of worker threads in the pool.
     * @param queueCapacity The maximum capacity of the task queue (bounded queue).
     * @param lockA A shared lock object for demonstration purposes.
     * @param lockB A shared lock object for demonstration purposes.
     * @param introduceDeadlock A flag to indicate whether workers should use conflicting lock order (true)
     * or a fixed, safe order (false).
     */
    public TaskDispatcher(int workerPoolSize, int queueCapacity, Object lockA, Object lockB, boolean introduceDeadlock) {

        this.taskQueue = new PriorityBlockingQueue<>(queueCapacity);
        this.workerPool = Executors.newFixedThreadPool(workerPoolSize);
        this.taskStates = new ConcurrentHashMap<>();
        this.processedTaskCount = new AtomicInteger(0);
        this.retryCounts = new ConcurrentHashMap<>();
        this.lockA = lockA;
        this.lockB = lockB;
        this.introduceDeadlock = introduceDeadlock;
        TaskLogger.log("TaskDispatcher initialized with " + workerPoolSize +
                " worker threads and queue capacity " + queueCapacity + ".");
    }

    /**
     * Starts the specified number of worker threads.
     * Each worker will continuously try to fetch and process tasks from the queue.
     */
    public void startWorkers() {
        TaskLogger.log("Starting worker threads...");
        for (int i = 0; i < ((ThreadPoolExecutor) workerPool).getCorePoolSize(); i++) {

            workerPool.submit(new TaskWorker(taskQueue, taskStates, processedTaskCount, retryCounts, lockA, lockB, introduceDeadlock));
        }
        TaskLogger.log(
                "Worker pool with " + ((ThreadPoolExecutor) workerPool).getCorePoolSize() + " threads started."
        );
    }

    /**
     * Starts a task producer thread.
     * @param producerName The name of the producer.
     * @param tasksToGenerate The total number of tasks this producer will generate.
     * @param generationIntervalMillis The delay between generating each task.
     */
    public void startProducer(String producerName, int tasksToGenerate, long generationIntervalMillis) {
        TaskLogger.log("Starting producer: " + producerName);
        new Thread(new TaskProducer(taskQueue, taskStates, producerName, tasksToGenerate, generationIntervalMillis), producerName).start();
    }

    /**
     * Initiates a graceful shutdown of the system.
     * It first shuts down the worker pool, then attempts to drain remaining tasks
     * from the queue, and finally logs the shutdown status.
     */
    public void shutdown() {
        TaskLogger.log("Initiating graceful shutdown...");


        workerPool.shutdown();
        try {

            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                TaskLogger.logWarning("Worker pool did not terminate in 30 seconds. Forcibly shutting down.");
                workerPool.shutdownNow();

                if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    TaskLogger.logError("Worker pool did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            TaskLogger.logError("Shutdown interrupted: " + e.getMessage());
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }


        if (!taskQueue.isEmpty()) {
            TaskLogger.logWarning("Draining " + taskQueue.size() + " remaining tasks from the queue...");
            taskQueue.forEach(task -> {
                TaskLogger.log("Task " + task.getId().toString().substring(0,8) + " was still in queue (Status: " + taskStates.get(task.getId()) + ")");

            });
            taskQueue.clear();
        }

        TaskLogger.log("ConcurQueue system shut down successfully.");
    }

    /**
     * Gets the shared task queue.
     * @return The BlockingQueue of tasks.
     */
    public BlockingQueue<Task> getTaskQueue() {
        return taskQueue;
    }

    /**
     * Gets the ExecutorService managing the worker threads.
     * @return The ExecutorService.
     */
    public ExecutorService getWorkerPool() {
        return workerPool;
    }

    /**
     * Gets the concurrent map tracking task states.
     * @return The ConcurrentHashMap of task UUIDs to TaskStatus.
     */
    public ConcurrentHashMap<UUID, TaskStatus> getTaskStates() {
        return taskStates;
    }

    /**
     * Gets the atomic integer tracking the total processed task count.
     * @return The AtomicInteger processed task count.
     */
    public AtomicInteger getProcessedTaskCount() {
        return processedTaskCount;
    }
}
