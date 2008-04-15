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

package org.nuxeo.ecm.platform.events;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.events.mock.MockJMSEventListener;
import org.nuxeo.ecm.platform.events.mock.MockMessageProducer;

public class TestMessageBuffering extends TestCase {

    MockJMSEventListener listener;

    @Override
    public void setUp() {
        listener = new MockJMSEventListener();
        listener.reset();
    }

    public void testSessionStacking() {
        MockMessageProducer producer = (MockMessageProducer) listener.getProducerService();

        listener.fakeSendCoreMessage(DocumentEventTypes.ABOUT_TO_CREATE, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_CREATED, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.ABOUT_TO_CREATE, "1","d2");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_UPDATED, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_CREATED, "1","d2");

        assertEquals(3, listener.getStackedMessageCount("0"));
        assertEquals(2, listener.getStackedMessageCount("1"));

        listener.fakeSendCoreMessage(DocumentEventTypes.SESSION_SAVED, "0",null);
        assertEquals(0, listener.getStackedMessageCount("0"));
        assertEquals(3, producer.producedMessages);
        assertEquals(2, listener.getStackedMessageCount("1"));
    }

    public void testEventDuplication() {
        MockMessageProducer producer = (MockMessageProducer) listener.getProducerService();

        listener.fakeSendCoreMessage(DocumentEventTypes.ABOUT_TO_CREATE, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_CREATED, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_CREATED, "0","d3");
        listener.fakeSendCoreMessage(DocumentEventTypes.ABOUT_TO_CREATE, "1","d2");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_UPDATED, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_UPDATED, "0","d1");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_CREATED, "1","d2");
        listener.fakeSendCoreMessage(DocumentEventTypes.DOCUMENT_UPDATED, "1","d2");

        assertEquals(5, listener.getStackedMessageCount("0"));
        assertEquals(3, listener.getStackedMessageCount("1"));

        listener.fakeSendCoreMessage(DocumentEventTypes.SESSION_SAVED, "0",null);
        assertEquals(0, listener.getStackedMessageCount("0"));
        assertEquals(3, listener.getStackedMessageCount("1"));
        assertEquals(5, producer.producedMessages);
        assertEquals(2, producer.duplicatedMessages);

        listener.fakeSendCoreMessage(DocumentEventTypes.SESSION_SAVED, "1",null);
        assertEquals(8, producer.producedMessages);
        assertEquals(3, producer.duplicatedMessages);
    }

}
