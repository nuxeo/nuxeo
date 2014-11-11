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

/**
 * Monitoring interface.
 *
 * @author Thierry Delprat
 */
public interface EventMonitoringMBean {

    /**
     * Gets the number of events in process or waiting for available slots.
     */
    int getEventsInQueueCount();

    /**
     * Gets number of active threads.
     */
    int getActiveThreadsCount();

    /**
     * Tells if time tracking is active for Async event handlers.
     */
    boolean isAsyncHandlersTrackingEnabled();

    /**
     * Enables or disables time tracking for Async event handlers.
     */
    void setAsyncHandlersTrackingEnabled(boolean collectAsyncHandlersExecTime);


    /**
     * Tells if time tracking is active for Sync event handlers.
     */
    boolean isSyncHandlersTrackingEnabled();

    /**
     * Enables or disables time tracking for Sync event handlers.
     */
    void setSyncHandlersTrackingEnabled(boolean collectSyncHandlersExecTime);

    /**
     * Returns the statistics for Async Handlers.
     */
    String getAsyncHandlersExecTime();

    /**
     * Returns the statistics for Sync Handlers.
     */
    String getSyncHandlersExecTime();

    /**
     * Resets the statistics.
     */
    void resetHandlersExecTime();

    /**
     * Tells if async handlers execution is blocked.
     */
    boolean isBlockAsyncHandlers();

    /**
     * Blocks or enables async handlers execution.
     */
    void setBlockAsyncHandlers(boolean blockAsyncHandlers);

    /**
     * Tells if post-commit sync handlers execution is blocked.
     */
    boolean isBlockSyncPostCommitHandlers();

    /**
     * Blocks or enables post-commit sync handlers execution.
     */
    void setBlockSyncPostCommitHandlers(boolean blockSyncPostCommitHandlers);

    /**
     * Get a summary of all registered listeners
     * (name - type - enabled).
     */
    String getListenersConfig();

    /**
     * Enables or disables a listener by its name.
     */
    void setListenerEnabledFlag(String listenerName, boolean enabled);

    /**
     * Getter for bulkMode processing.
     * Bulk mode may be used by listeners to drop some processing.
     */
    boolean isBulkModeEnabled();

    /**
     * Enables/Disables bulkMode processing.
     */
    void setBulkModeEnabled(boolean bulkModeEnabled);

}
