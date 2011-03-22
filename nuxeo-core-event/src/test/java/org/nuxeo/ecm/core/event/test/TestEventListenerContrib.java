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
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.script.ScriptingPostCommitEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestEventListenerContrib extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    public void testMerge() throws Exception {
        URL url = EventListenerTest.class.getClassLoader().getResource(
        "test-listeners.xml");
        RuntimeContext rc = deployTestContrib("org.nuxeo.ecm.core.event", url);

        EventService service = Framework.getService(EventService.class);
        EventServiceImpl serviceImpl = (EventServiceImpl) service;

        List<EventListenerDescriptor> inLineDescs = serviceImpl.getEventListenerList().getInlineListenersDescriptors();
        assertEquals(1, inLineDescs.size());
        assertEquals(1, serviceImpl.getEventListenerList().getInLineListeners().size());

        // check enable flag
        EventListenerDescriptor desc =  inLineDescs.get(0);
        desc.setEnabled(false);
        serviceImpl.addEventListener(desc);
        assertEquals(0, serviceImpl.getEventListenerList().getInLineListeners().size());

        desc.setEnabled(true);
        serviceImpl.addEventListener(desc);
        assertEquals(1, serviceImpl.getEventListenerList().getInLineListeners().size());

        // test PostCommit
        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        List<EventListenerDescriptor> apcDescs = serviceImpl.getEventListenerList().getAsyncPostCommitListenersDescriptors();
        assertEquals(1, apcDescs.size());
        assertEquals(1, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());

        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners2.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(0, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        assertEquals(1, serviceImpl.getEventListenerList().getSyncPostCommitListeners().size());

        boolean isScriptListener = false;
        PostCommitEventListener listener = serviceImpl.getEventListenerList().getSyncPostCommitListeners().get(0);
        if ( listener instanceof ScriptingPostCommitEventListener) {
            isScriptListener=true;
        }
        assertTrue(isScriptListener);

        url = EventListenerTest.class.getClassLoader().getResource("test-PostCommitListeners3.xml");
        deployTestContrib("org.nuxeo.ecm.core.event", url);
        assertEquals(1, serviceImpl.getEventListenerList().getAsyncPostCommitListeners().size());
        assertEquals(0, serviceImpl.getEventListenerList().getSyncPostCommitListeners().size());

        listener = serviceImpl.getEventListenerList().getAsyncPostCommitListeners().get(0);
        isScriptListener = listener instanceof ScriptingPostCommitEventListener;
        assertFalse(isScriptListener);
    }

}
