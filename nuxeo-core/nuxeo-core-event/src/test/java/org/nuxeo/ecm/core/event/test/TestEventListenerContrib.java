/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.script.ScriptingPostCommitEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestEventListenerContrib extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        fireFrameworkStarted();
    }

    @Test
    public void testMerge() throws Exception {
        EventService service = Framework.getService(EventService.class);
        EventServiceImpl serviceImpl = (EventServiceImpl) service;
        int N = serviceImpl.getEventListenerList().getInlineListenersDescriptors().size() + 1;
        URL url = EventListenerTest.class.getClassLoader().getResource("test-listeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);

        List<EventListenerDescriptor> inLineDescs = serviceImpl.getEventListenerList().getInlineListenersDescriptors();
        assertEquals(N, inLineDescs.size());
        assertEquals(N, serviceImpl.getEventListenerList().getInLineListeners().size());

        // check enable flag
        EventListenerDescriptor desc = inLineDescs.get(0);
        desc.setEnabled(false);
        serviceImpl.addEventListener(desc);
        assertEquals(N - 1, serviceImpl.getEventListenerList().getInLineListeners().size());

        desc.setEnabled(true);
        serviceImpl.addEventListener(desc);
        assertEquals(N, serviceImpl.getEventListenerList().getInLineListeners().size());

        // test PostCommit
        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        List<EventListenerDescriptor> apcDescs = serviceImpl.getEventListenerList()
                                                            .getAsyncPostCommitListenersDescriptors();
        assertEquals(1, apcDescs.size());
        assertEquals(1, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        desc = serviceImpl.getEventListener("testPostCommit");
        assertEquals(0, desc.getPriority());

        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners2.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(0, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        assertEquals(1, serviceImpl.getEventListenerList().getSyncPostCommitListeners().size());

        boolean isScriptListener = false;
        PostCommitEventListener listener = serviceImpl.getEventListenerList().getSyncPostCommitListeners().get(0);
        if (listener instanceof ScriptingPostCommitEventListener) {
            isScriptListener = true;
        }
        assertTrue(isScriptListener);
        desc = serviceImpl.getEventListener("testPostCommit");
        assertEquals(10, desc.getPriority());

        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners3.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(1, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        assertEquals(0, serviceImpl.getEventListenerList().getSyncPostCommitListeners().size());

        listener = serviceImpl.getEventListenerList().getAsyncPostCommitListeners().get(0);
        isScriptListener = listener instanceof ScriptingPostCommitEventListener;
        assertFalse(isScriptListener);
        desc = serviceImpl.getEventListener("testPostCommit");
        assertEquals(20, desc.getPriority());
    }

    @Test
    public void testInvalidListeners() throws Exception {
        EventService service = Framework.getService(EventService.class);
        EventServiceImpl serviceImpl = (EventServiceImpl) service;
        assertEquals(0, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        URL url = EventListenerTest.class.getClassLoader().getResource("test-InvalidListeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(0, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        List<String> warns = Framework.getRuntime().getWarnings();
        assertNotNull(warns);
        assertEquals(3, warns.size());

        assertEquals("Failed to register event listener in component 'service:test-invalid-listeners': "
                + "error initializing event listener 'invalidListenerUnknown' (java.lang.RuntimeException: "
                + "java.lang.ClassNotFoundException: org.nuxeo.invalid.listener.UnknownClass)", warns.get(0));
        assertEquals(
                "Failed to register event listener in component 'service:test-invalid-listeners': "
                        + "error initializing event listener 'invalidListenerNotEventListener' "
                        + "(java.lang.IllegalArgumentException: Listener extension must define a class extending "
                        + "EventListener or PostCommitEventListener: 'org.nuxeo.ecm.core.event.test.InvalidEventListener'.)",
                warns.get(1));
        assertEquals("Failed to register event listener in component 'service:test-invalid-listeners': "
                + "error initializing event listener 'invalidListenerNoRef' (java.lang.IllegalArgumentException: "
                + "Listener extension must define either a class or a script)", warns.get(2));
    }

}
