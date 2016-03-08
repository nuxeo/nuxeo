/*******************************************************************************
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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