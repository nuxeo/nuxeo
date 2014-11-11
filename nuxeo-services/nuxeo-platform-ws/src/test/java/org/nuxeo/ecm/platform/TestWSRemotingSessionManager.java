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

import java.util.Arrays;
import java.util.Comparator;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestWSRemotingSessionManager extends SQLRepositoryTestCase {

    WSRemotingSessionManager service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        service = Framework.getService(WSRemotingSessionManager.class);
        assertNotNull(service);
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployBundle("org.nuxeo.ecm.platform.ws");
        deployContrib("org.nuxeo.ecm.platform.tests",
                "login-config.xml");
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

    public void testSnapshotProperties() throws ClientException {
        openSession();
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel doc = session.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        doc.setProperty("dublincore", "title", "huum");
        doc = session.createDocument(doc);
        session.save();
        String docid = doc.getId();
        NuxeoRemotingBean remoting = new NuxeoRemotingBean();
        String sid = remoting.connect("Administrator", "Administrator");
        DocumentSnapshot snapshot = remoting.getDocumentSnapshot(sid, docid);
        DocumentProperty[] props = snapshot.getNoBlobProperties();
        Comparator<DocumentProperty> propsComparator = new Comparator<DocumentProperty>() {

            @Override
            public int compare(DocumentProperty o1, DocumentProperty o2) {
                return o1.getName().compareTo(o2.getName());
            }

        };
        Arrays.sort(props, propsComparator);
        // check for system properties
        int lci = Arrays.binarySearch(props, new DocumentProperty("lifeCycleState", null), propsComparator);
        assertTrue(lci > 0);
        assertEquals("lifeCycleState:project", props[lci].toString());

        // check for dublin core properties
        int tti =  Arrays.binarySearch(props, new DocumentProperty("dc:title", null), propsComparator);
        assertTrue(tti > 0);
        assertEquals("dc:title:huum", props[tti].toString());

        // cleanup
        remoting.disconnect(sid);
        closeSession();
    }

}
