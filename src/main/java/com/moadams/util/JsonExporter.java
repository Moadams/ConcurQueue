package com.moadams.util;

import com.moadams.model.TaskStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JsonExporter {
    public static void exportTaskStatuses(ConcurrentHashMap<UUID, TaskStatus> taskStates, String filePath){
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        int count = 0;
        for(Map.Entry<UUID, TaskStatus> entry : taskStates.entrySet()){
            jsonBuilder.append(" \"").append(entry.getKey().toString()).append("\": \"").append(entry.getValue().name()).append("\"");
            if(count < taskStates.size() - 1){
                jsonBuilder.append(",\n");
            }else{
                jsonBuilder.append("\n");
            }
            count++;
        }
        jsonBuilder.append("}\n");

        try(FileWriter file = new FileWriter(filePath)){
            file.write(jsonBuilder.toString());
            file.flush();
            TaskLogger.log("Task statuses exported to " + filePath);
        } catch (IOException e) {
            TaskLogger.log("Failed to export task statuses to JSON: " + e.getMessage());
        }
    }

}
