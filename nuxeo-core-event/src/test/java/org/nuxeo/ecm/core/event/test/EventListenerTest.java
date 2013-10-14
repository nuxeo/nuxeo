/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.rmi.dgc.VMID;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
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
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Test
    public void testFlags() {
        EventImpl event = new EventImpl("test", null);
        assertTrue(event.isPublic());
        assertEquals(Event.FLAG_NONE, event.getFlags());

        event.setLocal(true);
        event.setInline(true);
        assertEquals(Event.FLAG_LOCAL | Event.FLAG_INLINE, event.getFlags());
        assertTrue(event.isInline());
        assertTrue(event.isLocal());

        event.setLocal(false);
        assertEquals(Event.FLAG_INLINE, event.getFlags());
        assertTrue(event.isInline());
        assertFalse(event.isLocal());

        event.setInline(false);
        assertEquals(Event.FLAG_NONE, event.getFlags());
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

    @Test
    public void testEventCreation() {
        EventContextImpl ctx = new EventContextImpl();
        Event event = ctx.newEvent("test");
        assertEquals("test", event.getName());
        assertEquals(ctx, event.getContext());
        assertEquals(Event.FLAG_NONE, event.getFlags());

        event = ctx.newEvent("test2", Event.FLAG_COMMIT | Event.FLAG_INLINE);
        assertEquals("test2", event.getName());
        assertEquals(ctx, event.getContext());
        assertEquals(Event.FLAG_COMMIT | Event.FLAG_INLINE, event.getFlags());
    }

    @Test
    public void testTimestamp() {
        long tm = System.currentTimeMillis();
        EventContextImpl ctx = new EventContextImpl();
        Event event = ctx.newEvent("test");
        assertTrue(tm <= event.getTime());
    }

    /**
     * The script listener will update this counter
     */
    public static int SCRIPT_CNT = 0;

    @Test
    public void testScripts() throws Exception {
        URL url = EventListenerTest.class.getClassLoader().getResource(
                "test-listeners.xml");
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

    @Test
    public void testRemoteForwarding() throws Exception {
        VMID vmid1 = EventServiceImpl.VMID; // the source vmid
        // generate another different vmid that will be used as the target host VMID
        VMID vmid2 = new VMID();
        int cnt = 0;
        while (vmid2.equals(vmid1)) {
            Thread.sleep(1000);
            vmid2 = new VMID();
            if (cnt++ >10) {
                fail("Unable to complete test - unable to generate a target vmid");
            }
        }

        FakeEventBundle event = new FakeEventBundle();
        assertFalse(event.hasRemoteSource());

        // change the vmid of the event as it was created on another machine
        event.setVMID(vmid2);
        assertTrue(event.hasRemoteSource());

        // serialize the event as it was sent from vmid2 to vmid1
        event = (FakeEventBundle) serialize(event);
        // now test the event - it should be marked as remote
        assertTrue(event.hasRemoteSource());
        // redo the test but with a non remote event

        event = new FakeEventBundle();
        assertFalse(event.hasRemoteSource());

        event = (FakeEventBundle) serialize(event);
        // now test the event - it should be marked as local
        assertFalse(event.hasRemoteSource());
    }

    public static Object serialize(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(obj);
        out.flush();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        return in.readObject();
    }

}
