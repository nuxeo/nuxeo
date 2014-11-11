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
    private static final String COMMENT = "comment";

    private static final String CATEGORY = "category";

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
    public void testCoreEventWithInfoMap() {
        String eventId = "someEvent";

        Map<String, Serializable> info = new HashMap<String, Serializable>();
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
