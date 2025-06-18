package com.moadams.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TaskLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public static void log(String message) {
        System.out.println(getFormattedMessage("INFO", message));
    }

    public static void logWarning(String message){
        System.err.println(getFormattedMessage("WARNING", message));
    }

    public static void logError(String message){
        System.err.println(getFormattedMessage("ERROR", message));
    }

    private static String getFormattedMessage(String level, String message){
        String timestamp = FORMATTER.format(Instant.now());
        String threadName = Thread.currentThread().getName();
        return String.format("%s [%s] %s: %s", timestamp, threadName, level, message);
    }
}
