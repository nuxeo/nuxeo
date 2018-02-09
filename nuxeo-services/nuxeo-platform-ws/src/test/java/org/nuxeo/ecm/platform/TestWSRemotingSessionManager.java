/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     anguenot
 *
 * $Id: TestWSRemotingSessionManager.java 21485 2007-06-27 12:03:43Z sfermigier $
 */

package org.nuxeo.ecm.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.ws")
public class TestWSRemotingSessionManager {

    @Inject
    WSRemotingSessionManager service;

    @Inject
    protected CoreSession session;

    @Test
    public void testGetSessionWithNullSid() {
        boolean raises = false;
        try {
            service.getSession(null);
        } catch (NuxeoException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

    @Test
    public void testGetSessionWithInvalidSid() {
        boolean raises = false;
        try {
            service.getSession("fake");
        } catch (NuxeoException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

    @Test
    public void testCreateSession() {
        WSRemotingSession session = service.createSession("username", "password", "repository", null, null);
        assertEquals("username", session.getUsername());
        assertEquals("password", session.getPassword());
        assertEquals("repository", session.getRepository());
        assertNull(session.getDocumentManager());
        assertNull(session.getUserManager());
    }

    @Test
    public void testAddDelSession() {
        WSRemotingSession session = service.createSession("username", "password", "repository", null, null);
        service.addSession("sid0", session);
        assertNotNull(service.getSession("sid0"));
        service.delSession("sid0");

        boolean raises = false;
        try {
            service.getSession("sid0");
        } catch (NuxeoException ce) {
            raises = true;
        }
        assertTrue(raises);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.tests:login-config.xml")
    public void testSnapshotProperties() {
        DocumentModel doc = session.createDocumentModel("/", "youps", "File");
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
        int tti = Arrays.binarySearch(props, new DocumentProperty("dc:title", null), propsComparator);
        assertTrue(tti > 0);
        assertEquals("dc:title:huum", props[tti].toString());

        // cleanup
        remoting.disconnect(sid);
    }

}
