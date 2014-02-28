/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.ecm.platform.userworkspace.types" })
public class CollectionAddRemoveTest {

    private static final String TEST_FILE_NAME = "testFile";

    private static final String COLLECTION_NAME = "testCollection";

    @Inject
    CoreSession session;

    @Inject
    CollectionManager collectionManager;

    @Test
    public void testAddOneDocToNewCollection() throws ClientException {
        DocumentModel testWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(
                testWorkspace.getPathAsString(), TEST_FILE_NAME, "Workspace");
        testFile = session.createDocument(testFile);
        collectionManager.addToNewCollection(COLLECTION_NAME,
                "testDescription", testFile, session);

        final String collectionFolderPath = "/default-domain/UserWorkspaces/Administrator/"
                + CollectionConstants.DEFAULT_COLLECTIONS_NAME;
        assertTrue(session.exists(new PathRef(collectionFolderPath)));

        final String newlyCreatedCollectionPath = "/default-domain/UserWorkspaces/Administrator/"
                + CollectionConstants.DEFAULT_COLLECTIONS_NAME
                + "/"
                + COLLECTION_NAME;
        assertTrue(session.exists(new PathRef(newlyCreatedCollectionPath)));

        DocumentModel newlyCreatedCollection = session.getDocument(new PathRef(
                newlyCreatedCollectionPath));

        Collection collectionAdapter = newlyCreatedCollection.getAdapter(Collection.class);

        assertTrue(collectionAdapter.getCollectedDocuments().contains(testFile));
    }

    @Test
    public void testAddManyDocsToNewCollection() {
        // TODO
    }

    @Test
    public void testRemoveOneDocFromCollection() {
        // TODO
    }

    @Test
    public void testRemoveManyDocsFromCollection() {
        // TODO
    }

}
