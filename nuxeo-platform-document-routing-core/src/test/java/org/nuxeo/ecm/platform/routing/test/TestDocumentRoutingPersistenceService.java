/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * @author arussel
 *
 */
public class TestDocumentRoutingPersistenceService extends
        DocumentRoutingTestCase {

    public void testGetOrCreateRootOfDocumentRouteInstanceStructure() {
        DocumentModel doc = persistenceService.getOrCreateRootOfDocumentRouteInstanceStructure(session);
        assertNotNull(doc);
        assertEquals(doc.getPathAsString(),
                TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT);
    }

    public void testGetParentFolderForDocumentRouteInstance() {
        DocumentModel parent = persistenceService.getParentFolderForDocumentRouteInstance(
                null, session);
        assertNotNull(parent);
        assertTrue(parent.getPathAsString().startsWith(
                TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT));
    }

    @SuppressWarnings("unchecked")
    public void testCreateDocumentRouteInstanceFromDocumentRouteModel()
            throws ClientException {
        DocumentModel model = createDocumentRouteModel(session,
                DocumentRoutingTestCase.ROUTE1, ROOT_PATH);
        List<String> docsId = new ArrayList<String>();
        docsId.add("1");
        model.setPropertyValue(
                DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME,
                (Serializable) docsId);
        DocumentModel instance = persistenceService.createDocumentRouteInstanceFromDocumentRouteModel(
                model, session);
        assertNotNull(instance);
        assertTrue(instance.getPathAsString().startsWith(
                TestConstants.DEFAULT_DOMAIN_DOCUMENT_ROUTE_INSTANCES_ROOT));
        docsId = (List<String>) instance.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME);
        assertEquals("1", docsId.get(0));
        assertEquals(2, session.getChildren(instance.getRef()).size());
    }

    public void testSaveDocumentRouteInstanceAsNewModel()
            throws ClientException {
        DocumentModel model = createDocumentRouteModel(session,
                DocumentRoutingTestCase.ROUTE1, ROOT_PATH);
        DocumentModel instance = persistenceService.createDocumentRouteInstanceFromDocumentRouteModel(
                model, session);
        DocumentModel newModel = persistenceService.saveDocumentRouteInstanceAsNewModel(
                instance, session.getRootDocument(), session);
        assertNotNull(newModel);
    }
}
