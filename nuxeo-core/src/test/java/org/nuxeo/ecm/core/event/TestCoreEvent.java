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
 *
 * $Id: TestCoreEvent.java 28563 2008-01-08 08:56:29Z sfermigier $
 */

package org.nuxeo.ecm.core.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.model.Document;

/**
 * CoreSession event test case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestCoreEvent extends MockObjectTestCase {

    public void testCoreEventVarious() {
        String eventId = "someEvent";
        String source = "someDummySource";

        CoreEvent coreEvent = new CoreEventImpl(null, null, null, null, null, null);
        assertNotNull(coreEvent);
        assertNull(coreEvent.getEventId());
        assertNull(coreEvent.getSource());
        assertNull(coreEvent.getInfo());

        CoreEvent coreEvent1 = new CoreEventImpl(eventId, null, null, null, null, null);
        assertNotNull(coreEvent1);
        assertEquals(eventId, coreEvent1.getEventId());
        assertNull(coreEvent1.getSource());
        assertNull(coreEvent1.getInfo());

        CoreEvent coreEvent2 = new CoreEventImpl(eventId, source, null, null, null, null);
        assertNotNull(coreEvent2);
        assertEquals(eventId, coreEvent2.getEventId());
        assertEquals(source, coreEvent2.getSource());
        assertNull(coreEvent2.getInfo());

        Map<String, Serializable> info = null;
        CoreEvent coreEvent3 = new CoreEventImpl(eventId, source, info, null, null, null);
        assertNotNull(coreEvent3);
        assertEquals(eventId, coreEvent3.getEventId());
        assertEquals(source, coreEvent3.getSource());
        assertEquals(info, coreEvent3.getInfo());
    }

    // ::FIXME:
    public void testCoreEventWithMockDocument() {
        String eventId = "someEvent";
        Map<String, Serializable> info = null;

        CoreEvent coreEvent = new CoreEventImpl(eventId, null, info, null, null, null);
        assertEquals(eventId, coreEvent.getEventId());
        assertNull(coreEvent.getInfo());
        assertNull(coreEvent.getSource());
    }

    // ::FIXME:
    public void testCoreEventWithInfoMap() {
        String eventId = "someEvent";

        Map<String, Serializable> info = new HashMap<String, Serializable>();
        info.put("destination", "xxx");

        CoreEvent coreEvent = new CoreEventImpl(eventId, null, info, null, null, null);
        assertEquals(eventId, coreEvent.getEventId());
        Map<String, ?> infoBack = coreEvent.getInfo();
        assertEquals(info, infoBack);
        assertEquals("xxx", infoBack.get("destination"));
        assertNull(coreEvent.getSource());
    }

    public void testDateInitialisationNoData() {
        CoreEvent coreEvent = new CoreEventImpl(null, null, null, null, null, null);
        assertNotNull(coreEvent.getDate());
    }

    // :FIXME:
    public void testCoreEventWithInfo() {
        String eventId = "someEvent";
        Document source = (Document) new Mock(Document.class).proxy();

        Map<String, Serializable> info = new HashMap<String, Serializable>();
        info.put("destination", "xxx");

        CoreEvent coreEvent = new CoreEventImpl(eventId, source, info, null, null, null);
        assertNotNull(coreEvent.getDate());
    }

}
