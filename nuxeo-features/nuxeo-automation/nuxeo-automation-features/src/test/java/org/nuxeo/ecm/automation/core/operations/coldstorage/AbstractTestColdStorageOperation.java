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

package org.nuxeo.ecm.automation.core.operations.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.DummyThumbnailFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.ColdStorageHelper;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.test.ColdStorageFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(ColdStorageFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public abstract class AbstractTestColdStorageOperation {

    protected static final String FILE_CONTENT = "foo and boo";

    @Inject
    protected AutomationService automationService;

    protected void moveContentToColdStorage(CoreSession session, DocumentModel documentModel)
            throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(documentModel);
            DocumentModel updatedDocModel = (DocumentModel) automationService.run(context, MoveToColdStorage.ID);
            Blob fileContent = (Blob) updatedDocModel.getPropertyValue(ColdStorageHelper.FILE_CONTENT_PROPERTY);
            Blob coldStorageContent = (Blob) updatedDocModel.getPropertyValue(
                    ColdStorageHelper.COLD_STORAGE_CONTENT_PROPERTY);
            assertEquals(documentModel.getRef(), updatedDocModel.getRef());
            assertTrue(updatedDocModel.hasFacet(FacetNames.COLD_STORAGE));
            assertEquals(DummyThumbnailFactory.DUMMY_THUMBNAIL_CONTENT, fileContent.getString());
            assertEquals(FILE_CONTENT, coldStorageContent.getString());
        }
    }

    protected DocumentModel createFileDocument(CoreSession session, boolean withBlobContent) {
        DocumentModel documentModel = session.createDocumentModel("/", "MyFile", "File");
        if (withBlobContent) {
            documentModel.setPropertyValue(ColdStorageHelper.FILE_CONTENT_PROPERTY,
                    (Serializable) Blobs.createBlob(FILE_CONTENT));
        }
        return session.createDocument(documentModel);
    }
}
