/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.*;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.model.Document;

/**
 * CoreSession event test case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(JMock.class)
public class TestCoreEvent {
    private static final String COMMENT = "comment";

    private static final String CATEGORY = "category";

    protected Mockery jmcontext = new JUnit4Mockery();

    @Test
    public void testCoreEventVarious() {
        String eventId = "someEvent";
        String source = "someDummySource";

        CoreEvent coreEvent = new CoreEventImpl(null, null, null, null, null, null);
        assertNotNull(coreEvent);
        assertNull(coreEvent.getEventId());
        assertNull(coreEvent.getSource());
        assertNotNull(coreEvent.getInfo());
        assertNull(coreEvent.getComment());
        assertNull(coreEvent.getCategory());

        CoreEvent coreEvent1 = new CoreEventImpl(eventId, null, null, null, null, null);
        assertNotNull(coreEvent1);
        assertEquals(eventId, coreEvent1.getEventId());
        assertNull(coreEvent1.getSource());
        assertNotNull(coreEvent1.getInfo());
        assertNull(coreEvent1.getComment());
        assertNull(coreEvent1.getCategory());

        CoreEvent coreEvent2 = new CoreEventImpl(eventId, source, null, null, null, null);
        assertNotNull(coreEvent2);
        assertEquals(eventId, coreEvent2.getEventId());
        assertEquals(source, coreEvent2.getSource());
        assertNotNull(coreEvent2.getInfo());
        assertNull(coreEvent2.getComment());
        assertNull(coreEvent2.getCategory());

        Map<String, Serializable> info = null;
        CoreEvent coreEvent3 = new CoreEventImpl(eventId, source, info, null, null, null);
        assertNotNull(coreEvent3);
        assertEquals(eventId, coreEvent3.getEventId());
        assertEquals(source, coreEvent3.getSource());
        assertNotNull(coreEvent3.getInfo());
        assertNull(coreEvent3.getComment());
        assertNull(coreEvent3.getCategory());
    }

    // ::FIXME:
    @Test
    public void testCoreEventWithMockDocument() {
        String eventId = "someEvent";
        Map<String, Serializable> info = null;

        CoreEvent coreEvent = new CoreEventImpl(eventId, null, info, null, null, null);
        assertEquals(eventId, coreEvent.getEventId());
        assertNotNull(coreEvent.getInfo());
        assertNull(coreEvent.getComment());
        assertNull(coreEvent.getCategory());
        assertNull(coreEvent.getSource());
    }

    // ::FIXME:
    @Test
    public void testCoreEventWithInfoMap() {
        String eventId = "someEvent";

        Map<String, Serializable> info = new HashMap<>();
        info.put("destination", "xxx");

        CoreEvent coreEvent = new CoreEventImpl(eventId, null, info, null, null, null);
        assertEquals(eventId, coreEvent.getEventId());
        Map<String, ?> infoBack = coreEvent.getInfo();
        info.put(COMMENT, null);
        info.put(CATEGORY, null);
        assertEquals(info, infoBack);
        assertEquals("xxx", infoBack.get("destination"));
        assertNull(coreEvent.getSource());
    }

    @Test
    public void testDateInitialisationNoData() {
        CoreEvent coreEvent = new CoreEventImpl(null, null, null, null, null, null);
        assertNotNull(coreEvent.getDate());
    }

    // :FIXME:
    @Test
    public void testCoreEventWithInfo() {
        String eventId = "someEvent";
        Document source = jmcontext.mock(Document.class);

        Map<String, Serializable> info = new HashMap<>();
        info.put("destination", "xxx");

        CoreEvent coreEvent = new CoreEventImpl(eventId, source, info, null, null, null);
        assertNotNull(coreEvent.getDate());
    }

}
