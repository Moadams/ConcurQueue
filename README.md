# ConcurQueue - A Multithreaded Job Processing Platform

## Overview
ConcurQueue is a high-performance job dispatcher system that demonstrates core Java concurrency concepts including race conditions, deadlocks, and their resolutions. The system efficiently handles multiple producer clients submitting jobs and distributes them to worker threads for concurrent processing.

## Features
- **Priority-based Task Processing**: Tasks are processed based on priority using PriorityBlockingQueue
- **Multi-Producer Support**: Multiple producer threads simulate different client types
- **Thread Pool Management**: Fixed-size ExecutorService manages worker threads
- **Race Condition Demonstration**: Shows unsafe counters vs AtomicInteger
- **Deadlock Scenarios**: Demonstrates deadlock creation and resolution
- **Retry Mechanism**: Failed tasks are automatically retried up to 3 times
- **Real-time Monitoring**: Background monitor tracks system metrics
- **JSON Export**: Task statuses exported to JSON file periodically
- **Graceful Shutdown**: Proper cleanup with queue draining

## Architecture

### Core Components
1. **Task Model** (`com.moadams.model.Task`)
    - Immutable task with UUID, name, priority, timestamp, and payload
    - Implements Comparable for priority queue ordering
    - Built-in retry count tracking

2. **Producers** (`com.moadams.producer.TaskProducer`)
    - High Priority Producer: Generates urgent tasks (priority 1-3)
    - Low Priority Producer: Generates routine tasks (priority 5-9)
    - Mixed Priority Producer: Generates varied priority tasks (priority 1-9)

3. **Consumers** (`com.moadams.consumer.TaskWorker`)
    - Worker threads that fetch and process tasks
    - Simulates processing time (200-2000ms)
    - Handles failures and retry logic
    - Demonstrates lock acquisition patterns

4. **Task Dispatcher** (`com.moadams.service.TaskDispatcher`)
    - Central coordinator managing producers and consumers
    - Thread pool management
    - Graceful shutdown handling

5. **Monitor** (`com.moadams.service.TaskMonitor`)
    - Real-time system metrics logging
    - Stalled task detection
    - JSON status export

## Concurrency Demonstrations

### 1. Race Condition Fix
- **Problem**: Multiple threads incrementing a shared counter unsafely
- **Solution**: Using `AtomicInteger` for thread-safe operations
- **Observable**: Consistent task count across all logs

### 2. Deadlock Scenario
- **Problem**: Workers acquire locks (LOCK_A, LOCK_B) in different orders
- **Result**: System freezes when circular dependency occurs
- **Detection**: Monitor logs show workers stuck in lock acquisition

### 3. Deadlock Resolution
- **Solution**: Consistent lock ordering (always LOCK_A then LOCK_B)
- **Result**: All tasks process successfully without blocking
- **Verification**: All lock debug messages show successful acquisition/release

## Building and Running

### Prerequisites
- Java 24 or higher
- Maven 3.6+

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn exec:java -Dexec.mainClass="com.moadams.Main"
```

### Interactive Menu
The application provides an interactive menu with the following options:
1. **Race Condition Fix Demo**: Shows AtomicInteger preventing race conditions
2. **Deadlock Demo**: Demonstrates deadlock scenario (may require Ctrl+C to exit)
3. **Deadlock Resolution**: Shows proper lock ordering preventing deadlocks
4. **Exit**: Graceful application shutdown

## Key Learning Outcomes
- Understanding Java Memory Model and visibility issues
- Implementing producer-consumer patterns with BlockingQueues
- Using synchronization primitives (synchronized, volatile, AtomicInteger)
- Detecting and resolving deadlocks through proper lock ordering
- Building robust multithreaded systems with proper error handling
- Performance monitoring and debugging concurrent applications

## Sample Output
```
12:34:56.789 [Producer-HighPriority-1] INFO: Producer Producer-HighPriority-1 started
12:34:56.791 [pool-1-thread-1] INFO: Worker pool-1-thread-1 picked up Task{id=a1b2c3d4, name='UrgentTask-Producer-HighPriority-1-1', priority=2, retries=0}
12:34:56.792 [pool-1-thread-1] [LOCK_DEBUG]: Worker pool-1-thread-1 acquired LOCK_A for UrgentTask-Producer-HighPriority-1-1
12:34:56.893 [pool-1-thread-1] INFO: Worker pool-1-thread-1 completed Task{id=a1b2c3d4, name='UrgentTask-Producer-HighPriority-1-1', priority=2, retries=0} in 987ms. Total processed: 1
```

## Configuration
Key parameters can be adjusted in `Main.java`:
- `workerPoolSize`: Number of worker threads (default: 5)
- `queueCapacity`: Maximum queue size (default: 20)
- `producerGenerationInterval`: Delay between task generation (default: 500ms)
- `monitorInterval`: Monitor logging frequency (default: 3000ms)

## File Outputs
- `task_statuses.json`: Periodic export of all task statuses
- Console logs: Real-time system activity and debugging information

## Thread Safety
All shared data structures are thread-safe:
- `PriorityBlockingQueue<Task>` for task queue
- `ConcurrentHashMap<UUID, TaskStatus>` for task state tracking
- `AtomicInteger` for counters
- Proper synchronization for shared resources

## Shutdown Behavior
The application implements graceful shutdown:
1. Stop accepting new tasks
2. Allow current tasks to complete (30-second timeout)
3. Force shutdown if necessary
4. Drain and log remaining tasks
5. Clean up resources