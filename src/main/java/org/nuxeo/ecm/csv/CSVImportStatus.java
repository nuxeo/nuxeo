package org.nuxeo.ecm.csv;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportStatus {

    private final State state;
    private final int positionInQueue;
    private final int queueSize;

    public enum State {
        SCHEDULED,
        RUNNING,
        COMPLETED
    }

    public CSVImportStatus(State state) {
        this(state, 0, 0);
    }

    public CSVImportStatus(State state, int positionInQueue,
            int queueSize) {
        this.state = state;
        this.positionInQueue = positionInQueue;
        this.queueSize = queueSize;
    }

    public State getState() {
        return state;
    }

    public int getPositionInQueue() {
        return positionInQueue;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public boolean isScheduled() {
        return state == State.SCHEDULED;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isComplete() {
        return state == State.COMPLETED;
    }
}
