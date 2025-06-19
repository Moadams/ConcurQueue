package com.moadams.service;

import com.moadams.model.Task;
import com.moadams.model.TaskStatus;
import com.moadams.util.JsonExporter;
import com.moadams.util.TaskLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskMonitor periodically logs system metrics like queue size, worker pool status,
 * and processed task count. It can also detect stalled tasks and export task status to JSON.
 */
public class TaskMonitor implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final ExecutorService workerPool;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates;
    private final AtomicInteger processedTaskCount;
    private final long monitorIntervalMillis;
    private final String exportFilePath;

    public TaskMonitor(BlockingQueue<Task> taskQueue, ExecutorService workerPool, ConcurrentHashMap<UUID, TaskStatus> taskStates, AtomicInteger processedTaskCount, long monitorIntervalMillis, String exportFilePath) {
        this.taskQueue = taskQueue;
        this.workerPool = workerPool;
        this.taskStates = taskStates;
        this.processedTaskCount = processedTaskCount;
        this.monitorIntervalMillis = monitorIntervalMillis;
        this.exportFilePath = exportFilePath;
    }



    @Override
    public void run() {
        TaskLogger.log("TaskMonitor started");
        long lastExportTime =System.currentTimeMillis();

        try{
            while(!Thread.currentThread().isInterrupted()){
                Thread.sleep(monitorIntervalMillis);
                int queueSize = taskQueue.size();
                int activeWorkers = 0;
                int completedWorkers = 0;
                int totalWorkers = 0;

                if(workerPool instanceof ThreadPoolExecutor){
                    ThreadPoolExecutor tpe = (ThreadPoolExecutor) workerPool;
                    activeWorkers = tpe.getActiveCount();
                    completedWorkers = (int) tpe.getCompletedTaskCount();
                    totalWorkers = tpe.getPoolSize();
                }

                TaskLogger.log("MONITOR - Queue Size: " + queueSize +
                        " | Active Workers: " + activeWorkers + "/" + totalWorkers +
                        " | Processed Tasks (Total): " + processedTaskCount.get() +
                        " | Task Statuses: " + getStatusSummary());

                detectStalledTasks();

                if(exportFilePath != null && System.currentTimeMillis() - lastExportTime >= 60000){
                    JsonExporter.exportTaskStatuses(taskStates, exportFilePath);
                    lastExportTime = System.currentTimeMillis();
                    TaskLogger.log("MONITOR - Exported task statuses to " + exportFilePath);
                }
            }
        } catch (InterruptedException e) {
            TaskLogger.log("TaskMonitor interrupted and shutting down");
            Thread.currentThread().interrupt();
        }
        TaskLogger.log("TaskMonitor stopped");
    }

    private String getStatusSummary(){
        Map<TaskStatus, Long> statusCounts = new ConcurrentHashMap<>();
        for (TaskStatus status : taskStates.values()) {
            statusCounts.put(status, 0L);
        }
        taskStates.values().forEach(status -> statusCounts.merge(status, 1L, Long::sum));

        StringBuilder sb = new StringBuilder();
        statusCounts.forEach((status, count) -> {
            if(count > 0){
                sb.append(status.name()).append(":").append(count).append(" ");
            }
        });
        return sb.toString().trim();
    }

    private void detectStalledTasks(){
        long stalledThresholdMillis = 5000;
        long processingTasks = taskStates.values().stream().filter(status -> status == TaskStatus.PROCESSING).count();
        if (processingTasks > 0 && taskQueue.isEmpty() && ((ThreadPoolExecutor)workerPool).getActiveCount() == 0) {

            TaskLogger.logWarning("MONITOR - Potential system stall detected! " + processingTasks +
                    " tasks in PROCESSING state, but queue is empty and no active workers.");
        }
    }
}
