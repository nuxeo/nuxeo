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
 * $Id: DocumentMessageProducer.java 29091 2008-01-17 00:40:09Z tdelprat $
 */

package org.nuxeo.ecm.platform.events.api;

import java.util.List;

/**
 * Document message producer interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DocumentMessageProducer {

    /**
     * Produces a document message on the target messaging channel.
     *
     * @param message a DocumentMessage instance
     * @deprecated Use {@link #produce(EventMessage)} instead
     */
    @Deprecated
    void produce(DocumentMessage message);

    /**
     * Produces a document message on the target messaging channel.
     *
     * @param message a DocumentMessage instance
     */
    void produce(EventMessage message);

    /**
     * Produces a event on the target messaging channel.
     *
     * @param event a NXCoreEvent instance
     */
    void produce(NXCoreEvent event);


    void produceEventMessages(List<EventMessage> messages);


    void produceCoreEvents(List<NXCoreEvent> events);
}
