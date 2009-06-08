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
 *     anguenot
 *
 * $Id: TestWSRemotingSessionManager.java 21485 2007-06-27 12:03:43Z sfermigier $
 */

package org.nuxeo.ecm.platform;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionServiceDelegate;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestWSRemotingSessionManager extends NXRuntimeTestCase {

    WSRemotingSessionManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.tests",
                "test-nxws_remoting_session_manager-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.tests",
                "test-nxws_remoting_session_manager-platform-contrib.xml");

        service = WSRemotingSessionServiceDelegate.getRemoteWSRemotingSessionManager();
        assertNotNull(service);
    }

    public void testGetSessionWithNullSid() {
        boolean raises = false;
        try {
            service.getSession(null);
        } catch (ClientException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

    public void testGetSessionWithInvalidSid() {
        boolean raises = false;
        try {
            service.getSession("fake");
        } catch (ClientException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

    public void testCreateSession() {
        WSRemotingSession session = service.createSession("username",
                "password", "repository", null, null);
        assertEquals("username", session.getUsername());
        assertEquals("password", session.getPassword());
        assertEquals("repository", session.getRepository());
        assertNull(session.getDocumentManager());
        assertNull(session.getUserManager());
    }

    public void testAddDelSession() throws ClientException {
        WSRemotingSession session = service.createSession("username",
                "password", "repository", null, null);
        service.addSession("sid0", session);
        assertNotNull(service.getSession("sid0"));
        service.delSession("sid0");

        boolean raises = false;
        try {
            service.getSession("sid0");
        } catch (ClientException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

}
