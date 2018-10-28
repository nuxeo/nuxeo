/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.dgc.VMID;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * TODO add tests on post commit.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.event")
public class EventListenerTest {

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testFlags() {
        EventImpl event = new EventImpl("test", null);
        assertEquals(Event.FLAG_NONE, event.getFlags());

        event.setInline(true);
        assertEquals(Event.FLAG_INLINE, event.getFlags());
        assertTrue(event.isInline());

        event.setInline(false);
        assertEquals(Event.FLAG_NONE, event.getFlags());
        assertFalse(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setInline(true);
        assertTrue(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setIsCommitEvent(true);
        assertTrue(event.isInline());
        assertTrue(event.isCommitEvent());

        event.setIsCommitEvent(false);
        assertTrue(event.isInline());
        assertFalse(event.isCommitEvent());

        event.setInline(false);
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

    protected EventService getService() {
        return Framework.getService(EventService.class);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event:test-listeners.xml")
    @ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreIsolated.class)
    public void testScripts() throws Exception {

        assertEquals(0, SCRIPT_CNT);
        getService().fireEvent("test", new EventContextImpl(null, null));
        assertEquals(1, SCRIPT_CNT);

        hotDeployer.undeploy("org.nuxeo.ecm.core.event:test-listeners.xml");

        assertEquals(1, SCRIPT_CNT);
        getService().fireEvent("test", new EventContextImpl(null, null));
        assertEquals(1, SCRIPT_CNT);

        hotDeployer.deploy("org.nuxeo.ecm.core.event:test-listeners.xml");

        getService().fireEvent("test1", new EventContextImpl(null, null));
        assertEquals(2, SCRIPT_CNT);
        // test not accepted event
        getService().fireEvent("some-event", new EventContextImpl(null, null));
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
            if (cnt++ > 10) {
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
