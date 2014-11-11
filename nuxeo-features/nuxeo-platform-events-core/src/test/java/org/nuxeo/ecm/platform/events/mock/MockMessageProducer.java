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
 * $Id$
 */

package org.nuxeo.ecm.platform.events.mock;

import java.util.List;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;

public class MockMessageProducer implements DocumentMessageProducer {

    public int producedMessages = 0;
    public int duplicatedMessages = 0;

    public void produce(DocumentMessage message) {
    }

    public void produce(EventMessage message) {
    }

    public void produce(NXCoreEvent event) {
    }

    public void produceCoreEvents(List<NXCoreEvent> events) {
    }

    public void produceEventMessages(List<EventMessage> messages) {
        for (EventMessage evt : messages) {
            producedMessages += 1;

            Boolean dup = (Boolean) evt.getEventInfo().get(EventMessage.DUPLICATED);
            if (dup != null && dup) {
                duplicatedMessages += 1;
            }
        }
    }

}
