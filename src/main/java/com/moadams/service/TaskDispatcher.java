package com.moadams.service;

import com.moadams.consumer.TaskWorker;
import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
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

    public TaskDispatcher(int workerPoolSize, int queueCapacity) {
        this.taskQueue = new PriorityBlockingQueue<>(queueCapacity);
        this.workerPool = Executors.newFixedThreadPool(workerPoolSize);
        this.taskStates = new ConcurrentHashMap<>();
        this.processedTaskCount = new AtomicInteger(0);
        this.retryCounts = new ConcurrentHashMap<>();
        TaskLogger.log("TaskDispatcher initialized with " + workerPoolSize + " worker threads and queue capacity " + queueCapacity);
    }

    public void startWorkers(){
        TaskLogger.log("Starting worker threads");
        for (int i = 0; i < ((ThreadPoolExecutor) workerPool).getCorePoolSize(); i++) {
            workerPool.submit(new TaskWorker(taskQueue, taskStates, processedTaskCount, retryCounts));
        }
        TaskLogger.log(
                "Worker pool with " + ((ThreadPoolExecutor) workerPool).getCorePoolSize() + " worker threads started"
        );
    }

    public void shutdown(){
        TaskLogger.log("Initiating graceful shutdown");

        workerPool.shutdown();
        try {
            if(!workerPool.awaitTermination(30, TimeUnit.SECONDS)){
                TaskLogger.logWarning("Worker pool did not terminate in 30 seconds. Forcibly shutting down.");
                workerPool.shutdownNow();
                if(!workerPool.awaitTermination(10, TimeUnit.SECONDS)){
                    TaskLogger.logError("Worker pool did not terminate");
                }
            }
        }catch (InterruptedException e){
            TaskLogger.logError("Shutdown interrupted: " + e.getMessage());
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if(!taskQueue.isEmpty()){
            TaskLogger.logWarning("Draining " + taskQueue.size() + " remaining tasks from the queue...");
            taskQueue.forEach(task -> {
                TaskLogger.log("Task " + task.getId().toString().substring(0,8) + " was still in queue (Status: " + taskStates.get(task.getId()) + ")");
            });
            taskQueue.clear();
        }

        TaskLogger.log("ConcurQueue system shut down successfully");
    }

    public BlockingQueue<Task> getTaskQueue() {
        return taskQueue;
    }

    public ExecutorService getWorkerPool() {
        return workerPool;
    }

    public ConcurrentHashMap<UUID, TaskStatus> getTaskStates() {
        return taskStates;
    }

    public AtomicInteger getProcessedTaskCount() {
        return processedTaskCount;
    }
}
