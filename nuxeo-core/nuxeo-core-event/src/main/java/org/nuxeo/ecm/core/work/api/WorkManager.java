/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.work.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.work.api.Work.State;

/**
 * A {@link WorkManager} executes {@link Work} instances asynchronously.
 * <p>
 * A {@link Work} can be scheduled by calling {@link #schedule}.
 * <p>
 * Work is executed in a thread pool and a work queue that depends on the work's category.
 *
 * @since 5.6
 */
public interface WorkManager {

    /**
     * The scheduling policy to use when adding a work instance using {@link #schedule(Work, Scheduling)}.
     */
    enum Scheduling {
        /**
         * Always schedule the work.
         */
        ENQUEUE,
        /**
         * Any other scheduled work equals to this one is removed from scheduling and canceled first, before this work
         * is scheduled.
         */
        CANCEL_SCHEDULED,
        /**
         * If there is a scheduled work equals to this one, then don't schedule the work.
         */
        IF_NOT_SCHEDULED(State.SCHEDULED),
        /**
         * If there is a running or scheduled work equals to this one, then don't schedule the work.
         */
        IF_NOT_RUNNING_OR_SCHEDULED;

        public final State state;

        Scheduling() {
            state = null;
        }

        Scheduling(State state) {
            this.state = state;
        }
    }

    /**
     * Schedules work for execution at a later time.
     * <p>
     * This method is identical to {@link #schedule(Work, boolean)} with {@code afterCommit = false}.
     *
     * @param work the work to execute
     */
    void schedule(Work work);

    /**
     * Schedules work for execution at a later time, after the current transaction (if any) has committed.
     *
     * @param work the work to execute
     * @param afterCommit if {@code true} and the work is scheduled, it will only be run after the current transaction
     *            (if any) has committed
     */
    void schedule(Work work, boolean afterCommit);

    /**
     * Schedules work for execution at a later time, with a specific {@linkplain Scheduling scheduling} policy.
     * <p>
     * This method is identical to {@link #schedule(Work, Scheduling, boolean)} with {@code afterCommit = false}.
     *
     * @param work the work to execute
     * @param scheduling the scheduling policy
     * @see #schedule(Work)
     */
    void schedule(Work work, Scheduling scheduling);

    /**
     * Schedules work for execution at a later time, with a specific {@linkplain Scheduling scheduling} policy.
     *
     * @param work the work to execute
     * @param scheduling the scheduling policy
     * @param afterCommit if {@code true} and the work is scheduled, it will only be run after the current transaction
     *            (if any) has committed
     * @see #schedule(Work)
     */
    void schedule(Work work, Scheduling scheduling, boolean afterCommit);

    /** Admin API **/
    /**
     * Lists the ids of the existing work queues.
     *
     * @return the list of queue ids
     */
    List<String> getWorkQueueIds();

    /**
     * Gets the queue id used for a given work category.
     *
     * @param category the category
     * @return the queue id
     */
    String getCategoryQueueId(String category);

    /**
     * Gets the work queue descriptor for a given queue id.
     *
     * @param queueId the queue id
     * @return the work queue descriptor, or {@code null}
     */
    WorkQueueDescriptor getWorkQueueDescriptor(String queueId);

    /**
     * Is processing enabled for at least one queue
     *
     * @since 8.3
     */
    boolean isProcessingEnabled();

    /**
     * Set processing for all queues
     *
     * @since 8.3
     */

    void enableProcessing(boolean value);

    /**
     * Is processing enabled for a given queue id.
     *
     * @since 8.3
     */
    boolean isProcessingEnabled(String queueId);

    /**
     * Set processing for a given queue id.
     *
     * @since 8.3
     */
    void enableProcessing(String queueId, boolean value) throws InterruptedException;

    /**
     * Is queuing enabled for a given queue id.
     *
     * @since 8.3
     */
    boolean isQueuingEnabled(String queueId);

    /**
     * Starts up this {@link WorkManager} and attempts to resume work previously suspended and saved at
     * {@link #shutdown} time.
     */
    void init();

    /**
     * Shuts down a work queue and attempts to suspend and save the running and scheduled work instances.
     *
     * @param queueId the queue id
     * @param timeout the time to wait
     * @param unit the timeout unit
     * @return {@code true} if shutdown is done, {@code false} if there are still some threads executing after the
     *         timeout
     */
    boolean shutdownQueue(String queueId, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Shuts down this {@link WorkManager} and attempts to suspend and save the running and scheduled work instances.
     *
     * @param timeout the time to wait
     * @param unit the timeout unit
     * @return {@code true} if shutdown is done, {@code false} if there are still some threads executing after the
     *         timeout
     */
    boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Gets the number of work instances in a given queue in a defined state.
     * <p>
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for non-completed (
     *            {@link State#SCHEDULED SCHEDULED} or {@link State#RUNNING RUNNING})
     * @return the number of work instances in the given state
     * @since 5.8
     * @deprecated since 5.8,
     */
    @Deprecated
    int getQueueSize(String queueId, State state);

    /**
     * Gets the metrics for the {@code queueId}
     *
     * @since 8.3
     */
    WorkQueueMetrics getMetrics(String queueId);

    /**
     * Waits for completion of work in a given queue.
     *
     * @param queueId the queue id
     * @param timeout the time to wait
     * @param unit the timeout unit
     * @return {@code true} if all work completed in the queue, or {@code false} if there is still some non-completed
     *         work after the timeout
     */
    boolean awaitCompletion(String queueId, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for completion of all work.
     *
     * @param timeout the time to wait
     * @param unit the timeout unit
     * @return {@code true} if all work completed, or {@code false} if there is still some non-completed work after the
     *         timeout
     */
    boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * @return {@code true} if active
     * @see org.nuxeo.runtime.model.DefaultComponent#applicationStarted(org.nuxeo.runtime.model.ComponentContext)
     * @see #init()
     * @see #shutdown(long, TimeUnit)
     * @since 6.0
     */
    boolean isStarted();

    /** Works lookup API **/
    /**
     * Gets the state in which a work instance is.
     * <p>
     * This can be {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING RUNNING}, or null.
     *
     * @param workId the id of the work to find
     * @return the work state, or {@code null} if not found
     * @since 5.8
     */
    @Deprecated
    State getWorkState(String workId);

    /**
     * Finds a work instance.
     *
     * @param workId the id of the work to find
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, or {@code null} for non-completed
     * @return the found work instance, or {@code null} if not found
     * @since 7.3
     */
    Work find(String workId, State state);

    /**
     * Lists the work instances in a given queue in a defined state.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, or {@code null} for non-completed
     * @return the list of work instances in the given state
     */
    List<Work> listWork(String queueId, State state);

    /**
     * Lists the work ids in a given queue in a defined state.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, or {@code null} for non-completed
     * @return the list of work ids in the given state
     * @since 5.8
     */
    List<String> listWorkIds(String queueId, State state);

}
