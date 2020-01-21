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

package org.nuxeo.ecm.core.blob;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages the cold storage of the main content of a {@link DocumentModel}.
 *
 * @since 11.1
 */
public class ColdStorageHelper {

    private static final Logger log = LogManager.getLogger(ColdStorageHelper.class);

    public static final String FILE_CONTENT_PROPERTY = "file:content";

    public static final String COLD_STORAGE_CONTENT_PROPERTY = "coldstorage:coldContent";

    /**
     * Moves the main content associated with the document of the given {@link DocumentRef} to a cold storage.
     * 
     * @return the updated document model if the move succeeds
     * @throws NuxeoException if there is no main content associated with the given document, or if the main content is
     *             already in the cold storage
     */
    public static DocumentModel moveContentToColdStorage(CoreSession session, DocumentRef documentRef) {
        DocumentModel documentModel = session.getDocument(documentRef);
        log.debug("Move to cold storage the main content of document: {}", documentModel);

        Serializable mainContent = documentModel.getPropertyValue(FILE_CONTENT_PROPERTY);
        if (mainContent == null) {
            throw new NuxeoException(String.format("There is no main content for document: %s.", documentModel),
                    SC_NOT_FOUND);
        }

        if (documentModel.hasFacet(COLD_STORAGE)
                && documentModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY) != null) {
            throw new NuxeoException(
                    String.format("The main content for document: %s is already in cold storage.", documentModel),
                    SC_CONFLICT);
        }

        documentModel.addFacet(COLD_STORAGE);
        documentModel.setPropertyValue(COLD_STORAGE_CONTENT_PROPERTY, mainContent);
        Blob thumbnail = Framework.getService(ThumbnailService.class).getThumbnail(documentModel, session);
        documentModel.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) thumbnail);
        return session.saveDocument(documentModel);
    }

    private ColdStorageHelper() {
        // no instance allowed
    }
}
