package com.moadams.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TaskLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());


    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";


    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private static final String INFO_ICON = "📋";
    private static final String WARNING_ICON = "⚠️";
    private static final String ERROR_ICON = "❌";
    private static final String SUCCESS_ICON = "✅";
    private static final String LOCK_ICON = "🔒";
    private static final String WORKER_ICON = "👷";
    private static final String PRODUCER_ICON = "🏭";
    private static final String MONITOR_ICON = "📊";
    private static final String TASK_ICON = "📝";
    private static final String SHUTDOWN_ICON = "🔄";

    public static void log(String message) {
        String styledMessage = styleMessage(message);
        System.out.println(getFormattedMessage("INFO", INFO_ICON, GREEN, styledMessage));
    }

    public static void logWarning(String message) {
        String styledMessage = styleMessage(message);
        System.out.println(getFormattedMessage("WARN", WARNING_ICON, YELLOW, styledMessage));
    }

    public static void logError(String message) {
        String styledMessage = styleMessage(message);
        System.err.println(getFormattedMessage("ERROR", ERROR_ICON, RED + BOLD, styledMessage));
    }

    public static void logLock(String message) {
        String styledMessage = styleMessage(message);
        System.out.println(getFormattedMessage("LOCK_DEBUG", LOCK_ICON, PURPLE, styledMessage));
    }

    public static void logSuccess(String message) {
        String styledMessage = styleMessage(message);
        System.out.println(getFormattedMessage("SUCCESS", SUCCESS_ICON, GREEN + BOLD, styledMessage));
    }

    public static void printLine(String color) {
        System.out.println(color + "═══════════════════════════════════════════════════════════════════════" + RESET);
    }

    public static void printDoubleLine(String color) {
        System.out.println(color + "╔═══════════════════════════════════════════════════════════════════════╗" + RESET);
    }

    public static void printBottomLine(String color) {
        System.out.println(color + "╚═══════════════════════════════════════════════════════════════════════╝" + RESET);
    }

    public static void printSectionHeader(String title, String color) {
        System.out.println();
        printDoubleLine(color);
        System.out.println(color + BOLD + "║" + centerText(title, 69) + "║" + RESET);
        printBottomLine(color);
        System.out.println();
    }

    public static void printBox(String message, String color) {
        String[] lines = message.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, stripAnsiCodes(line).length());
        }
        maxLength = Math.max(maxLength, 40);


        System.out.println(color + "┌" + "─".repeat(maxLength + 2) + "┐" + RESET);


        for (String line : lines) {
            int padding = maxLength - stripAnsiCodes(line).length();
            System.out.println(color + "│ " + line + " ".repeat(padding) + " │" + RESET);
        }


        System.out.println(color + "└" + "─".repeat(maxLength + 2) + "┘" + RESET);
    }

    private static String styleMessage(String message) {

        if (message.contains("Worker") && message.contains("started")) {
            return WORKER_ICON + " " + BOLD + message + RESET;
        } else if (message.contains("Worker") && message.contains("completed")) {
            return WORKER_ICON + " " + message.replace("completed", GREEN + BOLD + "completed" + RESET);
        } else if (message.contains("Worker") && message.contains("failed")) {
            return WORKER_ICON + " " + message.replace("failed", RED + BOLD + "failed" + RESET);
        } else if (message.contains("Producer") && message.contains("started")) {
            return PRODUCER_ICON + " " + BOLD + CYAN + message + RESET;
        } else if (message.contains("Producer") && message.contains("submitted")) {
            return PRODUCER_ICON + " " + message.replace("submitted", GREEN + "submitted" + RESET);
        } else if (message.contains("MONITOR")) {
            return MONITOR_ICON + " " + stylizeMonitorMessage(message);
        } else if (message.contains("TaskDispatcher") || message.contains("shutdown") || message.contains("Shutdown")) {
            return SHUTDOWN_ICON + " " + BOLD + BLUE + message + RESET;
        } else if (message.contains("Task") && !message.contains("Worker") && !message.contains("Producer")) {
            return TASK_ICON + " " + message;
        }

        return message;
    }

    private static String stylizeMonitorMessage(String message) {
        String styled = message;


        styled = styled.replace("Queue Size:", YELLOW + BOLD + "Queue Size:" + RESET);
        styled = styled.replace("Active Workers:", BLUE + BOLD + "Active Workers:" + RESET);
        styled = styled.replace("Processed Tasks (Total):", GREEN + BOLD + "Processed Tasks (Total):" + RESET);
        styled = styled.replace("Task Statuses:", PURPLE + BOLD + "Task Statuses:" + RESET);


        styled = styled.replace("SUBMITTED:", CYAN + "SUBMITTED:" + RESET);
        styled = styled.replace("PROCESSING:", YELLOW + "PROCESSING:" + RESET);
        styled = styled.replace("COMPLETED:", GREEN + "COMPLETED:" + RESET);
        styled = styled.replace("FAILED:", RED + "FAILED:" + RESET);

        return styled;
    }

    private static String getFormattedMessage(String level, String icon, String levelColor, String message) {
        String timestamp = FORMATTER.format(Instant.now());
        String threadName = Thread.currentThread().getName();


        String styledThreadName = styleThreadName(threadName);


        return String.format("%s[%s]%s %s%s %-8s%s %s │ %s",
                DIM, timestamp, RESET,
                levelColor, icon, level, RESET,
                styledThreadName,
                message
        );
    }

    private static String styleThreadName(String threadName) {
        if (threadName.contains("Worker") || threadName.contains("pool")) {
            return BLUE + BOLD + threadName + RESET;
        } else if (threadName.contains("Producer")) {
            return CYAN + BOLD + threadName + RESET;
        } else if (threadName.contains("Monitor")) {
            return PURPLE + BOLD + threadName + RESET;
        } else if (threadName.equals("main")) {
            return GREEN + BOLD + threadName + RESET;
        }
        return WHITE + threadName + RESET;
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, width - text.length() - padding));
    }

    private static String stripAnsiCodes(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

}