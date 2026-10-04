package my.celium.org.schedule;

/** Handle for a repeating task; cancel it when it is no longer needed. */
public interface ScheduledTask {
    /** Cancels future executions. Already-running executions are unaffected. */
    void cancel();

    /** Whether this task was cancelled. */
    boolean isCancelled();
}
