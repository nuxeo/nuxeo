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
package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.event.impl.EventListenerList;

/**
 * Interface for EventService administration
 *
 * @author Thierry Delprat
 */
public interface EventServiceAdmin {

    int getEventsInQueueCount();

    int getActiveThreadsCount();

    boolean isBlockAsyncHandlers();

    void setBlockAsyncHandlers(boolean blockAsyncHandlers);

    boolean isBlockSyncPostCommitHandlers();

    void setBlockSyncPostCommitHandlers(boolean blockSyncPostCommitHandlers);

    EventListenerList getListenerList();

    void setListenerEnabledFlag(String listenerName, boolean enabled);

    boolean isBulkModeEnabled();

    void setBulkModeEnabled(boolean bulkModeEnabled);

}
