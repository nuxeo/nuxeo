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

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.avro.reflect.AvroDefault;
import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.AsyncStatus;

/**
 * A message representing a command status or a change of status (delta).
 *
 * @since 10.2
 */
public class BulkStatus implements AsyncStatus<String> {

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
        COMPLETED,

        /** The command has been aborted, the action might have been partially applied on the document set. */
        ABORTED
    }

    protected String commandId;

    @Nullable
    protected String action;

    @Nullable
    protected String username;

    protected boolean delta;

    protected long errorCount;

    @Nullable
    protected String errorMessage;

    /** @since 11.5 **/
    @Nullable
    protected Integer errorCode;

    @Nullable
    protected Long processed;

    @Nullable
    protected State state;

    @Nullable
    protected Long submitTime;

    @Nullable
    protected Long scrollStartTime;

    @Nullable
    protected Long scrollEndTime;

    @Nullable
    protected Long processingStartTime;

    @Nullable
    protected Long processingEndTime;

    @Nullable
    protected Long completedTime;

    @Nullable
    protected Long total;

    @Nullable
    protected Long processingDurationMillis;

    @AvroDefault("false")
    protected boolean queryLimitReached;

    @Nullable
    @AvroEncode(using = MapAsJsonAsStringEncoding.class)
    protected Map<String, Serializable> result = new HashMap<>();

    protected BulkStatus() {
        // Empty constructor for Avro decoder
    }

    public BulkStatus(@NotNull String commandId) {
        this.commandId = commandId;
    }

    /**
     * Creates a delta status for a command.
     */
    public static BulkStatus deltaOf(@NotNull String commandId) {
        BulkStatus ret = new BulkStatus();
        ret.setId(commandId);
        ret.delta = true;
        return ret;
    }

    /**
     * Creates a delta status for a command.
     */
    public static BulkStatus unknownOf(@NotNull String commandId) {
        BulkStatus ret = new BulkStatus();
        ret.setId(commandId);
        ret.delta = true;
        ret.setState(State.UNKNOWN);
        return ret;
    }

    /**
     * Updates the status with the provided update.
     *
     * @since 10.3
     */
    public void merge(@NotNull BulkStatus update) {
        if (!update.isDelta()) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge an a full status: %s with %s", this, update));
        }
        if (!getId().equals(update.getId())) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge different command: %s with %s", this, update));
        }
        if (update.getState() != null && getState() != State.ABORTED) {
            setState(update.getState());
        }
        if (update.processed != null) {
            setProcessed(getProcessed() + update.getProcessed());
        }
        if (update.scrollStartTime != null) {
            scrollStartTime = update.scrollStartTime;
        }
        if (update.scrollEndTime != null) {
            scrollEndTime = update.scrollEndTime;
        }
        if (update.submitTime != null) {
            submitTime = update.submitTime;
        }
        if (update.processingStartTime != null
                && (processingStartTime == null || update.processingStartTime < processingStartTime)) {
            // we take the minimum
            processingStartTime = update.processingStartTime;
        }
        if (update.processingEndTime != null
                && (processingEndTime == null || update.processingEndTime > processingEndTime)) {
            // we take the maximum
            processingEndTime = update.processingEndTime;
        }
        if (update.processingStartTime != null && update.processingEndTime != null) {
            long deltaDuration = update.processingEndTime - update.processingStartTime;
            setProcessingDurationMillis(getProcessingDurationMillis() + deltaDuration);
        }
        if (update.completedTime != null) {
            completedTime = update.completedTime;
        }
        if (update.total != null) {
            setTotal(update.getTotal());
        }
        if (update.getAction() != null && getAction() == null) {
            setAction(update.action);
        }
        if (update.getResult() != null) {
            setResult(update.getResult());
        }
        if (update.getUsername() != null && getUsername() == null) {
            setUsername(getUsername());
        }
        if (update.errorCount > 0) {
            errorCount += update.errorCount;
        }
        if (update.errorMessage != null && errorMessage == null) {
            errorMessage = update.errorMessage;
        }
        if (update.errorCode != null && errorCode == null) {
            errorCode = update.errorCode;
        }
        if (update.queryLimitReached) {
            queryLimitReached = true;
        }
        checkForCompletedState();
    }

    protected void checkForCompletedState() {
        if (!isDelta() && getTotal() > 0 && getProcessed() >= getTotal()) {
            if (getState() != State.COMPLETED && getState() != State.ABORTED) {
                setState(State.COMPLETED);
                setCompletedTime(Instant.now());
            }
        }
    }

    @Override
    public String getId() {
        return commandId;
    }

    public void setId(String id) {
        this.commandId = id;
    }

    /**
     * Gets the state of the command.
     */
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /**
     * Gets the time when the command was submitted to the Bulk service.
     */
    public Instant getSubmitTime() {
        return submitTime == null ? null : Instant.ofEpochMilli(submitTime);
    }

    public void setSubmitTime(@NotNull Instant submitTime) {
        this.submitTime = submitTime.toEpochMilli();
    }

    /**
     * Gets the time when the scroll computation starts.
     */
    public Instant getScrollStartTime() {
        return scrollStartTime == null ? null : Instant.ofEpochMilli(scrollStartTime);
    }

    public void setScrollStartTime(@NotNull Instant scrollStartTime) {
        this.scrollStartTime = scrollStartTime.toEpochMilli();
    }

    /**
     * Gets the time when the scrolling is completed.
     */
    public Instant getScrollEndTime() {
        return scrollEndTime == null ? null : Instant.ofEpochMilli(scrollEndTime);
    }

    public void setScrollEndTime(@NotNull Instant scrollEndTime) {
        this.scrollEndTime = scrollEndTime.toEpochMilli();
    }

    /**
     * Gets the time when the action computation starts.
     */
    public Instant getProcessingStartTime() {
        return processingStartTime == null ? null : Instant.ofEpochMilli(processingStartTime);
    }

    public void setProcessingStartTime(@NotNull Instant processingStartTime) {
        this.processingStartTime = processingStartTime.toEpochMilli();
    }

    /**
     * Gets the time when the last action computation has terminated.
     */
    public Instant getProcessingEndTime() {
        return processingEndTime == null ? null : Instant.ofEpochMilli(processingEndTime);
    }

    public void setProcessingEndTime(@NotNull Instant processingEndTime) {
        this.processingEndTime = processingEndTime.toEpochMilli();
    }

    /**
     * Gets the time when the command has been detected as completed.
     */
    public Instant getCompletedTime() {
        return completedTime == null ? null : Instant.ofEpochMilli(completedTime);
    }

    public void setCompletedTime(@NotNull Instant completedTime) {
        this.completedTime = completedTime.toEpochMilli();
    }

    /**
     * Returns true if the query used by the scroller has been limited.
     *
     * @since 11.4
     */
    public boolean isQueryLimitReached() {
        return queryLimitReached;
    }

    /**
     * @since 11.4
     */
    public void setQueryLimitReached(boolean queryLimitReached) {
        this.queryLimitReached = queryLimitReached;
    }

    @Override
    public boolean isCompleted() {
        return getState() == State.COMPLETED;
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
     * This is an update of a status containing only partial information. For a delta the processing start and end time,
     * and the processed count are also delta.
     *
     * @since 10.3
     */
    public boolean isDelta() {
        return delta;
    }

    /**
     * Gets the action name of the command.
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets the username of the user running the command.
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the accumulated processing time in milliseconds.
     */
    public long getProcessingDurationMillis() {
        if (processingDurationMillis == null) {
            return 0;
        }
        return processingDurationMillis;
    }

    public void setProcessingDurationMillis(long processingDurationMillis) {
        this.processingDurationMillis = processingDurationMillis;
    }

    @Override
    public boolean hasError() {
        return errorCount > 0;
    }

    /**
     * Returns the first error message if any or null.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the number of errors encountered
     */
    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * An error occurred during the processing
     */
    public void inError(String message) {
        inError(1, message, SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * An error occurred during the processing
     */
    public void inError(long count, String message) {
        inError(count, message, SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * An error occurred during the processing
     *
     * @since 11.5
     */
    public void inError(String message, int code) {
        inError(1, message, code);
    }

    /**
     * An error occurred during the processing
     */
    public void inError(long count, String message, int code) {
        this.errorCount += count;
        if (StringUtils.isBlank(this.errorMessage)) {
            this.errorMessage = message;
        }
        if (this.errorCode == null) {
            this.errorCode = code;
        }
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

    @Override
    public int getErrorCode() {
        if (errorCode == null) {
            return 0;
        }
        return errorCode;
    }
}
