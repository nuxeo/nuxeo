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

/**
 * A message representing a command status or a change of status (delta).
 *
 * @since 10.2
 */
public class BulkStatus implements Serializable {

    private static final long serialVersionUID = 20181021L;

    /**
     * Possible states of a bulk command:
     */
    public enum State {
        /** The command or the status is unknown. */
        UNKNOWN,

        /** The command has been submitted to the service. */
        SCHEDULED,

        /** The scroller is running and materialize the document set. */
        SCROLLING_RUNNING,

        /** The scroller has terminated, the size of the document set is known and action is applied. */
        RUNNING,

        /** The action has been applied to the document set, the command is completed. */
        COMPLETED
    }

    protected String commandId;

    @Nullable
    protected String action;

    protected boolean delta;

    @Nullable
    protected Long processed;

    @Nullable
    protected State state;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant submitTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollStartTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollEndTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant completedTime;

    @Nullable
    protected Long total;

    @Nullable
    @AvroEncode(using = MapAsJsonAsStringEncoding.class)
    protected Map<String, Serializable> result = new HashMap<>();

    protected BulkStatus() {
        // Empty constructor for Avro decoder
    }

    public BulkStatus(String commandId) {
        this.commandId = commandId;
    }

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
        if (!getCommandId().equals(update.getCommandId())) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge different command: %s with %s", this, update));
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
        if (update.getCompletedTime() != null) {
            setCompletedTime(update.getCompletedTime());
        }
        if (update.total != null) {
            setTotal(update.getTotal());
        }
        if (update.action != null) {
            setAction(update.action);
        }
        if (update.getResult() != null) {
            setResult(update.getResult());
        }
        checkForCompletedState();
    }

    protected void checkForCompletedState() {
        if (!isDelta() && getTotal() > 0 && getProcessed() >= getTotal()) {
            if (!State.COMPLETED.equals(getState())) {
                setState(State.COMPLETED);
                setCompletedTime(Instant.now());
            }
        }
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String id) {
        this.commandId = id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Instant getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Instant submitTime) {
        this.submitTime = submitTime;
    }

    public Instant getScrollStartTime() {
        return scrollStartTime;
    }

    public void setScrollStartTime(Instant scrollStartTime) {
        this.scrollStartTime = scrollStartTime;
    }

    public Instant getScrollEndTime() {
        return scrollEndTime;
    }

    public void setScrollEndTime(Instant scrollEndTime) {
        this.scrollEndTime = scrollEndTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }

    /**
     * For a full status returns the number of documents where the action has been applied so far.
     */
    public long getProcessed() {
        if (processed == null) {
            return 0;
        }
        return processed;
    }

    /**
     * Sets number of processed documents. For a delta this is a relative value that is aggregated during
     * {@link #merge(BulkStatus)} operation.
     */
    public void setProcessed(long processed) {
        this.processed = processed;
    }

    /**
     * Gets the total number of documents in the document set. Returns 0 when the scroll is not yet completed.
     */
    public long getTotal() {
        if (total == null) {
            return 0;
        }
        return total;
    }

    /**
     * Sets the total number of documents in the document set
     */
    public void setTotal(long count) {
        this.total = count;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
