/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thomas Roger
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.event.test;

import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestEventServiceComponent extends NXRuntimeTestCase {

    protected int initialThreadCount;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        initialThreadCount = Thread.activeCount();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    public void testDisablingListener() throws Exception {
        URL url = EventListenerTest.class.getClassLoader().getResource(
                "test-disabling-listeners1.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        EventService service = Framework.getService(EventService.class);
        EventServiceImpl serviceImpl = (EventServiceImpl) service;

        List<EventListenerDescriptor> eventListenerDescriptors = serviceImpl.getEventListenerList().getSyncPostCommitListenersDescriptors();
        assertEquals(1, eventListenerDescriptors.size());

        EventListenerDescriptor eventListenerDescriptor = eventListenerDescriptors.get(0);
        assertTrue(eventListenerDescriptor.isEnabled());

        url = EventListenerTest.class.getClassLoader().getResource(
                "test-disabling-listeners2.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);

        eventListenerDescriptors = serviceImpl.getEventListenerList().getSyncPostCommitListenersDescriptors();
        assertEquals(1, eventListenerDescriptors.size());

        eventListenerDescriptor = eventListenerDescriptors.get(0);
        assertFalse(eventListenerDescriptor.isEnabled());
    }

    /**
     * Test that when the event service component is deactivated, the threads of
     * the async event executor are shut down.
     */
    public void testAsyncEventExecutorShutdown() throws Exception {
        // send an async event to make sure the async event executor spawned
        // some threads
        // load contrib
        URL url = getClass().getClassLoader().getResource(
                "test-PostCommitListeners3.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        // send event
        EventService service = Framework.getService(EventService.class);
        Event test1 = new EventImpl("test1", new EventContextImpl());
        test1.setIsCommitEvent(true);
        service.fireEvent(test1);
        // wait for async processing to be done
        service.waitForAsyncCompletion(2 * 1000);
        assertEquals("test1 not handled",
                DummyPostCommitEventListener.handledCount, 1);
        // can still fire events
        Event test2 = new EventImpl("test2", new EventContextImpl());
        test2.setIsCommitEvent(true);
        service.fireEvent(test2);
        // now stop service
        // this is called by EventServiceComponent.deactivate() in real life
        ((EventServiceImpl) service).shutdown(2 * 1000);
        assertEquals("test2 not handled",
                DummyPostCommitEventListener.handledCount, 2);
        Thread.sleep(2 * 1000);
        assertEquals("thread not death", initialThreadCount,
                Thread.activeCount());
    }

}
