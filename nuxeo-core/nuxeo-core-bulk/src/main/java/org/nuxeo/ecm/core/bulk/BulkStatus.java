/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk;

import java.io.Serializable;
import java.time.Instant;

/**
 * This object holds the current bulk command execution status.
 * <p/>
 * This aggregates status and metrics of documentSet creation and action computation.
 *
 * @since 10.2
 */
public class BulkStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Possible states of bulk execution.
     */
    public enum State {
        /** The {@link BulkCommand} has been submitted to the system. */
        SCHEDULED,

        /** System is currently scrolling the database and computing the action. */
        SCROLLING_RUNNING,

        /** System is currently computing the action (scrolling is finished). */
        RUNNING,

        /** System has finished to scroll. */
        COMPLETED
    }

    protected String id;

    protected BulkCommand command;

    protected State state;

    protected Instant submitTime;

    protected Instant scrollStartTime;

    protected Instant scrollEndTime;

    protected Long processed;

    protected Long count;

    /**
     * Gets bulk id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets bulk command id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets bulk command.
     *
     * @return the bulk command.
     */
    public BulkCommand getCommand() {
        return command;
    }

    /**
     * Sets bulk command.
     *
     * @param command the bulk command
     */
    public void setCommand(BulkCommand command) {
        this.command = command;
    }

    /**
     * Returns the bulk action state.
     *
     * @return the bulk action state
     */
    public State getState() {
        return state;
    }

    /**
     * Sets bulk action state.
     *
     * @param state the state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Gets bulk action submission time.
     *
     * @return the submit time
     */
    public Instant getSubmitTime() {
        return submitTime;
    }

    /**
     * Sets bulk submission time.
     *
     * @param submitTime the submit time
     */
    public void setSubmitTime(Instant submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * Gets bulk action scroll start time.
     *
     * @return the scroll start time
     */
    public Instant getScrollStartTime() {
        return scrollStartTime;
    }

    /**
     * Sets bulk scroll start time.
     *
     * @param scrollStartTime the scroll start time
     */
    public void setScrollStartTime(Instant scrollStartTime) {
        this.scrollStartTime = scrollStartTime;
    }

    /**
     * Gets bulk action scroll end time.
     *
     * @return the scroll end time
     */
    public Instant getScrollEndTime() {
        return scrollEndTime;
    }

    /**
     * Sets bulk scroll end time.
     *
     * @param scrollEndTime the scroll end time
     */
    public void setScrollEndTime(Instant scrollEndTime) {
        this.scrollEndTime = scrollEndTime;
    }

    /**
     * Gets number of elements processed in this bulk.
     *
     * @return the number of processed elements
     */
    public Long getProcessed() {
        return processed;
    }

    /**
     * Sets number of elements processed in this bulk.
     *
     * @param processed the number of elements
     */
    public void setProcessed(Long processed) {
        this.processed = processed;
    }

    /**
     * Gets number of element touched in this bulk.
     *
     * @return the number of element
     */
    public Long getCount() {
        return count;
    }

    /**
     * Sets number of element touched in this bulk.
     *
     * @param count the number of element
     */
    public void setCount(Long count) {
        this.count = count;
    }
}
