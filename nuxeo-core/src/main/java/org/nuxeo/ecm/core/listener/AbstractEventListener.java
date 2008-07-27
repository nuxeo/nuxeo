/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: AbstractEventListener.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.listener;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;

/**
 * Abstract event listener.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractEventListener implements EventListener {

    private String name;

    private Integer order = 0;

    private List<String> eventIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void addEventId(String eventId) {
        if (eventId == null || eventId.length() == 0) {
            throw new IllegalArgumentException("eventId can't be null or empty");
        }

        if (eventIds == null) {
            eventIds = new ArrayList<String>();
        }
        eventIds.add(eventId);
    }

    public void removeEventId(String eventId) {
        if (eventIds != null) {
            eventIds.remove(eventId);
        }
    }

    public boolean accepts(String eventId) {
        if (eventIds == null || eventIds.isEmpty()) {
            return true;
        } else {
            return eventIds.contains(eventId);
        }
    }

    public void operationStarted(Operation<?> cmd) throws Exception {
        // ignore this for now - avoid breaking compatibility by making it abstract
    }

    public void operationTerminated(Operation<?> cmd) throws Exception {
        // ignore this for now - avoid breaking compatibility by making it abstract
    }

    /**
     * Handles a core event.
     *
     * @param coreEvent the core event to notify
     * @deprecated use handleEvent instead - notifyEvent is a bad name
     * This method is preserved for compatibility - remove it when all event listeners will be refactored
     */
    // TODO: remove in 5.2
    @Deprecated
    public void notifyEvent(CoreEvent coreEvent) throws Exception {
    }

    /**
     * A default implementation required for compatibility with notifyEvent
     * When notifyEvent will be removed remove this default implementation too
     */
    // TODO: remove in 5.2
    @Deprecated
    public void handleEvent(CoreEvent coreEvent) throws Exception {
        notifyEvent(coreEvent);
    }

}
