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
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * A {@link Work} instance gets executed by a {@link WorkManager}.
 * <p>
 * It's a runnable that can be {@linkplain #suspend() suspended} and can report
 * its status and progress.
 * <p>
 * Implementors must take care to implement correctly {@link #beforeRun} and
 * {@link #afterRun} to change the state.
 *
 * @see AbstractWork
 * @since 5.6
 */
public interface Work extends Runnable {

    /**
     * The running state of a {@link Work} instance.
     */
    enum State {
        /** Work instance is scheduled but not yet running. */
        SCHEDULED,
        /** Work instance is running. */
        RUNNING,
        /** Work instance is running but should suspend work. */
        SUSPENDING,
        /** Work instance is suspended in memory and work is stopped. */
        SUSPENDED,
        /** Work instance has completed. */
        COMPLETED,
        /** Work instance has failed. */
        FAILED,
        /** Work instance was canceled. */
        CANCELED
    }

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
            this.percent = percent > 100F ? 100F : percent;
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

        public boolean getIsWithPercent() {
            return percent != PERCENT_INDETERMINATE;
        }

        public boolean getIsWithCurrentAndTotal() {
            return current != CURRENT_INDETERMINATE;
        }

        public boolean getIsIndeterminate() {
            return percent == PERCENT_INDETERMINATE
                    && current == CURRENT_INDETERMINATE;
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

    /**
     * Gets the category for this work.
     * <p>
     * Used to choose a thread pool queue.
     *
     * @return the category, or {@code null} for the default
     */
    String getCategory();

    /**
     * Called by the thread pool executor before the work is run. Must set the
     * proper state (RUNNING if not already SUSPENDED).
     */
    void beforeRun();

    /**
     * The actual work.
     */
    @Override
    void run();

    /**
     * Called by the thread pool executor after the work is run. Must set the
     * proper state (COMPLETED, SUSPENDED or FAILED).
     *
     * @param ok {@code false} if there was an exception during task run and the
     *            state should be FAILED
     */
    void afterRun(boolean ok);

    /**
     * Gets the running state for this work instance.
     *
     * @return the running state
     */
    State getState();

    /**
     * Gets a human-readable name for this work instance.
     *
     * @return a human-readable name
     */
    String getTitle();

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
     * Requests that this work instance suspend its state in memory.
     * <p>
     * Does not block. Use {@link #awaitTermination} to wait for actual
     * suspension and then {@link #getData} to get the data.
     * <p>
     * A QUEUED work instance must immediately suspend.
     *
     * @return {@code true} if suspend was started, or {@code false} if the job
     *         was already completed
     */
    boolean suspend();

    /**
     * Wait for the work to be completed or suspension finished.
     *
     * @param timeout how long to wait
     * @param unit the timeout unit
     * @return {@code true} if the work is completed or suspended, or
     *         {@code false} for a timeout
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Sets the state of this queued work instance to {@link State#CANCELED
     * CANCELED}. Called by the work manager implementation.
     */
    void setCanceled();

    /**
     * Gets the state data for this suspended work instance.
     *
     * @return the data allowing the work instance to be resumed, or
     *         {@code null} if no data is available
     */
    Map<String, Serializable> getData();

    /**
     * Restores a saved state data for this work instance.
     *
     * @param data the saved state data
     */
    void setData(Map<String, Serializable> data);

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

}
