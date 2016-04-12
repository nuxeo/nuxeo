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

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

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
     * Starts up this {@link WorkQueuing} and attempts to resume work previously suspended and saved at
     * shutdown time.
     */
    void init();

    /**
     * Creates a blocking queue of work used by the {@link ThreadPoolExecutor}.
     *
     * @since 8.1
     * @param queueId
     * @return
     */
    BlockingQueue<Runnable> initWorkQueue(String queueId);

    /**
     * Shuts down a queue.
     *
     * @param queueId
     * @since 8.3
     */
    void shutdownWorkQueue(String queueId);

    /**
     * Submit a work to the {@link ThreadPoolExecutor} and put it in the scheduled set.
     *
     * @param queueId the queue id
     * @param work the work instance
     * @since 8.1
     */
    boolean workSchedule(String queueId, Work work);

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
     * Finds a work instance in the scheduled or running or completed sets.
     *
     * @param workId the id of the work to find
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for SCHEDULED or RUNNING
     * @return the found work instance, or {@code null} if not found
     */
    Work find(String workId, State state);

    /**
     * Finds a scheduled work instance and removes it from the scheduled queue.
     *
     * @param queueId the queue id
     * @param workId the id of the work to find
     * @return the work if found, otherwise {@code null}
     * @since 5.8
     */
    Work removeScheduled(String queueId, String workId);

    /**
     * Checks if a work instance with the given id is in the given state.
     *
     * @param workId the work id
     * @param state the state, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING RUNNING}, {@link State#COMPLETED
     *            COMPLETED}, or {@code null} for non-completed
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
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for non-completed
     * @return the list of work instances in the given state
     */
    List<Work> listWork(String queueId, State state);

    /**
     * Lists the work ids in a given queue in a defined state.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null} for non-completed
     * @return the list of work ids in the given state
     */
    List<String> listWorkIds(String queueId, State state);

    /**
     * Gets the number of work instances in the given state in a given queue.
     *
     * @param queueId the queue id
     * @param state the state, {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING RUNNING} or
     *            {@link State#COMPLETED COMPLETED}
     * @return the number of scheduled work instances in the queue
     * @since 5.8
     */
    int count(String queueId, State state);

    /**
     * Finds which queues have completed work.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    Set<String> getCompletedQueueIds();

    /**
     * Clears the list of completed work instances older than the given time in the given queue.
     *
     * @param queueId the queue id
     * @param completionTime the completion time (milliseconds since epoch) before which completed work instances are
     *            cleared, or {@code 0} for all
     */
    void clearCompletedWork(String queueId, long completionTime);

}
