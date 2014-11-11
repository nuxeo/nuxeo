/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;

/**
 * Interface describing how the {@link WorkManager} implements queuing.
 * <p>
 * There are 3 structures maintained per-queue:
 * <ul>
 * <li>the queue of scheduled work,</li>
 * <li>the set of running work,</li>
 * <li>the set of completed work.</li>
 *
 * @since 5.8
 */
public interface WorkQueuing {

    /**
     * Starts up this {@link WorkQueuing} and attempts to resume work previously
     * suspended and saved at {@link #shutdown} time.
     */
    void init();

    /**
     * @since 5.9.6
     * @param queueId
     * @return
     */
    BlockingQueue<Runnable> initScheduleQueue(String queueId);

    /**
     * Gets the blocking queue for scheduled work, to be used in a
     * {@link ThreadPoolExecutor}.
     *
     * @param queueId the queue id
     * @return the queue
     * @since 5.8
     */
    BlockingQueue<Runnable> getScheduledQueue(String queueId);

    /**
     * Moves a work instance from the scheduled queue to the running set. When
     * called, the work instance is already removed from the scheduled queue.
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
     * Finds a work instance in the scheduled queue or running or completed
     * sets.
     *
     * @param workId the id of the work to find
     * @param state the state defining the state to look into,
     *            {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null}
     *            for SCHEDULED or RUNNING
     *
     * @return the found work instance, or {@code null} if not found
     */
    Work find(String workId, State state);

    /**
     * Finds a scheduled work instance and removes it from the scheduled work.
     *
     * @param queueId the queue id
     * @param workId the id of the work to find
     *
     * @return the work if found, otherwise {@code null}
     * @since 5.8
     */
    Work removeScheduled(String queueId, String workId);

    /**
     * Checks if a work instance with the given id is in the given state.
     *
     * @param workId the work id
     * @param state the state, {@link State#SCHEDULED SCHEDULED},
     *            {@link State#RUNNING RUNNING}, {@link State#COMPLETED
     *            COMPLETED}, or {@code null} for non-completed
     * @return {@code true} if a work instance with the given id is in the given
     *         state
     * @since 5.8
     */
    boolean isWorkInState(String workId, State state);

    /**
     * Gets the state in which a work instance is.
     * <p>
     * This can be {@link State#SCHEDULED}, {@link State#RUNNING},
     * {@link State#COMPLETED}, {@link State#FAILED}, or {@link State#CANCELED}.
     *
     * @param workId the id of the work to find
     * @return the work state, or {@code null} if not found
     *
     * @since 5.8
     */
    State getWorkState(String workId);

    /**
     * Lists the work instances in a given queue in a defined state.
     * <p>
     * Note that an instance requested as RUNNING could be found SUSPENDING or
     * SUSPENDED, and an instance requested as COMPLETED could be found FAILED.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into,
     *            {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null}
     *            for non-completed
     * @return the list of work instances in the given state
     */
    List<Work> listWork(String queueId, State state);

    /**
     * Lists the work ids in a given queue in a defined state.
     *
     * @param queueId the queue id
     * @param state the state defining the state to look into,
     *            {@link State#SCHEDULED SCHEDULED}, {@link State#RUNNING
     *            RUNNING}, {@link State#COMPLETED COMPLETED}, or {@code null}
     *            for non-completed
     * @return the list of work ids in the given state
     */
    List<String> listWorkIds(String queueId, State state);

    /**
     * Gets the number of work instances in the given state in a given queue.
     *
     * @param queueId the queue id
     * @param state the state, {@link State#SCHEDULED SCHEDULED},
     *            {@link State#RUNNING RUNNING} or {@link State#COMPLETED
     *            COMPLETED}
     * @return the number of scheduled work instances in the queue
     *
     * @since 5.8
     */
    int getQueueSize(String queueId, State state);

    /**
     * Notifies this queuing that all work should be suspending.
     *
     * @return the number of scheduled instances removed from queue
     */
    int setSuspending(String queueId);

    /**
     * Finds which queues have completed work.
     *
     * @return a set of queue ids
     * @since 5.8
     */
    Set<String> getCompletedQueueIds();

    /**
     * Clears the list of completed work instances older than the given time in
     * the given queue.
     *
     * @param queueId the queue id
     * @param completionTime the completion time (milliseconds since epoch)
     *            before which completed work instances are cleared, or
     *            {@code 0} for all
     */
    void clearCompletedWork(String queueId, long completionTime);

}
