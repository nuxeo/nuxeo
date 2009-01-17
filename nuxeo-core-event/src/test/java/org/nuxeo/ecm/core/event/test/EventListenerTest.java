/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import java.net.URL;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * TODO add tests on post commit.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class EventListenerTest extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    public void testFlags() {
        EventImpl event = new EventImpl("test", null);
        assertTrue(event.isPublic());
        assertEquals(0, event.getFlags());
        event.setFlags(Event.LOCAL | Event.INLINE);
        assertEquals(Event.LOCAL | Event.INLINE, event.getFlags());
        assertTrue(event.isInline());
        assertTrue(event.isLocal());
        event.clearFlags(Event.LOCAL);
        assertEquals(Event.INLINE, event.getFlags());
        assertTrue(event.isInline());
        assertFalse(event.isLocal());

        event.clearFlags(~0); // clear all flags
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());

        event.setPublic(false);
        assertFalse(event.isPublic());
        assertTrue(event.isLocal());
        assertFalse(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setPublic(true);
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());
        assertFalse(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setInline(true);
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());
        assertTrue(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setIsCommitEvent(true);
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());
        assertTrue(event.isInline());
        assertTrue(event.isCommitEvent());

        event.setIsCommitEvent(false);
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());
        assertTrue(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setInline(false);
        assertTrue(event.isPublic());
        assertFalse(event.isLocal());
        assertFalse(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setPublic(false);
        assertFalse(event.isPublic());
        assertTrue(event.isLocal());
        assertFalse(event.isInline());
        assertFalse(event.isCommitEvent());
    }

    public void testEventCreation() {
        EventContextImpl ctx = new EventContextImpl();
        Event event = ctx.event("test");
        assertEquals("test", event.getName());
        assertEquals(ctx, event.getContext());
        assertEquals(0, event.getFlags());

        event = ctx.event("test2", Event.COMMIT | Event.INLINE);
        assertEquals("test2", event.getName());
        assertEquals(ctx, event.getContext());
        assertEquals(Event.COMMIT | Event.INLINE, event.getFlags());
    }

    public void testTimestamp() {
        long tm = System.currentTimeMillis();
        EventContextImpl ctx = new EventContextImpl();
        Event event = ctx.event("test");
        assertTrue(tm <= event.getTime());
    }

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;
    public void testScripts() throws Exception {
        URL url = EventListenerTest.class.getClassLoader().getResource("test-listeners.xml");
        RuntimeContext rc = deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(0, SCRIPT_CNT);
        EventService service = Framework.getService(EventService.class);
        service.fireEvent("test", new EventContextImpl(null, null));
        assertEquals(1, SCRIPT_CNT);
        rc.undeploy(url);
        assertEquals(1, SCRIPT_CNT);
        service.fireEvent("test", new EventContextImpl(null, null));
        assertEquals(1, SCRIPT_CNT);
        rc = deployTestContrib("org.nuxeo.ecm.core.event", url);
        service.fireEvent("test1", new EventContextImpl(null, null));
        assertEquals(2, SCRIPT_CNT);
        // test not accepted event
        service.fireEvent("some-event", new EventContextImpl(null, null));
        assertEquals(2, SCRIPT_CNT);
    }

}
