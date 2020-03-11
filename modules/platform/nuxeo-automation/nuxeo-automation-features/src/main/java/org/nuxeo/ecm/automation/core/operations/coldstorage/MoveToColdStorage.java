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

import java.io.Serializable;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.blob.ColdStorageHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Moves the main content associated with the input {@link DocumentModel} to the cold storage.
 * 
 * @since 11.1
 */
@Operation(id = MoveToColdStorage.ID, category = Constants.CAT_BLOB, label = "Move to Cold Storage", description = "Move the main document content to the cold storage.")
public class MoveToColdStorage {

    public static final String ID = "Document.MoveToColdStorage";

    @Context
    protected CoreSession session;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        // retrieve the thumbnail which will be used to replace the content, once the move done
        Blob thumbnail = Framework.getService(ThumbnailService.class).getThumbnail(doc, session);

        // make the move
        DocumentModel documentModel = ColdStorageHelper.moveContentToColdStorage(session, doc.getRef());

        // replace the file content document by the thumbnail
        documentModel.setPropertyValue(ColdStorageHelper.FILE_CONTENT_PROPERTY, (Serializable) thumbnail);
        if (save) {
            documentModel = session.saveDocument(documentModel);
        }

        return documentModel;
    }

}
