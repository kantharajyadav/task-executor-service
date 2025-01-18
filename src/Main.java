import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final AtomicInteger sharedResource = new AtomicInteger(0);

    public static void main(String[] args) {
        TaskExecutorImpl executor = new TaskExecutorImpl(4);

        // Create different groups for different test scenarios
        TaskGroup readGroup1 = new TaskGroup(UUID.randomUUID());
        TaskGroup readGroup2 = new TaskGroup(UUID.randomUUID());
        TaskGroup writeGroup1 = new TaskGroup(UUID.randomUUID());
        TaskGroup writeGroup2 = new TaskGroup(UUID.randomUUID());

        // Test Scenario 1: Multiple READ operations in same group
        System.out.println("Test 1: Sequential READ operations in same group");
        List<Future<?>> allFutures = new ArrayList<>(submitReadTasks(executor, readGroup1, 5));

        // Test Scenario 2: Multiple WRITE operations in same group
        System.out.println("\nTest 2: Sequential WRITE operations in same group");
        allFutures.addAll(submitWriteTasks(executor, writeGroup1, 5));

        // Test Scenario 3: Concurrent READ operations across groups
        System.out.println("\nTest 3: Concurrent READ operations across groups");
        allFutures.addAll(submitReadTasks(executor, readGroup1, 3));
        allFutures.addAll(submitReadTasks(executor, readGroup2, 3));

        // Test Scenario 4: Concurrent WRITE operations across groups
        System.out.println("\nTest 4: Concurrent WRITE operations across groups");
        allFutures.addAll(submitWriteTasks(executor, writeGroup1, 3));
        allFutures.addAll(submitWriteTasks(executor, writeGroup2, 3));

        // Test Scenario 5: Mixed READ/WRITE operations
        System.out.println("\nTest 5: Mixed READ/WRITE operations");
        allFutures.addAll(submitMixedTasks(executor, readGroup1, writeGroup1));

        // Wait for all tasks to complete
        waitForCompletion(allFutures);

        // Shutdown the executor
        executor.shutdown();
    }

    private static List<Future<String>> submitReadTasks(TaskExecutor executor, TaskGroup group, int count) {
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int taskId = i;
            Task<String> readTask = new Task<>(
                    UUID.randomUUID(),
                    group,
                    TaskType.READ,
                    () -> {
                        System.out.println("READ Task " + taskId + " in group " + group.groupUUID() + " starting");
                        Thread.sleep(500); // Simulate read operation
                        int value = sharedResource.get();
                        System.out.println("READ Task " + taskId + " read value: " + value);
                        return "READ " + taskId + " completed, value=" + value;
                    }
            );
            futures.add(executor.submitTask(readTask));
        }
        return futures;
    }

    private static List<Future<String>> submitWriteTasks(TaskExecutor executor, TaskGroup group, int count) {
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int taskId = i;
            Task<String> writeTask = new Task<>(
                    UUID.randomUUID(),
                    group,
                    TaskType.WRITE,
                    () -> {
                        System.out.println("WRITE Task " + taskId + " in group " + group.groupUUID() + " starting");
                        Thread.sleep(1000); // Simulate write operation
                        int newValue = sharedResource.incrementAndGet();
                        System.out.println("WRITE Task " + taskId + " updated value to: " + newValue);
                        return "WRITE " + taskId + " completed, new value=" + newValue;
                    }
            );
            futures.add(executor.submitTask(writeTask));
        }
        return futures;
    }

    private static List<Future<String>> submitMixedTasks(TaskExecutor executor, TaskGroup readGroup, TaskGroup writeGroup) {
        List<Future<String>> futures = new ArrayList<>();

        // Submit alternating read/write tasks
        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            if (i % 2 == 0) {
                Task<String> readTask = new Task<>(
                        UUID.randomUUID(),
                        readGroup,
                        TaskType.READ,
                        () -> {
                            System.out.println("Mixed READ Task " + taskId + " starting");
                            Thread.sleep(500);
                            int value = sharedResource.get();
                            return "Mixed READ " + taskId + " completed, value=" + value;
                        }
                );
                futures.add(executor.submitTask(readTask));
            } else {
                Task<String> writeTask = new Task<>(
                        UUID.randomUUID(),
                        writeGroup,
                        TaskType.WRITE,
                        () -> {
                            System.out.println("Mixed WRITE Task " + taskId + " starting");
                            Thread.sleep(1000);
                            int newValue = sharedResource.incrementAndGet();
                            return "Mixed WRITE " + taskId + " completed, new value=" + newValue;
                        }
                );
                futures.add(executor.submitTask(writeTask));
            }
        }
        return futures;
    }

    private static void waitForCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                Object result = future.get(10, TimeUnit.SECONDS);
                System.out.println("Task result: " + result);
            } catch (Exception e) {
                System.out.println("Task failed: " + e.getMessage());
            }
        }
    }
}