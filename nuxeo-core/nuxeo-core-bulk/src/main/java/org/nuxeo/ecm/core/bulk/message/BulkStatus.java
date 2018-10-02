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

package org.nuxeo.ecm.core.bulk.message;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.bulk.io.InstantAsLongEncoding;
import org.omg.CORBA.UNKNOWN;

/**
 * A message representing a command status or a change of status (delta).
 *
 * @since 10.2
 */
public class BulkStatus implements Serializable {

    private static final long serialVersionUID = 20181021L;

    /**
     * Possible states of bulk execution.
     */
    public enum State {
        UNKNOWN,

        /** The {@link BulkCommand} has been submitted to the system. */
        SCHEDULED,

        /** System is currently scrolling the database and computing the action. */
        SCROLLING_RUNNING,

        /** System is currently computing the action (scrolling is finished). */
        RUNNING,

        /** System has finished to scroll. */
        COMPLETED
    }

    protected String commandId;

    protected boolean delta;

    @Nullable
    protected State state;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant submitTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollStartTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollEndTime;

    @Nullable
    protected Long processed;

    @Nullable
    protected Long count;

    @Nullable
    @AvroEncode(using = MapAsJsonAsStringEncoding.class)
    protected Map<String, Serializable> result = new HashMap<>();

    /**
     * Creates a delta status for a command.
     */
    public static BulkStatus deltaOf(String commandId) {
        BulkStatus ret = new BulkStatus();
        ret.setCommandId(commandId);
        ret.delta = true;
        return ret;
    }

    /**
     * Creates a delta status for a command.
     */
    public static BulkStatus unknownOf(String commandId) {
        BulkStatus ret = new BulkStatus();
        ret.setCommandId(commandId);
        ret.delta = true;
        ret.setState(State.UNKNOWN);
        return ret;
    }

    /**
     * Updates the status with the provided update.
     *
     * @since 10.3
     */
    public void merge(BulkStatus update) {
        if (!update.isDelta()) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge an a full status: %s with %s", this, update));
        }
        if (update.getState() != null) {
            setState(update.getState());
        }
        if (update.processed != null) {
            setProcessed(getProcessed() + update.getProcessed());
        }
        if (update.getScrollStartTime() != null) {
            setScrollStartTime(update.getScrollStartTime());
        }
        if (update.getScrollEndTime() != null) {
            setScrollEndTime(update.getScrollEndTime());
        }
        if (update.getSubmitTime() != null) {
            setSubmitTime(update.getSubmitTime());
        }
        if (update.count != null) {
            setCount(update.getCount());
        }
        checkForCompletedState();
    }

    protected void checkForCompletedState() {
        if (!isDelta() && getCount() > 0 && getProcessed() >= getCount()) {
            setState(State.COMPLETED);
        }
    }

    /**
     * Gets command id.
     *
     * @return the id
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * Sets bulk command id.
     *
     * @param id the id
     */
    public void setCommandId(String id) {
        this.commandId = id;
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
    public long getProcessed() {
        if (processed == null) {
            return 0;
        }
        return processed;
    }

    /**
     * Sets number of elements processed in this bulk.
     *
     * @param processed the number of elements
     */
    public void setProcessed(long processed) {
        this.processed = processed;
    }

    /**
     * Gets number of element touched in this bulk.
     *
     * @return the number of element
     */
    public long getCount() {
        if (count == null) {
            return 0;
        }
        return count;
    }

    /**
     * Sets number of element touched in this bulk.
     *
     * @param count the number of element
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * Gets action result.
     *
     * @return the action result
     * @since 10.3
     */
    public Map<String, Serializable> getResult() {
        return Collections.unmodifiableMap(result);
    }

    /**
     * Sets action result.
     *
     * @param result the action result
     * @since 10.3
     */
    public void setResult(Map<String, Serializable> result) {
        this.result = result;
    }

    /**
     * This is an update of a status containing only partial information.
     *
     * @since 10.3
     */
    public boolean isDelta() {
        return delta;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
