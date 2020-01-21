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

package org.nuxeo.ecm.automation.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.DummyThumbnailFactory.DUMMY_THUMBNAIL_CONTENT;
import static org.nuxeo.ecm.core.blob.ColdStorageHelper.COLD_STORAGE_BEING_RETRIEVED_PROPERTY;
import static org.nuxeo.ecm.core.blob.ColdStorageHelper.COLD_STORAGE_CONTENT_PROPERTY;
import static org.nuxeo.ecm.core.blob.ColdStorageHelper.FILE_CONTENT_PROPERTY;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.MoveToColdStorage;
import org.nuxeo.ecm.automation.core.operations.document.RetrieveFromColdStorage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class ColdStorageTestUtils {

    public static final String FILE_CONTENT = "foo and boo";

    public static final int DEFAULT_NUMBER_OF_DAYS_OF_AVAILABILITY = 5;

    public static void moveContentToColdStorage(CoreSession session, DocumentModel documentModel)
            throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(documentModel);
            DocumentModel updatedDocModel = (DocumentModel) Framework.getService(AutomationService.class)
                                                                     .run(context, MoveToColdStorage.ID);
            Blob fileContent = (Blob) updatedDocModel.getPropertyValue(FILE_CONTENT_PROPERTY);
            Blob coldStorageContent = (Blob) updatedDocModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY);
            assertEquals(documentModel.getRef(), updatedDocModel.getRef());
            assertTrue(updatedDocModel.hasFacet(COLD_STORAGE));
            assertEquals(DUMMY_THUMBNAIL_CONTENT, fileContent.getString());
            assertEquals(FILE_CONTENT, coldStorageContent.getString());
        }
    }

    public static void retrieveContentFromColdStorage(CoreSession session, DocumentModel documentModel)
            throws OperationException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(documentModel);
            context.setInput(documentModel);
            AutomationService automationService = Framework.getService(AutomationService.class);
            Map<String, Integer> params = Map.of("numberOfDaysOfAvailability", DEFAULT_NUMBER_OF_DAYS_OF_AVAILABILITY);
            DocumentModel updatedDocument = (DocumentModel) automationService.run(context, RetrieveFromColdStorage.ID,
                    params);
            assertTrue((boolean) updatedDocument.getPropertyValue(COLD_STORAGE_BEING_RETRIEVED_PROPERTY));
        }
    }

    public static DocumentModel createDocument(CoreSession session, boolean withBlobContent) {
        DocumentModel documentModel = session.createDocumentModel("/", "MyFile", "File");
        if (withBlobContent) {
            documentModel.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) Blobs.createBlob(FILE_CONTENT));
        }
        return session.createDocument(documentModel);
    }

    private ColdStorageTestUtils() {
        // no instance allowed
    }
}
