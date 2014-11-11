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
 *
 * $Id: EventListener.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.listener;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.operation.Operation;

/**
 * EventListener interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface EventListener {

    /**
     * Returns the name of the event listener.
     *
     * @return the name of the event listener
     */
    String getName();

    /**
     * Sets the name of the event listener.
     *
     * @param name
     *            of the event listener
     */
    void setName(String name);

    /**
     * Returns the int order of the event listener.
     *
     * @return the int order of the event listener.
     */
    Integer getOrder();

    /**
     * Sets the int order of the event listener.
     *
     * @param order
     *            the Integer order of the event listener.
     */
    void setOrder(Integer order);

    /**
     * Adds an event id for this listener to process.
     */
    void addEventId(String eventId);

    /**
     * Removes an event id for this listener to process.
     */
    void removeEventId(String eventId);

    /**
     * Returns true if listener processes given event id.
     * <p>
     * If no event ids are set for this listener, returns true.
     */
    boolean accepts(String eventId);

    /**
     * Handle the given event
     * @param coreEvent
     * @throws Exception
     */
    void handleEvent(CoreEvent coreEvent) throws Exception;

    void operationStarted(Operation<?> cmd) throws Exception;

    void operationTerminated(Operation<?> cmd) throws Exception;

}
