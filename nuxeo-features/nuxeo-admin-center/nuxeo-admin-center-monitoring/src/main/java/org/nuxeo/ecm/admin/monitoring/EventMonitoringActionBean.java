/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.admin.monitoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.management.events.EventMonitoring;

/**
 * Simple Seam Bean that wraps {@link EventMonitoring} to expose it to JSF/Seam layer
 *
 * @author tiry
 */
@Name("eventMonitoringAction")
@Scope(ScopeType.EVENT)
public class EventMonitoringActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected EventMonitoring monitor;

    protected EventMonitoring getEventMonitoring() {
        if (monitor == null) {
            monitor = new EventMonitoring();
        }
        return monitor;
    }

    public int getActiveThreads() {
        return getEventMonitoring().getActiveThreadsCount();
    }

    public int getQueuedEvents() {
        return getEventMonitoring().getEventsInQueueCount();
    }

    @Factory(value = "eventSyncStats", scope = ScopeType.EVENT)
    public List<List<String>> getSyncStats() {
        String stats = getEventMonitoring().getSyncHandlersExecTime();
        return formatStats(stats);
    }

    @Factory(value = "eventAsyncStats", scope = ScopeType.EVENT)
    public List<List<String>> getAsyncStats() {
        String stats = getEventMonitoring().getAsyncHandlersExecTime();
        return formatStats(stats);
    }

    protected List<List<String>> formatStats(String stats) {

        List<List<String>> result = new ArrayList<>();
        if (stats == null || stats.length() == 0) {
            return result;
        }

        String[] lines = stats.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            String[] parts = line.split(" - ");
            List<String> lin = Arrays.asList(parts);
            result.add(lin);
        }
        return result;
    }

    public void refresh() {
        cleanSeamEventCache();
    }

    public String getEventStatistics() {
        StringBuilder sb = new StringBuilder();

        sb.append("Active Threads : ");
        sb.append(getEventMonitoring().getActiveThreadsCount());
        sb.append("\nQueued events : ");
        sb.append(getEventMonitoring().getEventsInQueueCount());

        sb.append("\nSync processing time : ");
        if (getEventMonitoring().isSyncHandlersTrackingEnabled()) {
            sb.append(getEventMonitoring().getSyncHandlersExecTime());
        } else {
            sb.append("[tracking not enabled]");
        }
        sb.append("\nAsync processing time : ");
        if (getEventMonitoring().isAsyncHandlersTrackingEnabled()) {
            sb.append(getEventMonitoring().getAsyncHandlersExecTime());
        } else {
            sb.append("[tracking not enabled]");
        }
        return sb.toString();
    }

    protected void cleanSeamEventCache() {
        Contexts.getEventContext().remove("eventSyncTrackingEnabled");
        Contexts.getEventContext().remove("eventAsyncTrackingEnabled");
        Contexts.getEventContext().remove("eventSyncStats");
        Contexts.getEventContext().remove("eventAsyncStats");
    }

    public void enableSyncTracking() {
        getEventMonitoring().setSyncHandlersTrackingEnabled(true);
        cleanSeamEventCache();
    }

    public void enableAsyncTracking() {
        getEventMonitoring().setAsyncHandlersTrackingEnabled(true);
        cleanSeamEventCache();
    }

    public void disableSyncTracking() {
        getEventMonitoring().setSyncHandlersTrackingEnabled(false);
        cleanSeamEventCache();
    }

    public void disableAsyncTracking() {
        getEventMonitoring().setAsyncHandlersTrackingEnabled(false);
        cleanSeamEventCache();
    }

    @Factory(value = "eventSyncTrackingEnabled", scope = ScopeType.EVENT)
    public boolean isSyncTrackingEnabled() {
        return getEventMonitoring().isSyncHandlersTrackingEnabled();
    }

    @Factory(value = "eventAsyncTrackingEnabled", scope = ScopeType.EVENT)
    public boolean isAsyncTrackingEnabled() {
        return getEventMonitoring().isAsyncHandlersTrackingEnabled();
    }

}
