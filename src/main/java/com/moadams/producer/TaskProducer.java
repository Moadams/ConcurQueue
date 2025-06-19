package com.moadams.producer;

import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
import com.moadams.util.TaskLogger;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TaskProducer simulates clients submitting tasks to the ConcurQueue
 * Each producer can generate tasks with varying priorities and payloads
 */
public class TaskProducer implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates;
    private final String producerName;
    private final int tasksToGenerate;
    private final long generationIntervalMillis;

    public TaskProducer(BlockingQueue<Task> taskQueue, ConcurrentHashMap<UUID, TaskStatus> taskStates, String producerName, int tasksToGenerate, long generationIntervalMillis) {
        this.taskQueue = taskQueue;
        this.taskStates = taskStates;
        this.producerName = producerName;
        this.tasksToGenerate = tasksToGenerate;
        this.generationIntervalMillis = generationIntervalMillis;
    }

    @Override
    public void run() {
        TaskLogger.log("Producer " + producerName + " started");
        for (int i = 0; i < tasksToGenerate; i++) {
            int priority;
            String taskName;
            if (producerName.contains("HighPriority")) {
                priority = ThreadLocalRandom.current().nextInt(1, 4);
                taskName = "UrgentTask-" + producerName + "-" + (i + 1);
            }else if (producerName.contains("LowPriority")) {
                priority = ThreadLocalRandom.current().nextInt(5,10);
                taskName = "RoutineTask-" + producerName + "-" + (i + 1);
            }else{
                priority = ThreadLocalRandom.current().nextInt(1, 10);
                taskName = "MixedTask-" + producerName + "-" + (i + 1);
            }

            String payload = "Data for " + taskName + " (Priority: " + priority + ")";
            Task task = new Task(taskName, priority, payload);

            try{
                taskQueue.put(task);
                taskStates.put(task.getId(), TaskStatus.SUBMITTED);
                TaskLogger.log(producerName + " submitted " + task.getName());
                Thread.sleep(generationIntervalMillis);
            }catch(InterruptedException e){
                TaskLogger.logError(producerName + " interrupted while submitting task " + task.getName());
                Thread.currentThread().interrupt();
                break;
            }
        }
        TaskLogger.log(producerName + " finished generating " + tasksToGenerate + " tasks");
    }
}
