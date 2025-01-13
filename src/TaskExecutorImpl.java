import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskExecutorImpl implements TaskExecutor {
    private final ExecutorService executor;
    // To ensure tasks in the same group don't run concurrently
    private final ConcurrentHashMap<UUID, Lock> groupLocks;
    private final BlockingQueue<TaskWrapper<?>> taskQueue;
    private final Thread taskScheduler;
    private volatile boolean isRunning;

    public TaskExecutorImpl(int maxThreads) {
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.groupLocks = new ConcurrentHashMap<>();
        this.taskQueue = new PriorityBlockingQueue<>(11, Comparator.comparing(TaskWrapper::getSubmissionTime));
        this.isRunning = true;
        this.taskScheduler = new Thread(this::processTaskQueue);
        this.taskScheduler.start();
    }


    @Override
    public <T> Future<T> submitTask(Task<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }

        TaskWrapper<T> wrapper = new TaskWrapper<>(task, System.nanoTime());
        taskQueue.offer(wrapper);
        return wrapper.future;
    }

    private void processTaskQueue() {
        while (isRunning || !taskQueue.isEmpty()) {
            try {
                TaskWrapper<?> wrapper = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if (wrapper != null) {
                    scheduleTask(wrapper);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private <T> void scheduleTask(TaskWrapper<T> wrapper) {
        Lock groupLock = groupLocks.computeIfAbsent(wrapper.task.taskGroup().groupUUID(), k -> new ReentrantLock());
        CompletableFuture.runAsync(() -> {
            try {
                groupLock.lock();
                T result = wrapper.task.taskAction().call();
                wrapper.future.complete(result);
            } catch (Exception e) {
                wrapper.future.completeExceptionally(e);
            } finally {
                groupLock.unlock();
            }
        }, executor);
    }

    private static class TaskWrapper<T> {
        private final Task<T> task;
        private final CompletableFuture<T> future;
        private long submissionTime;

        TaskWrapper(Task<T> task, long submissionTime) {
            this.task = task;
            this.future = new CompletableFuture<>();
            this.submissionTime = submissionTime;
        }

        public long getSubmissionTime() {
            return submissionTime;
        }
    }

    public void shutdown() {
        isRunning = false;
        taskScheduler.interrupt();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
