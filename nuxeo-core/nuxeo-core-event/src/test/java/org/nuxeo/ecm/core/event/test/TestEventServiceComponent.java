/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import java.net.URL;
import java.util.List;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:throger@gmail.com">Thomas Roger</a>
 */
public class TestEventServiceComponent extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
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

}
