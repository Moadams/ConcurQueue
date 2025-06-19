package com.moadams.util;

import com.moadams.enums.TaskStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JsonExporter {
    public static void exportTaskStatuses(ConcurrentHashMap<UUID, TaskStatus> taskStates, String filePath){
        if(taskStates.isEmpty()){
            TaskLogger.log("No task statuses to export - taskStates is empty");
            return;
        }

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"export_timestamp\": \"").append(Instant.now().toString()).append("\",\n");
        jsonBuilder.append("  \"total_tasks\": ").append(taskStates.size()).append(",\n");
        jsonBuilder.append("  \"task_statuses\": {\n");

        int count = 0;
        for(Map.Entry<UUID, TaskStatus> entry : taskStates.entrySet()){
            jsonBuilder.append("    \"").append(entry.getKey().toString()).append("\": \"").append(entry.getValue().name()).append("\"");
            if(count < taskStates.size() - 1){
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
            count++;
        }
        jsonBuilder.append("  }\n");
        jsonBuilder.append("}\n");

        try(FileWriter file = new FileWriter(filePath)){
            file.write(jsonBuilder.toString());
            file.flush();
            TaskLogger.log("Task statuses exported to " + filePath + " (" + taskStates.size() + " tasks)");
        } catch (IOException e) {
            TaskLogger.logError("Failed to export task statuses to JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}