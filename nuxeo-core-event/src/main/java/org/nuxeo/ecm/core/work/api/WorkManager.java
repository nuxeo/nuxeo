/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WorkManager} executes {@link Work} instances asynchronously.
 * <p>
 * A {@link Work} can be scheduled by calling {@link #schedule}.
 * <p>
 * Work is executed in a thread pool and a work queue that depends on the work's
 * category.
 *
 * @since 5.6
 */
public interface WorkManager {

    /**
     * Lists the ids of the existing work queues.
     *
     * @return the list of queue ids
     */
    List<String> getWorkQueueIds();

    /**
     * Gets a work queue descriptor for a given id
     *
     * @param queueId the queue id
     * @return the work queue descriptor, or {@code null}
     */
    WorkQueueDescriptor getWorkQueueDescriptor(String queueId);

    /**
     * Starts up this {@link WorkManager} and attempts to resume work previously
     * suspended at {@link #shutdown} time.
     */
    void init();

    /**
     * Shuts down this {@link WorkManager} and attempts to suspend and persist
     * any remaining executing work.
     *
     * @return {@code true} if shutdown is done, {@code false} if there are
     *         still some threads executing after the timeout
     */
    boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Schedules work for execution at a later time.
     *
     * @param work the work to execute
     */
    void schedule(Work work);

    /**
     * Gets the list of scheduled work instances for a given queue.
     *
     * @param queueId the queue id
     * @return the list of scheduled work
     */
    List<Work> getScheduledWork(String queueId);

    /**
     * Gets the list of currently running work instances for a given queue.
     *
     * @param queueId the queue id
     * @return the list of running work
     */
    List<Work> getRunningWork(String queueId);

    /**
     * Gets the list of completed work instances for a given queue.
     *
     * @param queueId the queue id
     * @return the list of completed work
     */
    List<Work> getCompletedWork(String queueId);

    /**
     * Gets the size of the non-completed work (scheduled + running) for a give
     * queue.
     *
     * @param queueId the queue id
     * @return the number of non-completed work instances
     */
    int getNonCompletedWorkSize(String queueId);

    /**
     * Clears the list of completed work instances for a given queue.
     *
     * @param queueId the queue id
     */
    void clearCompletedWork(String queueId);

    /**
     * Clears the list of completed work instances older than the given time.
     *
     * @param completionTime the completion time (milliseconds since epoch)
     *            before which completed work instances are cleared, or
     *            {@code 0} for all
     */
    void clearCompletedWork(long completionTime);

}
