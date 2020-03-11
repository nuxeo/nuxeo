/*******************************************************************************
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *******************************************************************************/
package org.nuxeo.ecm.core.work.api;

import java.io.Serializable;

/**
 * Provides coherent queue metrics
 *
 * @since 8.3
 *
 */
public class WorkQueueMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String queueId;

    public final Number scheduled;

    public final Number running;

    public final Number completed;

    public final Number canceled;

    public WorkQueueMetrics(String queueId, Number scheduled, Number running, Number completed, Number canceled) {
        this.queueId = queueId;
        this.scheduled = scheduled;
        this.running = running;
        this.completed = completed;
        this.canceled = canceled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +  queueId.hashCode();
        result = prime * result +  scheduled.hashCode();
        result = prime * result +  running.hashCode();
        result = prime * result +  completed.hashCode();
        result = prime * result +  canceled.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WorkQueueMetrics)) {
            return false;
        }
        WorkQueueMetrics other = (WorkQueueMetrics) obj;
        if (!queueId.equals(other.queueId)) {
            return false;
        }
        if (scheduled.longValue() != other.scheduled.longValue()) {
            return false;
        }
        if (running.longValue() != other.running.longValue()) {
            return false;
        }
        if (completed.longValue() != other.completed.longValue()) {
            return false;
        }
        if (canceled.longValue() != other.canceled.longValue()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[")
                .append(queueId)
                .append(", ")
                .append(scheduled)
                .append(", ")
                .append(running)
                .append(", ")
                .append(completed)
                .append(", ")
                .append(canceled)
                .append("]");
        return builder.toString();
    }

    public String getQueueId() {
        return queueId;
    }

    public Number getScheduled() {
        return scheduled;
    }

    public Number getRunning() {
        return running;
    }

    public Number getCompleted() {
        return completed;
    }

    public Number getCanceled() {
        return canceled;
    }
}