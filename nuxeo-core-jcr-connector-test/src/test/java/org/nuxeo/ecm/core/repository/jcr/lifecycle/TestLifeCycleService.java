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
 * $Id: TestLifeCycleService.java 19318 2007-05-24 18:48:39Z fguillaume $
 */

package org.nuxeo.ecm.core.repository.jcr.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleManager;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Tests the lifecycle service along with the JCRLifeCycleManager registered.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestLifeCycleService extends RepositoryTestCase {

    Session session;
    Document root;

    private LifeCycleService lifeCycleService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "LifeCycleService.xml");
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "LifeCycleManagerTestExtensions.xml");
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "CoreEventListenerService.xml");
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        lifeCycleService = NXCore.getLifeCycleService();
    }

    @Override
    protected void tearDown() throws Exception {
        lifeCycleService = null;
        session.cancel();
        super.tearDown();
    }

    public void testLifeCycleManagerRegistration() {
        Collection<LifeCycleManager> managers = lifeCycleService.getLifeCycleManagers();
        assertEquals(1, managers.size());

        LifeCycleManager manager = lifeCycleService.getLifeCycleManagerByName("jcrlifecyclemanager");
        assertEquals("jcrlifecyclemanager", manager.getName());
    }

    public void testGetLifeCycleManagerFor() throws DocumentException {
        Document doc = root.addChild("docA", "File");
        LifeCycleManager manager = lifeCycleService.getLifeCycleManagerFor(doc);
        assertNotNull(manager);
    }

    public void testInitialize() throws LifeCycleException, DocumentException {
        Document doc = root.addChild("docB", "File");
        root.save();
        lifeCycleService.initialize(doc);
        doc.save();
        assertEquals("work", lifeCycleService.getCurrentLifeCycleState(doc));
    }

    public void testChain() throws LifeCycleException, DocumentException {
        Document doc = root.addChild("docC", "File");
        root.save();
        lifeCycleService.initialize(doc);
        doc.save();
        assertEquals("work", lifeCycleService.getCurrentLifeCycleState(doc));

        lifeCycleService.followTransition(doc, "approve");
        doc.save();
        assertEquals("approved", lifeCycleService.getCurrentLifeCycleState(doc));

        lifeCycleService.followTransition(doc, "obsolete");
        doc.save();
        assertEquals("obsolete", lifeCycleService.getCurrentLifeCycleState(doc));

        boolean checked = false;
        try {
            lifeCycleService.followTransition(doc, "fake");
        } catch (LifeCycleException lce) {
            checked = true;
        }
        assertTrue(checked);
        doc.save();
        assertEquals("obsolete", lifeCycleService.getCurrentLifeCycleState(doc));

        // API document
        assertEquals(doc.getCurrentLifeCycleState(),
                lifeCycleService.getCurrentLifeCycleState(doc));
    }

    public void testDocumentAPI() throws DocumentException, LifeCycleException {
        Document doc = root.addChild("docD", "File");
        root.save();

        lifeCycleService.initialize(doc);

        assertEquals("default", doc.getLifeCyclePolicy());

        assertEquals("work", doc.getCurrentLifeCycleState());
        doc.save();
        doc.followTransition("approve");
        doc.save();
        assertEquals("approved", doc.getCurrentLifeCycleState());
        doc.followTransition("obsolete");
        doc.save();
        assertEquals("obsolete", doc.getCurrentLifeCycleState());
        assertEquals("default", doc.getLifeCyclePolicy());
    }

    public void testQuery() throws Exception {
        Document q1 = root.addChild("docQ1", "File");
        Document q2 = root.addChild("docQ2", "File");
        Document q3 = root.addChild("docQ3", "File");

        lifeCycleService.initialize(q1);
        lifeCycleService.initialize(q2);
        lifeCycleService.initialize(q3);

        // save session so that next queries works
        session.save();

        Query query = session.createQuery(
                "SELECT * FROM Document WHERE ecm:currentLifecycleState='work'",
                Query.Type.NXQL);
        QueryResult qr = query.execute();
        DocumentModelList dml =  qr.getDocumentModels();
        assertEquals(3, dml.size());
        List<String> docs = new ArrayList<String>();
        for (DocumentModel dm : dml) {
            docs.add(dm.getName());
        }
        assertTrue(docs.contains("docQ1"));
        assertTrue(docs.contains("docQ2"));
        assertTrue(docs.contains("docQ3"));

        q1.followTransition("approve");
        session.save();

        query = session.createQuery(
                "SELECT * FROM Document WHERE ecm:currentLifecycleState='work'",
                Query.Type.NXQL);
        qr = query.execute();
        dml = qr.getDocumentModels();
        assertEquals(2, dml.size());
        docs = new ArrayList<String>();
        for (DocumentModel dm : dml) {
            docs.add(dm.getName());
        }
        assertTrue(docs.contains("docQ2"));
        assertTrue(docs.contains("docQ3"));

        query = session.createQuery(
                "SELECT * FROM Document WHERE ecm:currentLifecycleState='approved'",
                Query.Type.NXQL);
        qr = query.execute();
        dml = qr.getDocumentModels();
        assertEquals(1, dml.size());
        assertEquals("docQ1", dml.get(0).getName());

        query = session.createQuery(
                "SELECT * FROM Document WHERE ecm:currentLifecycleState='obsolete'",
                Query.Type.NXQL);
        qr = query.execute();
        dml = qr.getDocumentModels();
        assertEquals(0, dml.size());
    }

}
