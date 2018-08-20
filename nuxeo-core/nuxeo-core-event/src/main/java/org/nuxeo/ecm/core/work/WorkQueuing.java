/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;

/**
 * Interface describing how the {@link WorkManager} implements queuing.
 * <p>
 * There are 4 structures maintained per-queue:
 * <ul>
 * <li>the work queue managed by the {@link ThreadPoolExecutor},</li>
 * <li>the set of scheduled work, this enables to list a work as being scheduled while it has been removed from the
 * queue by the {@link ThreadPoolExecutor} and not yet executed (not yet running).</li>
 * <li>the set of running work,</li>
 * <li>the set of completed work.</li>
 *
 * @since 5.8
 */
public interface WorkQueuing {

    /**
     * Starts up this {@link WorkQueuing} and attempts to resume work previously suspended and saved at shutdown time.
     * @return
     */
    NuxeoBlockingQueue init(WorkQueueDescriptor config);

    /**
     * Enable/disable this {@code queueId} processing
     * @since 8.3
     */
    void setActive(String queueId, boolean value);

    /**
     * @return true if the implementation supports processing disabling
     * @since 10.3
     */
    boolean supportsProcessingDisabling();

    /**
     * Gets the blocking queue of work used by the {@link ThreadPoolExecutor}.
     *
     * @since 8.1
     * @param queueId
     * @return
     */
    NuxeoBlockingQueue getQueue(String queueId);

    /**
     * Submit a work to the {@link ThreadPoolExecutor} and put it in the scheduled set.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @since 8.1
     */
    void workSchedule(String queueId, Work work);

    /**
     * Removes a work instance from scheduled set.
     *
     * @since 8.3
     **/
    void workCanceled(String queueId, Work work);

    /**
     * Put the work instance into the running set.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @since 5.8
     */
    void workRunning(String queueId, Work work);

    /**
     * Moves a work instance from the running set to the completed set.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @since 5.8
     */
    void workCompleted(String queueId, Work work);

    /**
     * Moves back a work instance from running set to the scheduled set.
     *
     * @since 8.3
     **/
    void workReschedule(String queueId, Work work);

    /**
     * Finds a work instance in the scheduled or running or completed sets.
     *
     * @param workId the id of the work to find
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *        RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for SCHEDULED or RUNNING
     * @return the found work instance, or {@code null} if not found
     */
    Work find(String workId, State state);

    /**
     * Finds a scheduled work instance and removes it from the scheduled queue.
     *
     * @param queueId the queue id
     * @param workId the id of the work to find
     * @since 5.8
     */
    void removeScheduled(String queueId, String workId);

    /**
     * Checks if a work instance with the given id is in the given state.
     *
     * @param workId the work id
     * @param state the state, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING RUNNING}, {@link State#COMPLETED
     *        COMPLETED}, or {@code null} for non-completed
     * @return {@code true} if a work instance with the given id is in the given state
     * @since 5.8
     */
    boolean isWorkInState(String workId, State state);

    /**
     * Gets the state in which a work instance is.
     * <p>
     * This can be {@link State#SCHEDULED}, {@link State#RUNNING}, {@link State#COMPLETED}, {@link State#FAILED}, or
     * {@link State#CANCELED}.
     *
     * @param workId the id of the work to find
     * @return the work state, or {@code null} if not found
     * @since 5.8
     */
    State getWorkState(String workId);

    /**
     * Lists the work instances in a given queue in a defined state.
     * <p>
     * Note that an instance requested as RUNNING could be found SUSPENDING or SUSPENDED, and an instance requested as
     * COMPLETED could be found FAILED.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *        RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for non-completed
     * @return the list of work instances in the given state
     */
    List<Work> listWork(String queueId, State state);

    /**
     * Lists the work ids in a given queue in a defined state.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *        RUNNING},  or {@code null} for non-completed
     * @return the list of work ids in the given state
     */
    List<String> listWorkIds(String queueId, State state);

    /**
     * Gets the number of work instances in the given state in a given queue.
     *
     * @param queueId the queue id
     * @param state the state, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING RUNNING} or
     *        {@link State#COMPLETED COMPLETED}
     * @return the number of scheduled work instances in the queue
     * @since 5.8
     */
    long count(String queueId, State state);

    /**
     * Returns current metrics of queue identified by the {@code queueId}
     *
     * @since 8.3
     */
    WorkQueueMetrics metrics(String queueId);

    /**
     * Set the callback for debugging purpose
     *
     * @since 8.3
     */
    void listen(Listener listener);

    public interface Listener {

        void queueActivated(WorkQueueMetrics metric);

        void queueDeactivated(WorkQueueMetrics metric);

        void queueChanged(Work work, WorkQueueMetrics metric);

        static Listener lookupListener() {
            final Log log = LogFactory.getLog(WorkQueuing.class);
            if (log.isTraceEnabled()) {
                class Tracing implements Listener {
                    private final Log log;

                    protected Tracing(Log log) {
                        this.log = log;
                    }

                    @Override
                    public void queueChanged(Work work, WorkQueueMetrics metrics) {
                        log.trace(String.format("%s -> changed on %s %s",
                                metrics,
                                work.getWorkInstanceState(),
                                work.getSchedulePath()));
                    }

                    @Override
                    public void queueActivated(WorkQueueMetrics metrics) {
                        log.trace(String.format("%s -> activated", metrics));
                    }

                    @Override
                    public void queueDeactivated(WorkQueueMetrics metrics) {
                        log.trace(String.format("%s -> deactivated", metrics));
                    }
                }

                return new Tracing(log);
            } else {
                class Null implements Listener {
                    @Override
                    public void queueActivated(WorkQueueMetrics metric) {

                    }

                    @Override
                    public void queueDeactivated(WorkQueueMetrics metric) {

                    }

                    @Override
                    public void queueChanged(Work work, WorkQueueMetrics metric) {

                    }
                }
                return new Null();
            }
        }
    }
}
