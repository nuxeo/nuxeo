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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.script.ScriptingPostCommitEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestEventListenerContrib extends NXRuntimeTestCase {

    protected EventServiceImpl serviceImpl;

    @Override
    protected void setUp() throws Exception {
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    protected void postSetUp() throws Exception {
        serviceImpl = (EventServiceImpl) Framework.getService(EventService.class);
    }

    @Test
    public void testMerge() throws Exception {
        int N = serviceImpl.getEventListenerList().getInlineListenersDescriptors().size() + 1;
        pushInlineDeployments("org.nuxeo.ecm.core.event:test-listeners.xml");

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
        pushInlineDeployments("org.nuxeo.ecm.core.event:test-PostCommitListeners.xml");
        List<EventListenerDescriptor> apcDescs = serviceImpl.getEventListenerList()
                                                            .getAsyncPostCommitListenersDescriptors();
        assertEquals(1, apcDescs.size());
        assertEquals(1, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        desc = serviceImpl.getEventListener("testPostCommit");
        assertEquals(0, desc.getPriority());

        pushInlineDeployments("org.nuxeo.ecm.core.event:test-PostCommitListeners2.xml");

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

        pushInlineDeployments("org.nuxeo.ecm.core.event:test-PostCommitListeners3.xml");

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
        pushInlineDeployments("org.nuxeo.ecm.core.event:test-InvalidListeners.xml");

        assertEquals(0, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        List<String> errors = Framework.getRuntime().getMessageHandler().getErrors();
        assertNotNull(errors);
        assertEquals(3, errors.size());

        assertEquals("Failed to register event listener in component 'service:test-invalid-listeners': "
                + "error initializing event listener 'invalidListenerUnknown' (java.lang.RuntimeException: "
                + "java.lang.ClassNotFoundException: org.nuxeo.invalid.listener.UnknownClass)", errors.get(0));
        assertEquals(
                "Failed to register event listener in component 'service:test-invalid-listeners': "
                        + "error initializing event listener 'invalidListenerNotEventListener' "
                        + "(java.lang.IllegalArgumentException: Listener extension must define a class extending "
                        + "EventListener or PostCommitEventListener: 'org.nuxeo.ecm.core.event.test.InvalidEventListener'.)",
                errors.get(1));
        assertEquals("Failed to register event listener in component 'service:test-invalid-listeners': "
                + "error initializing event listener 'invalidListenerNoRef' (java.lang.IllegalArgumentException: "
                + "Listener extension must define either a class or a script)", errors.get(2));
    }

}
