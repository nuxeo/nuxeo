/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.impl.PostCommitEventExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestEventServiceComponent extends NXRuntimeTestCase {

    protected int initialThreadCount;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        fireFrameworkStarted();
        // 2 quartz threads launched by the event contribs above
        Thread.sleep(100);
        initialThreadCount = Thread.activeCount();
        DummyPostCommitEventListener.handledCountReset();
        DummyPostCommitEventListener.eventCountReset();
        NuxeoContainer.install();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Event commit = new EventImpl("commit", new EventContextImpl());
        commit.setIsCommitEvent(true);
        EventService service = Framework.getService(EventService.class);
        service.fireEvent(commit);
        service.waitForAsyncCompletion();
        NuxeoContainer.uninstall();
        super.tearDown();
    }

    @Test
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

    @Test
    public void testAsync() throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "test-async-listeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        EventService service = Framework.getService(EventService.class);

        // send two events, only one of which is recognized by the listener
        // (the other is filtered out of the bundle passed to this listener)
        Event test1 = new EventImpl("testasync", new EventContextImpl());
        service.fireEvent(test1);
        assertEquals(DummyPostCommitEventListener.handledCount(), 0);
        Event test2 = new EventImpl("testnotmached", new EventContextImpl());
        test2.setIsCommitEvent(true);
        service.fireEvent(test2);
        service.waitForAsyncCompletion();
        Thread.sleep(100); // TODO async completion has race conditions
        assertEquals(DummyPostCommitEventListener.handledCount(), 1);
        assertEquals(1, DummyPostCommitEventListener.eventCount());

        // check new information from sync event are retrieved in postcommit
        // listener
        assertEquals("bar", DummyPostCommitEventListener.properties.get("foo"));
    }

    @Test
    public void testAsyncRetry() throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "test-async-listeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        EventService service = Framework.getLocalService(EventService.class);

        // send two events, only one of which is recognized by the listener
        // (the other is filtered out of the bundle passed to this listener)
        EventContextImpl context = new EventContextImpl();
        context.setProperty("concurrentexception", Boolean.TRUE);
        Event test1 = new EventImpl("testasync", context);
        test1.setIsCommitEvent(true);
        service.fireEvent(test1);
        service.waitForAsyncCompletion();
        assertEquals(2, DummyPostCommitEventListener.handledCount());
    }

    @Test
    public void testSyncPostCommit() throws Exception {
        doTestSyncPostCommit(false, false, false, 2, 4);
    }

    @Test
    public void testSyncPostCommitError() throws Exception {
        // second handler done even though there's an error
        doTestSyncPostCommit(false, true, false, 2, 4);
    }

    @Test
    public void testSyncPostCommitTimeout() throws Exception {
        // returned after timeout (300ms), so only one listener done at this time
        doTestSyncPostCommit(false, false, true, 1, 2);
        Thread.sleep(3000); // wait other listener
        assertEquals(2, DummyPostCommitEventListener.handledCount());
        assertEquals(4, DummyPostCommitEventListener.eventCount());
    }

    @Test
    public void testSyncPostCommitBulk() throws Exception {
        doTestSyncPostCommit(true, false, false, 2, 4);
    }

    @Test
    public void testSyncPostCommitErrorBulk() throws Exception {
        // second handler not done because error in first one (bulk)
        doTestSyncPostCommit(true, true, false, 1, 2);
    }

    @Test
    public void testSyncPostCommitTimeoutBulk() throws Exception {
        // returned after timeout (300ms), so only one listener done
        doTestSyncPostCommit(true, false, true, 1, 2);
        Thread.sleep(3000); // wait other listener
        // other listener was never run due to interrupt
        assertEquals(1, DummyPostCommitEventListener.handledCount());
        assertEquals(2, DummyPostCommitEventListener.eventCount());
    }

    protected void doTestSyncPostCommit(boolean bulk, boolean error,
            boolean timeout, int expectedHandled, int expectedEvents)
            throws Exception {
        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        try {
            eventServiceAdmin.setBulkModeEnabled(bulk);
            if (timeout) {
                Framework.getProperties().setProperty(
                        PostCommitEventExecutor.BULK_TIMEOUT_PROP, "1"); // 1s
            }
            doTestSyncPostCommit(error, timeout, expectedHandled,
                    expectedEvents);
        } finally {
            eventServiceAdmin.setBulkModeEnabled(false);
            Framework.getProperties().remove(
                    PostCommitEventExecutor.BULK_TIMEOUT_PROP);
        }
    }

    protected void doTestSyncPostCommit(boolean error, boolean timeout,
            int expectedHandled, int expectedEvents) throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "test-sync-postcommit-listeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        EventService service = Framework.getService(EventService.class);

        // Send 4 events, only 2 of which are recognized by the listeners
        // (the last one is filtered out of the bundle passed to the listeners
        // and also servers as a trigger for the event bundle execution).
        Event event1 = new EventImpl("testnotmached", new EventContextImpl());
        service.fireEvent(event1);
        EventContextImpl context = new EventContextImpl();
        if (error) {
            // Provoke an error in the first listener, to check whether the
            // second one is done or not (depending on bulk mode)
            context.setProperty("throw", Boolean.TRUE);
        }
        if (timeout) {
            // Provoke a sleep that makes the work time out
            context.setProperty("sleep", Boolean.TRUE);
        }
        Event event2 = new EventImpl("testsyncpostcommit", context);
        service.fireEvent(event2);
        Event event3 = new EventImpl("testsyncpostcommit",
                new EventContextImpl());
        service.fireEvent(event3);
        Event event4 = new EventImpl("testnotmached", new EventContextImpl());
        event4.setIsCommitEvent(true);
        assertEquals(0, DummyPostCommitEventListener.handledCount());
        service.fireEvent(event4);
        service.waitForAsyncCompletion();
        // Thread.sleep(3000);
        assertEquals(expectedHandled,
                DummyPostCommitEventListener.handledCount());
        assertEquals(expectedEvents, DummyPostCommitEventListener.eventCount());
        if (timeout) {
            // wait for listeners to finish
            Thread.sleep(2000);
        }
    }

    /**
     * Test that when the event service component is deactivated, the threads of
     * the async event executor are shut down.
     */
    @Test
    @Ignore
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
        assertEquals(1, DummyPostCommitEventListener.handledCount());
        // can still fire events
        Event test2 = new EventImpl("test1", new EventContextImpl());
        test2.setIsCommitEvent(true);
        service.fireEvent(test2);
        // now stop service
        // this is called by EventServiceComponent.deactivate() in real life
        ((EventServiceImpl) service).shutdown(2 * 1000);
        ((EventServiceImpl) service).init();
        assertEquals(2, DummyPostCommitEventListener.handledCount());
        Thread.sleep(2 * 1000);
        assertEquals("Threads not dead", 0, Thread.activeCount()
                - initialThreadCount);
    }

}
