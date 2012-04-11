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

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * A {@link Work} instance gets executed by a {@link WorkManager}.
 * <p>
 * It's a runnable that can be {@linkplain #suspend() suspended} and can report
 * its status and progress.
 *
 * @since 5.6
 */
public interface Work extends Runnable {

    /**
     * Gets the category for this work.
     * <p>
     * Used to choose a thread pool queue.
     *
     * @return the category, or {@code null} for the default
     */
    String getCategory();

    /**
     * Gets the principal on behalf of which this work is done.
     * <p>
     * This is informative only.
     *
     * @return the principal, or {@code null}
     */
    Principal getPrincipal();

    /**
     * Gets the documents impacted by the work.
     * <p>
     * This is informative only.
     *
     * @return the documents
     */
    Collection<DocumentLocation> getDocuments();

    /**
     * Requests that this work instance suspend its state in memory.
     * <p>
     * Does not block. Use {@link #getData} to wait for actual suspension and
     * get the data.
     *
     * @return {@code true} if suspend was started, or {@code false} if the job
     *         was already completed
     */
    boolean suspend();

    /**
     * Gets the state data for this suspended work instance.
     * <p>
     * May wait for the instance to actually finish suspending.
     *
     * @param timeout how long to wait for the data to be available
     * @param unit timeout unit
     * @return the data allowing the work instance to be resumed, or
     *         {@code null} if no data is available in the given timeout
     */
    Map<String, Serializable> getData(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Restores a saved state data for this work instance.
     *
     * @param data the saved state data
     */
    void setData(Map<String, Serializable> data);

    // resume should be in workmanager

    /**
     * The running state of a {@link Work} instance.
     */
    enum State {
        /** Work instance is queued but not yet running. */
        QUEUED,
        /** Work instance is running. */
        RUNNING,
        /** Work instance is in the process of suspending work. */
        SUSPENDING,
        /** Work instance is suspended in memory and work is stopped. */
        SUSPENDED,
        /** Work instance has completed. */
        COMPLETED,
        /** Work instance execution failed. */
        FAILED
    };

    /**
     * Gets the running state for this work instance.
     *
     * @return the running state
     */
    State getState();

    /**
     * Gets a human-readable status for this work instance.
     *
     * @return a human-readable status
     */
    String getStatus();

    /**
     * Gets the time at which this work instance was scheduled.
     *
     * @return the scheduling time (milliseconds since epoch)
     */
    long getSchedulingTime();

    /**
     * Gets the time at which this work instance was started.
     *
     * @return the start time (milliseconds since epoch), or {@code 0} if not
     *         stated
     */
    // TODO what if suspended / resumed
    long getStartTime();

    /**
     * Gets the time at which this work instance was completed, suspended or
     * failed.
     *
     * @return the completion time (milliseconds since epoch), or {@code 0} if
     *         not completed
     */
    long getCompletionTime();

    /**
     * Gets a progress report for this work instance.
     *
     * @return a progress report, not {@code null}
     */
    Progress getProgress();

    /**
     * A progress report about a work instance.
     * <p>
     * Progress can be expressed as a percentage, or with an "n out of m" pair.
     * <ul>
     * <li>26.2% (percent not indeterminate)</li>
     * <li>12/345 (current not indeterminate)</li>
     * <li>?/345 (percent and current indeterminate but total non-zero)</li>
     * <li>? (percent and current indeterminate and total zero)</li>
     * </ul>
     *
     * @since 5.6
     */
    public static class Progress {

        public static long CURRENT_INDETERMINATE = -1;

        public static float PERCENT_INDETERMINATE = -1F;

        public static final Progress PROGRESS_INDETERMINATE = new Progress(
                PERCENT_INDETERMINATE);

        public static final Progress PROGRESS_0_PC = new Progress(0F);

        public static final Progress PROGRESS_100_PC = new Progress(100F);

        protected final float percent;

        protected final long current;

        protected final long total;

        public Progress(float percent) {
            this.percent = percent;
            this.current = CURRENT_INDETERMINATE;
            this.total = 0;
        }

        public Progress(long current, long total) {
            this.percent = PERCENT_INDETERMINATE;
            this.current = current;
            this.total = total;
        }

        public float getPercent() {
            return percent;
        }

        public long getCurrent() {
            return current;
        }

        public long getTotal() {
            return total;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + "("
                    + (percent == PERCENT_INDETERMINATE ? "?"
                            : Float.valueOf(percent))
                    + "%, "
                    + (current == CURRENT_INDETERMINATE ? "?"
                            : Long.valueOf(current)) + "/" + total + ")";
        }
    }

}
