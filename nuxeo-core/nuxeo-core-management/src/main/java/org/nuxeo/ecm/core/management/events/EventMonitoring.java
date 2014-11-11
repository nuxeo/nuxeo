/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.events;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.runtime.api.Framework;

/**
 * Monitoring MBean implementation.
 *
 * @author Thierry Delprat
 */
public class EventMonitoring implements EventMonitoringMBean {

    private static EventServiceAdmin getAdminService() {
        return Framework.getLocalService(EventServiceAdmin.class);
    }

    @Override
    public boolean isAsyncHandlersTrackingEnabled() {
        return EventStatsHolder.isCollectAsyncHandlersExecTime();
    }

    @Override
    public void setAsyncHandlersTrackingEnabled(boolean collectAsyncHandlersExecTime) {
        EventStatsHolder
                .setCollectAsyncHandlersExecTime(collectAsyncHandlersExecTime);
    }

    @Override
    public boolean isSyncHandlersTrackingEnabled() {
        return EventStatsHolder.isCollectSyncHandlersExecTime();
    }

    @Override
    public void setSyncHandlersTrackingEnabled(boolean collectSyncHandlersExecTime) {
        EventStatsHolder
                .setCollectSyncHandlersExecTime(collectSyncHandlersExecTime);
    }

    @Override
    public int getActiveThreadsCount() {
        return getAdminService().getActiveThreadsCount();
    }

    @Override
    public int getEventsInQueueCount() {
        return getAdminService().getEventsInQueueCount();
    }

    @Override
    public String getAsyncHandlersExecTime() {
        return EventStatsHolder.getAsyncHandlersExecTime();
    }

    @Override
    public String getSyncHandlersExecTime() {
        return EventStatsHolder.getSyncHandlersExecTime();
    }

    @Override
    public boolean isBlockAsyncHandlers() {
        return getAdminService().isBlockAsyncHandlers();
    }

    @Override
    public void setBlockAsyncHandlers(boolean blockAsyncHandlers) {
        getAdminService().setBlockAsyncHandlers(blockAsyncHandlers);
    }

    @Override
    public boolean isBlockSyncPostCommitHandlers() {
        return getAdminService().isBlockSyncPostCommitHandlers();
    }

    @Override
    public void setBlockSyncPostCommitHandlers(boolean blockSyncPostCommitHandlers) {
        getAdminService().setBlockSyncPostCommitHandlers(
                blockSyncPostCommitHandlers);
    }

    @Override
    public void resetHandlersExecTime() {
        EventStatsHolder.resetHandlersExecTime();
    }

    @Override
    public String getListenersConfig() {

        EventListenerList listenerList = getAdminService().getListenerList();
        StringBuilder sb = new StringBuilder();
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

    @Override
    public void setListenerEnabledFlag(String listenerName, boolean enabled) {
        getAdminService().setListenerEnabledFlag(listenerName, enabled);
    }

    @Override
    public boolean isBulkModeEnabled() {
        return getAdminService().isBulkModeEnabled();
    }

    @Override
    public void setBulkModeEnabled(boolean bulkModeEnabled) {
        getAdminService().setBulkModeEnabled(bulkModeEnabled);
    }

}
