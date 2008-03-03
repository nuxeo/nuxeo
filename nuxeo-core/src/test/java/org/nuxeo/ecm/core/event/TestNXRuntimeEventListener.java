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
 *
 * $Id: TestNXRuntimeEventListener.java 28594 2008-01-09 12:42:18Z sfermigier $
 */

package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the NXRuntimeEventListener.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXRuntimeEventListener extends NXRuntimeTestCase {

    private static final String LISTENER_NAME = "nxruntimelistener";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("RepositoryService.xml");
        deployContrib("CoreEventListenerService.xml");
    }

    public void testNXListenerRegistration() {
        CoreEventListenerService listenerService = NXCore.getCoreEventListenerService();
        EventListener nxListener = listenerService.getEventListenerByName(
                LISTENER_NAME);
        assertNotNull(nxListener);
    }

}
