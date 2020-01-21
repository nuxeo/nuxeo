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

package org.nuxeo.ecm.automation.core.operations.document;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.DummyThumbnailFactory.DUMMY_THUMBNAIL_CONTENT;
import static org.nuxeo.ecm.core.blob.ColdStorageHelper.COLD_STORAGE_CONTENT_PROPERTY;
import static org.nuxeo.ecm.core.blob.ColdStorageHelper.FILE_CONTENT_PROPERTY;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-coldstorage-contrib.xml")
public class MoveToColdStorageTest {

    protected static final String FILE_CONTENT = "foo and boo";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void shouldMoveToColdStorage() throws OperationException, IOException {
        DocumentModel documentModel = createDocument(true);
        moveContentToColdStorage(documentModel);
    }

    @Test
    public void shouldFailWhenMovingDocumentBlobAlreadyInColdStorage() throws OperationException, IOException {
        DocumentModel documentModel = createDocument(true);
        // make a move
        moveContentToColdStorage(documentModel);
        try {
            // try to make a second move
            moveContentToColdStorage(documentModel);
            fail("Should fail because the content is already in cold storage");
        } catch (NuxeoException ne) {
            assertEquals(SC_CONFLICT, ne.getStatusCode());
        }
    }

    @Test
    public void shouldFailWhenMovingToColdStorageDocumentWithoutContent() throws OperationException, IOException {
        DocumentModel documentModel = createDocument(false);
        try {
            moveContentToColdStorage(documentModel);
            fail("Should fail because there is no main content associated with the document");
        } catch (NuxeoException ne) {
            assertEquals(SC_NOT_FOUND, ne.getStatusCode());
        }
    }

    protected void moveContentToColdStorage(DocumentModel documentModel) throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(documentModel);
            DocumentModel updatedDocModel = (DocumentModel) automationService.run(context, MoveToColdStorage.ID);
            Blob fileContent = (Blob) updatedDocModel.getPropertyValue(FILE_CONTENT_PROPERTY);
            Blob coldStorageContent = (Blob) updatedDocModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY);
            assertEquals(documentModel.getRef(), updatedDocModel.getRef());
            assertTrue(updatedDocModel.hasFacet(COLD_STORAGE));
            assertEquals(DUMMY_THUMBNAIL_CONTENT, fileContent.getString());
            assertEquals(FILE_CONTENT, coldStorageContent.getString());
        }
    }

    protected DocumentModel createDocument(boolean withBlobContent) {
        DocumentModel documentModel = session.createDocumentModel("/", "MyFile", "File");
        if (withBlobContent) {
            documentModel.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) Blobs.createBlob(FILE_CONTENT));
        }
        return session.createDocument(documentModel);
    }

}
