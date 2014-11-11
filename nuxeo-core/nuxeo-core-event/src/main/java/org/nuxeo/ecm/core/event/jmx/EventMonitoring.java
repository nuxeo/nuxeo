/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.event.jmx;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Monitoring MBean implementation
 *
 * @author Thierry Delprat
 *
 */
public class EventMonitoring implements EventMonitoringMBean {

    private EventServiceAdmin getAdminService() {
        return Framework.getLocalService(EventServiceAdmin.class);
    }

    public boolean isAsyncHandlersTrackingEnabled() {
        return EventStatsHolder.isCollectAsyncHandlersExecTime();
    }

    public void setAsyncHandlersTrackingEnabled(boolean collectAsyncHandlersExecTime) {
        EventStatsHolder
                .setCollectAsyncHandlersExecTime(collectAsyncHandlersExecTime);
    }

    public boolean isSyncHandlersTrackingEnabled() {
        return EventStatsHolder.isCollectSyncHandlersExecTime();
    }

    public void setSyncHandlersTrackingEnabled(boolean collectSyncHandlersExecTime) {
        EventStatsHolder
                .setCollectSyncHandlersExecTime(collectSyncHandlersExecTime);
    }

    public int getActiveThreadsCount() {
        return getAdminService().getActiveThreadsCount();
    }

    public int getEventsInQueueCount() {
        return getAdminService().getEventsInQueueCount();
    }

    public String getAsyncHandlersExecTime() {
        return EventStatsHolder.getAsyncHandlersExecTime();
    }

    public String getSyncHandlersExecTime() {
        return EventStatsHolder.getSyncHandlersExecTime();
    }

    public boolean isBlockAsyncHandlers() {
        return getAdminService().isBlockAsyncHandlers();
    }

    public void setBlockAsyncHandlers(boolean blockAsyncHandlers) {
        getAdminService().setBlockAsyncHandlers(blockAsyncHandlers);
    }

    public boolean isBlockSyncPostCommitHandlers() {
        return getAdminService().isBlockSyncPostCommitHandlers();
    }

    public void setBlockSyncPostCommitHandlers(boolean blockSyncPostCommitHandlers) {
        getAdminService().setBlockSyncPostCommitHandlers(
                blockSyncPostCommitHandlers);
    }

    public void resetHandlersExecTime() {
        EventStatsHolder.resetHandlersExecTime();
    }

    public String getListenersConfig() {

        EventListenerList listenerList = getAdminService().getListenerList();
        StringBuffer sb = new StringBuffer();
        for (EventListenerDescriptor desc : listenerList
                .getAsyncPostCommitListenersDescriptors()) {
            sb.append(desc.getName());
            sb.append(" - Async PostCommit - ");
            sb.append(desc.isEnabled());
            sb.append("\n");
        }
        for (EventListenerDescriptor desc : listenerList
                .getSyncPostCommitListenersDescriptors()) {
            sb.append(desc.getName());
            sb.append(" - Sync PostCommit - ");
            sb.append(desc.isEnabled());
            sb.append("\n");
        }
        for (EventListenerDescriptor desc : listenerList
                .getInlineListenersDescriptors()) {
            sb.append(desc.getName());
            sb.append(" - Synchronous - ");
            sb.append(desc.isEnabled());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void setListenerEnabledFlag(String listenerName, boolean enabled) {
        getAdminService().setListenerEnabledFlag(listenerName, enabled);
    }

    public boolean isBulkModeEnabled() {
        return getAdminService().isBulkModeEnabled();
    }

    public void setBulkModeEnabled(boolean bulkModeEnabled) {
        getAdminService().setBulkModeEnabled(bulkModeEnabled);
    }

}
