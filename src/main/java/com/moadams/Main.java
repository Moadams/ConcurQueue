package com.moadams;

import com.moadams.service.TaskDispatcher;
import com.moadams.service.TaskMonitor;
import com.moadams.util.TaskLogger;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Object LOCK_A = new Object();
    public static final Object LOCK_B = new Object();

    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        displayWelcomeMessage();
        mainMenuLoop();
        scanner.close();
        TaskLogger.log("ConcurQueue Application Exited.");
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        for (int i = 0; i < 50; ++i) System.out.println();
    }

    private static void displayWelcomeMessage() {
        clearConsole();
        TaskLogger.printLine(CYAN);
        System.out.println(BOLD + GREEN + "         Welcome to ConcurQueue!" + RESET);
        System.out.println(BOLD + YELLOW + " A Multithreaded Job Processing Platform" + RESET);
        TaskLogger.printLine(CYAN);
        System.out.println("\n" + BLUE + "This application demonstrates core Java concurrency concepts:" + RESET);
        System.out.println(BLUE + " - Race Conditions and their fixes (using AtomicInteger)" + RESET);
        System.out.println(BLUE + " - Deadlocks and their resolution (using consistent lock ordering)" + RESET);
        TaskLogger.printLine(CYAN);
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void mainMenuLoop() {
        while (true) {
            clearConsole();
            TaskLogger.printLine(PURPLE);
            System.out.println(BOLD + PURPLE + "        Main Menu" + RESET);
            TaskLogger.printLine(PURPLE);
            System.out.println(YELLOW + "1. Run Race Condition Fix Demonstration" + RESET);
            System.out.println(YELLOW + "2. Run Deadlock Demonstration (will likely get stuck!)" + RESET);
            System.out.println(YELLOW + "3. Run Deadlock Resolution Demonstration (fixed order)" + RESET);
            System.out.println(YELLOW + "4. Exit Application" + RESET);
            TaskLogger.printLine(PURPLE);
            System.out.print(GREEN + "Please select an option: " + RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    runDemo(DemoType.RACE_CONDITION_FIX);
                    break;
                case "2":
                    runDemo(DemoType.DEADLOCK_DEMO);
                    break;
                case "3":
                    runDemo(DemoType.DEADLOCK_RESOLUTION);
                    break;
                case "4":
                    return;
                default:
                    System.out.println(RED + "Invalid option. Please try again." + RESET);
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
            }
        }
    }

    private enum DemoType {
        RACE_CONDITION_FIX,
        DEADLOCK_DEMO,
        DEADLOCK_RESOLUTION
    }

    private static void runDemo(DemoType demoType) {
        clearConsole();
        TaskLogger.printLine(CYAN);
        switch (demoType) {
            case RACE_CONDITION_FIX:
                System.out.println(BOLD + GREEN + "Running Race Condition Fix Demonstration..." + RESET);
                TaskLogger.log("\n" + BOLD + "--- Race Condition Fix Explanation ---" + RESET);
                TaskLogger.log("The 'processedTaskCount' is handled using " + YELLOW + "AtomicInteger" + RESET + ", " +
                        "which ensures thread-safe increments and accurate final counts, " +
                        "preventing the race condition that would occur with a simple 'int' variable.");
                TaskLogger.log("Observe the 'Total processed' count in worker logs and monitor logs for correctness. It should " + GREEN + "ALWAYS increment consistently" + RESET + ".");
                TaskLogger.printLine(CYAN);
                runSimulation(false);
                break;
            case DEADLOCK_DEMO:
                System.out.println(BOLD + RED + "Running Deadlock Demonstration..." + RESET);
                TaskLogger.log("\n" + BOLD + "--- Deadlock Explanation ---" + RESET);
                TaskLogger.log("Worker threads will attempt to acquire two shared locks (LOCK_A, LOCK_B) in " + RED + "conflicting orders" + RESET + ".");
                TaskLogger.log("Some workers will try LOCK_A then LOCK_B. Others will try LOCK_B then LOCK_A.");
                TaskLogger.log("This conflicting order " + RED + BOLD + "WILL LIKELY LEAD TO A DEADLOCK" + RESET + ", where workers get stuck waiting for each other indefinitely.");
                TaskLogger.log("Look for " + YELLOW + "[LOCK_DEBUG]" + RESET + " messages and notice when workers stop progressing.");
                TaskLogger.log("You may need to press " + BOLD + "Ctrl+C" + RESET + " to terminate the application if it deadlocks.");
                TaskLogger.printLine(CYAN);
                runSimulation(true);
                break;
            case DEADLOCK_RESOLUTION:
                System.out.println(BOLD + GREEN + "Running Deadlock Resolution Demonstration..." + RESET);
                TaskLogger.log("\n" + BOLD + "--- Deadlock Resolution Explanation ---" + RESET);
                TaskLogger.log("Worker threads will acquire two shared locks (LOCK_A, LOCK_B) using a " + GREEN + "consistent, predefined order" + RESET + " (always LOCK_A then LOCK_B).");
                TaskLogger.log("This consistent ordering " + GREEN + BOLD + "PREVENTS DEADLOCKS" + RESET + ", allowing all tasks to be processed successfully.");
                TaskLogger.log("Observe that all " + YELLOW + "[LOCK_DEBUG]" + RESET + " messages show successful lock acquisition and release, and tasks complete as expected.");
                TaskLogger.printLine(CYAN);
                runSimulation(false);
                break;
        }

        System.out.println("\n" + BOLD + GREEN + "Demonstration Finished. Press Enter to return to Main Menu..." + RESET);
        scanner.nextLine();
    }

    private static void runSimulation(boolean introduceDeadlock) {
        int workerPoolSize = 5;
        int queueCapacity = 20;
        long producerGenerationInterval = 500;
        int tasksPerProducer = 20;
        long monitorInterval = 3000;
        String jsonExportPath = "task_statuses.json";

        TaskDispatcher dispatcher = new TaskDispatcher(workerPoolSize, queueCapacity, LOCK_A, LOCK_B, introduceDeadlock);

        Thread shutdownHook = new Thread(() -> {
            TaskLogger.log("Simulation Shutdown hook activated. Shutting down ConcurQueue...");
            dispatcher.shutdown();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        dispatcher.startWorkers();

        dispatcher.startProducer("Producer-HighPriority-1", tasksPerProducer, producerGenerationInterval);
        dispatcher.startProducer("Producer-LowPriority-1", tasksPerProducer, producerGenerationInterval);
        dispatcher.startProducer("Producer-MixedPriority-2", tasksPerProducer, producerGenerationInterval * 2);

        Thread monitorThread = new Thread(new TaskMonitor(
                dispatcher.getTaskQueue(),
                dispatcher.getWorkerPool(),
                dispatcher.getTaskStates(),
                dispatcher.getProcessedTaskCount(),
                monitorInterval,
                jsonExportPath
        ), "TaskMonitor-Thread");
        monitorThread.start();

        try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            TaskLogger.logError("Simulation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            monitorThread.interrupt();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                TaskLogger.logError("Error joining monitor thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            dispatcher.shutdown();

            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }
}
