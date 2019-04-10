/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingPersister;
import org.nuxeo.ecm.platform.routing.core.impl.DocumentRoutingTreePersister;

/**
 * @author arussel
 */
public class TestDocumentRoutingTreePersister extends DocumentRoutingTestCase {

    protected DocumentRoutingPersister persister;

    @Before
    public void createPersister() throws Exception {
        persister = new DocumentRoutingTreePersister();
    }

    @Test
    public void testGetOrCreateRootOfDocumentRouteInstanceStructure() throws Exception {
        DocumentModel doc = persister.getOrCreateRootOfDocumentRouteInstanceStructure(session);
        assertNotNull(doc);
        assertEquals(doc.getPathAsString(), TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT);
        session.save();

        try (CloseableCoreSession membersSession = CoreInstance.openCoreSession(session.getRepositoryName(), "members")) {
            assertFalse(membersSession.hasPermission(doc.getRef(), SecurityConstants.READ));
        }
    }

    /**
     * Test creation when there's a non-folderish doc at the root.
     */
    @Test
    public void testDocumentRouteInstancesRootCreation() throws Exception {
        // create a document coming before '/default-domain' in name order
        DocumentModel firstDoc = session.createDocumentModel("/", "aaa", "File");
        firstDoc = session.createDocument(firstDoc);
        session.save();
        DocumentModel doc = persister.getOrCreateRootOfDocumentRouteInstanceStructure(session);
        assertNotNull(doc);
        assertEquals(doc.getPathAsString(), TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT);
        session.save();

        try (CloseableCoreSession membersSession = CoreInstance.openCoreSession(session.getRepositoryName(), "members")) {
            assertFalse(membersSession.hasPermission(doc.getRef(), SecurityConstants.READ));
        }
    }

    @Test
    public void testGetParentFolderForDocumentRouteInstance() {
        DocumentModel parent = persister.getParentFolderForDocumentRouteInstance(null, session);
        assertNotNull(parent);
        assertTrue(parent.getPathAsString().startsWith(TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDocumentRouteInstanceFromDocumentRouteModel() {
        DocumentModel model = createDocumentRouteModel(session, DocumentRoutingTestCase.ROUTE1, ROOT_PATH);
        List<String> docsId = new ArrayList<String>();
        docsId.add("1");
        model.setPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME, (Serializable) docsId);
        DocumentModel instance = persister.createDocumentRouteInstanceFromDocumentRouteModel(model, session);
        assertNotNull(instance);
        assertTrue(instance.getPathAsString().startsWith(TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT));
        docsId = (List<String>) instance.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME);
        assertEquals("1", docsId.get(0));

        try (CloseableCoreSession managersSession = CoreInstance.openCoreSession(session.getRepositoryName(),
                DocumentRoutingConstants.ROUTE_MANAGERS_GROUP_NAME)) {
            assertEquals(3, managersSession.getChildren(instance.getRef()).size());
        }
    }

    @Test
    public void testSaveDocumentRouteInstanceAsNewModel() {
        DocumentModel model = createDocumentRouteModel(session, DocumentRoutingTestCase.ROUTE1, ROOT_PATH);
        DocumentModel instance = persister.createDocumentRouteInstanceFromDocumentRouteModel(model, session);
        DocumentModel newModel = persister.saveDocumentRouteInstanceAsNewModel(instance, session.getRootDocument(),
                null, session);
        assertNotNull(newModel);
    }
}
