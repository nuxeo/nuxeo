/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */

package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, CollectionFeature.class })
public class CollectionCreateFolderVersionTest {

    protected static final String TEST_FOLDER = "testFolder";

    protected static final String TEST_COLLECTION_IN_FOLDER = "testCollectionInFolder";

    protected static final String TEST_FILE_NAME = "testFile";

    protected static final int NB_TEST_FILES = 3;

    @Inject
    protected CollectionManager collectionManager;

    @Inject
    protected CoreSession session;

    @Test
    public void testCreateFolderVersionContainingACollection() throws Exception {
        List<DocumentModel> files = new ArrayList<>();
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel folder = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FOLDER, "Folder");
        folder = session.createDocument(folder);

        for (int i = 1; i <= NB_TEST_FILES; i++) {
            DocumentModel testFile = session.createDocumentModel(folder.getPath().toString(), TEST_FILE_NAME + i,
                    "File");
            testFile = session.createDocument(testFile);
            files.add(testFile);
        }

        DocumentModel collectionInFolder = session.createDocumentModel(folder.getPathAsString(),
                TEST_COLLECTION_IN_FOLDER, CollectionConstants.COLLECTION_TYPE);
        collectionInFolder = session.createDocument(collectionInFolder);
        session.save();
        collectionManager.addToCollection(collectionInFolder, files, session);
        Collection collectionAdapter = collectionInFolder.getAdapter(Collection.class);

        // Check all children files are in the collection
        assertEquals(NB_TEST_FILES, collectionAdapter.getCollectedDocumentIds().size());

        // Create a new version of the folder
        session.checkIn(folder.getRef(), VersioningOption.MAJOR, "");
        assertEquals(1, session.getVersions(folder.getRef()).size());

        // Test if children documents are all still collected
        List<DocumentModel> children = session.getChildren(folder.getRef());
        int size = 0;
        for (DocumentModel child : children) {
            if (!child.getId().equals(collectionInFolder.getId())) {
                assertTrue(collectionManager.isCollected(child));
                size++;
            }
        }
        assertEquals(NB_TEST_FILES, size);
    }
}
