/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id: TestLifeCycleService.java 4265 2006-10-17 00:50:35Z janguenot $
 */

package org.nuxeo.ecm.core.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the repository event listener service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestRepositoryListenerService extends NXRuntimeTestCase {

    private CoreEventListenerService repositoryListenerService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event.compat");
        deployTestContrib("org.nuxeo.ecm.core.event.compat",
                "CoreEventListenerTestExtensions.xml");

        repositoryListenerService = Framework.getLocalService(CoreEventListenerService.class);
    }

    public void testComponentRegistration() {
        assertNotNull(repositoryListenerService);
    }

    public void testListenerRegistration() {
        Collection<EventListener> listeners = repositoryListenerService.getEventListeners();
        assertEquals(4, listeners.size());
    }

    public void testGetListenerByName() {
        EventListener fakeListener = repositoryListenerService.getEventListenerByName("fakelistener");
        assertNotNull(fakeListener);
        assertEquals("fakelistener", fakeListener.getName());
    }

    public void testAddRemoveListeners() {
        EventListener listener = new FakeEventListener();
        listener.setName("newlistener");

        // Register a new one.
        repositoryListenerService.addEventListener(listener);
        Collection<EventListener> listeners = repositoryListenerService.getEventListeners();
        assertEquals(5, listeners.size());

        // Unregister
        repositoryListenerService.removeEventListener(listener);
        listeners = repositoryListenerService.getEventListeners();
        assertEquals(4, listeners.size());
    }

    public void testNotifyListeners() {
        List<String> hits = new ArrayList<String>();

        Map<String, Object> info = new HashMap<String, Object>();
        info.put("hits", hits);

        CoreEventImpl coreEvent = new CoreEventImpl(null, null, info, null,
                null, null);

        // fake listener is configured to add info in the event hits info when
        // processing it => this is a way to test what listeners have been
        // notified and in what order.
        repositoryListenerService.notifyEventListeners(coreEvent);
        List<String> expectedHits = Arrays.asList("fakelistener",
                "first-listener", "second-listener");
        assertEquals(expectedHits, hits);

        // test that party listener is notified when good event id is set
        hits.clear();

        coreEvent = new CoreEventImpl("party", null, info, null, null, null);
        repositoryListenerService.notifyEventListeners(coreEvent);
        expectedHits = Arrays.asList("fakelistener", "party-listener",
                "first-listener", "second-listener");
        assertEquals(expectedHits, hits);
    }

}
