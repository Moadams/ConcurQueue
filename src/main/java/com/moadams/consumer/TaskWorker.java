package com.moadams.consumer;

import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
import com.moadams.util.TaskLogger;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskWorker implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates;
    private final AtomicInteger processedTaskCount;
    private final ConcurrentHashMap<UUID, Integer> retryCounts;
    private static  final int MAX_RETRIES = 3;

    public TaskWorker(BlockingQueue<Task> taskQueue, ConcurrentHashMap<UUID, TaskStatus> taskStates, AtomicInteger processedTaskCount, ConcurrentHashMap<UUID, Integer> retryCounts) {
        this.taskQueue = taskQueue;
        this.taskStates = taskStates;
        this.processedTaskCount = processedTaskCount;
        this.retryCounts = retryCounts;
    }

    @Override
    public void run() {
        TaskLogger.log("Worker " + Thread.currentThread().getName() + " started");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Task task =taskQueue.take();
                TaskLogger.log("Worker " + Thread.currentThread().getName() + " picked up " + task);

                taskStates.put(task.getId(), TaskStatus.PROCESSING);

                try{
                    long processingTime = ThreadLocalRandom.current().nextLong(200, 2001);
                    Thread.sleep(processingTime);

                    boolean failed = ThreadLocalRandom.current().nextInt(10) < 1;
                    if (failed && task.getRetryCount() < MAX_RETRIES){
                        task.incrementRetryCount();
                        retryCounts.put(task.getId(), task.getRetryCount());
                        taskStates.put(task.getId(), TaskStatus.FAILED);
                        taskQueue.put(task);
                        TaskLogger.logWarning("Worker " + Thread.currentThread().getName() +
                                " failed processing " + task + ". Retrying (" +
                                task.getRetryCount() + "/" + MAX_RETRIES + ").");
                    }else{
                        if(failed){
                            taskStates.put(task.getId(), TaskStatus.FAILED);
                            TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                                    " failed processing " + task + ". Max retries reached. Task abandoned.");
                        }else{
                            taskStates.put(task.getId(), TaskStatus.COMPLETED);
                            processedTaskCount.incrementAndGet();
                            TaskLogger.log("Worker " + Thread.currentThread().getName() + " completed " + task +
                                    " in " + processingTime + "ms. Total processed: " + processedTaskCount.get());
                        }
                        retryCounts.remove(task.getId());
                    }
                }
                catch (InterruptedException e){
                    TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                            " interrupted during processing of " + task.getName() + ": " + e.getMessage());
                    taskStates.put(task.getId(), TaskStatus.FAILED);
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                    TaskLogger.logError("Worker " + Thread.currentThread().getName() +
                            " encountered an unexpected error processing " + task.getName() + ": " + e.getMessage());
                    taskStates.put(task.getId(), TaskStatus.FAILED);
                }

            }
        } catch (Exception e) {
            TaskLogger.log("Worker " + Thread.currentThread().getName() + " interrupted and shutting down.");
            Thread.currentThread().interrupt();
        }
        TaskLogger.log("Worker " + Thread.currentThread().getName() + " stopped.");
    }
}
