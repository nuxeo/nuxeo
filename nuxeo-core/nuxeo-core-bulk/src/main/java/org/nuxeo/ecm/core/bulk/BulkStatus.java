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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.reflect.AvroEncode;
import org.nuxeo.ecm.core.bulk.io.InstantAsLongEncoding;
import org.nuxeo.ecm.core.bulk.io.MapAsJsonAsStringEncoding;

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
        COMPLETED;

    }

    protected String id;

    protected State state;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant submitTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollStartTime;

    @AvroEncode(using = InstantAsLongEncoding.class)
    protected Instant scrollEndTime;

    protected long processed;

    protected long count;

    @AvroEncode(using = MapAsJsonAsStringEncoding.class)
    protected Map<String, Serializable> result = new HashMap<>();

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

}
