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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.ColdStorageHelper;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.test.ColdStorageFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(ColdStorageFeature.class)
public class TestColdStorage {

    protected static final String FILE_CONTENT = "foo";

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldMoveToColdStorage() throws IOException {
        DocumentModel documentModel = createDocument(true);

        // move the blob to cold storage
        documentModel = ColdStorageHelper.moveContentToColdStorage(session, documentModel.getRef());
        session.saveDocument(documentModel);
        transactionalFeature.nextTransaction();
        documentModel.refresh();

        assertTrue(documentModel.hasFacet(FacetNames.COLD_STORAGE));

        assertNull(documentModel.getPropertyValue(ColdStorageHelper.FILE_CONTENT_PROPERTY));

        // check if the `coldstorage:coldContent` property contains the original file content
        Blob content = (Blob) documentModel.getPropertyValue(ColdStorageHelper.COLD_STORAGE_CONTENT_PROPERTY);
        assertNotNull(content);
        assertEquals(FILE_CONTENT, content.getString());
        assertEquals("dummy", ((ManagedBlob) content).getProviderId());
    }

    @Test
    public void shouldFailMoveAlreadyInColdStorage() {
        DocumentModel documentModel = createDocument(true);

        // move for the first time
        documentModel = ColdStorageHelper.moveContentToColdStorage(session, documentModel.getRef());
        session.saveDocument(documentModel);

        // try to make another move
        try {
            ColdStorageHelper.moveContentToColdStorage(session, documentModel.getRef());
            fail("Should fail because the content is already in cold storage");
        } catch (NuxeoException e) {
            assertEquals(SC_CONFLICT, e.getStatusCode());
            assertEquals(String.format("The main content for document: %s is already in cold storage.", documentModel),
                    e.getMessage());
        }
    }

    @Test
    public void shouldFailMoveToColdStorageNoContent() {
        DocumentModel documentModel = createDocument(false);
        try {
            ColdStorageHelper.moveContentToColdStorage(session, documentModel.getRef());
            fail("Should fail because there is no main content associated with the document");
        } catch (NuxeoException e) {
            assertEquals(SC_NOT_FOUND, e.getStatusCode());
            assertEquals(String.format("There is no main content for document: %s.", documentModel), e.getMessage());
        }
    }

    protected DocumentModel createDocument(boolean addBlobContent) {
        DocumentModel documentModel = session.createDocumentModel("/", "anyFile", "File");
        if (addBlobContent) {
            documentModel.setPropertyValue("file:content", (Serializable) Blobs.createBlob(FILE_CONTENT));
        }
        return session.createDocument(documentModel);
    }

}
