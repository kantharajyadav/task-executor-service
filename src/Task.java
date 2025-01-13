/**
 * Representation of computation to be performed by the {@link
 * TaskExecutor}.
 *
 * @param taskUUID Unique task identifier.
 * @param taskGroup Task group.
 * @param taskType Task type.
 * @param taskAction Callable representing task computation and returning
 * the result.
 * @param ask computation result value type.
 */

import java.util.UUID;
import java.util.concurrent.Callable;

public record Task<T>(UUID taskUUID,
                      TaskGroup taskGroup,
                      TaskType taskType,
                      Callable<T> taskAction) {
    public Task {
        if (taskUUID == null || taskGroup == null || taskType == null ||
                taskAction == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
    }
}
