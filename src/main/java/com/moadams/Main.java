package com.moadams;

import com.moadams.enums.DemoType;
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
        TaskLogger.log("🚪 ConcurQueue Application Exited. Goodbye!");
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        for (int i = 0; i < 3; ++i) System.out.println();
    }

    private static void displayWelcomeMessage() {
        clearConsole();


        TaskLogger.printSectionHeader("🎯 WELCOME TO CONCURQUEUE 🎯", CYAN);

        String welcomeMessage = BOLD + GREEN + "A Multithreaded Job Processing Platform" + RESET + "\n\n" +
                BLUE + "🧵 This application demonstrates core Java concurrency concepts:" + RESET + "\n" +
                GREEN + "  ✓ Race Conditions and their fixes (using AtomicInteger)" + RESET + "\n" +
                GREEN + "  ✓ Deadlocks and their resolution (using consistent lock ordering)" + RESET + "\n" +
                GREEN + "  ✓ Thread-safe data structures and synchronization" + RESET;

        TaskLogger.printBox(welcomeMessage, CYAN);

        System.out.println("\n" + YELLOW + "🚀 Press Enter to continue to the main menu..." + RESET);
        scanner.nextLine();
    }

    private static void mainMenuLoop() {
        while (true) {
            clearConsole();

            TaskLogger.printSectionHeader("🎮 MAIN MENU", PURPLE);

            String menuOptions = YELLOW + "1. " + GREEN + "🏁 Run Race Condition Fix Demonstration" + RESET + "\n" +
                    YELLOW + "2. " + RED + "💀 Run Deadlock Demonstration (will likely get stuck!)" + RESET + "\n" +
                    YELLOW + "3. " + GREEN + "🔧 Run Deadlock Resolution Demonstration (fixed order)" + RESET + "\n" +
                    YELLOW + "4. " + BLUE + "🚪 Exit Application" + RESET;

            TaskLogger.printBox(menuOptions, PURPLE);

            System.out.print("\n" + GREEN + BOLD + "🎯 Please select an option (1-4): " + RESET);

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
                    System.out.println("\n" + GREEN + "👋 Thank you for using ConcurQueue! Goodbye!" + RESET);
                    return;
                default:
                    System.out.println("\n" + RED + BOLD + "❌ Invalid option. Please select 1, 2, 3, or 4." + RESET);
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
            }
        }
    }



    private static void runDemo(DemoType demoType) {
        clearConsole();

        switch (demoType) {
            case RACE_CONDITION_FIX:
                TaskLogger.printSectionHeader("🏁 RACE CONDITION FIX DEMONSTRATION", GREEN);

                String raceFixExplanation = BOLD + "🔍 Race Condition Fix Explanation:" + RESET + "\n\n" +
                        "The 'processedTaskCount' is handled using " + YELLOW + BOLD + "AtomicInteger" + RESET + ", " +
                        "which ensures thread-safe increments and accurate final counts, " +
                        "preventing the race condition that would occur with a simple 'int' variable.\n\n" +
                        GREEN + "📊 What to observe:" + RESET + "\n" +
                        "• Watch the 'Total processed' count in worker logs\n" +
                        "• Monitor logs show consistent increments\n" +
                        "• " + GREEN + BOLD + "Count should ALWAYS increment consistently" + RESET;

                TaskLogger.printBox(raceFixExplanation, GREEN);
                runSimulation(false);
                break;

            case DEADLOCK_DEMO:
                TaskLogger.printSectionHeader("💀 DEADLOCK DEMONSTRATION", RED);

                String deadlockExplanation = BOLD + RED + "⚠️  DEADLOCK EXPLANATION:" + RESET + "\n\n" +
                        "Worker threads will attempt to acquire two shared locks (LOCK_A, LOCK_B) in " +
                        RED + BOLD + "conflicting orders" + RESET + ".\n\n" +
                        "• Some workers: LOCK_A → LOCK_B\n" +
                        "• Other workers: LOCK_B → LOCK_A\n\n" +
                        RED + BOLD + "⚡ This WILL LIKELY CAUSE A DEADLOCK!" + RESET + "\n\n" +
                        YELLOW + "👀 What to watch for:" + RESET + "\n" +
                        "• Look for " + PURPLE + "[LOCK_DEBUG]" + RESET + " messages\n" +
                        "• Notice when workers stop progressing\n" +
                        "• You may need " + BOLD + "Ctrl+C" + RESET + " to terminate";

                TaskLogger.printBox(deadlockExplanation, RED);
                runSimulation(true);
                break;

            case DEADLOCK_RESOLUTION:
                TaskLogger.printSectionHeader("🔧 DEADLOCK RESOLUTION DEMONSTRATION", GREEN);

                String resolutionExplanation = BOLD + GREEN + "🛠️  DEADLOCK RESOLUTION EXPLANATION:" + RESET + "\n\n" +
                        "Worker threads acquire locks using a " + GREEN + BOLD + "consistent, predefined order" + RESET +
                        " (always LOCK_A then LOCK_B).\n\n" +
                        GREEN + BOLD + "✅ This prevents deadlocks completely!" + RESET + "\n\n" +
                        BLUE + "📈 What you'll see:" + RESET + "\n" +
                        "• All " + PURPLE + "[LOCK_DEBUG]" + RESET + " messages show successful operations\n" +
                        "• Tasks complete successfully without hanging\n" +
                        "• Consistent processing throughout the demonstration";

                TaskLogger.printBox(resolutionExplanation, GREEN);
                runSimulation(false);
                break;
        }

        System.out.println("\n");
        TaskLogger.printLine(CYAN);
        System.out.println(BOLD + GREEN + "🎉 Demonstration Complete! Press Enter to return to Main Menu..." + RESET);
        TaskLogger.printLine(CYAN);
        scanner.nextLine();
    }

    private static void runSimulation(boolean introduceDeadlock) {
        System.out.println(CYAN + "🚀 Initializing simulation..." + RESET);

        int workerPoolSize = 5;
        int queueCapacity = 20;
        long producerGenerationInterval = 1000;
        int tasksPerProducer = 10;
        long monitorInterval = 5000;
        String jsonExportPath = "task_statuses.json";

        TaskDispatcher dispatcher = new TaskDispatcher(workerPoolSize, queueCapacity, LOCK_A, LOCK_B, introduceDeadlock);

        Thread shutdownHook = new Thread(() -> {
            TaskLogger.log("🛑 Simulation Shutdown hook activated. Shutting down ConcurQueue...");
            dispatcher.shutdown();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        TaskLogger.printLine(BLUE);
        TaskLogger.log("🎬 Starting simulation components...");

        dispatcher.startWorkers();

        dispatcher.startProducer("Producer-HighPriority-1", tasksPerProducer, producerGenerationInterval);
        dispatcher.startProducer("Producer-LowPriority-1", tasksPerProducer, producerGenerationInterval);

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
            TaskLogger.log("🔄 Cleaning up simulation...");
            monitorThread.interrupt();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                TaskLogger.logError("Error joining monitor thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            dispatcher.shutdown();

            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            TaskLogger.logSuccess("✅ Simulation cleanup completed successfully!");
        }
    }
}