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
