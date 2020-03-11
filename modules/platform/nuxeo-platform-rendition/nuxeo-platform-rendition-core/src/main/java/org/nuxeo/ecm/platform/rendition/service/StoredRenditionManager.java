/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.impl.StoredRendition;

/**
 * Manages {@link StoredRendition}s on behalf of the {@link RenditionService}.
 *
 * @since 8.1
 */
public interface StoredRenditionManager {

    /**
     * Creates a {@link StoredRendition} for the live document or the version document if provided.
     *
     * @param liveDocument the live document
     * @param versionDocument the version document
     * @param renditionBlob the rendition blob
     * @param renditionDefinition the rendition definition
     * @return the created stored rendition
     */
    StoredRendition createStoredRendition(DocumentModel liveDocument, DocumentModel versionDocument, Blob renditionBlob,
            RenditionDefinition renditionDefinition);

    /**
     * Finds the {@link StoredRendition} associated with a {@link DocumentModel}.
     *
     * @param sourceDocument the source document
     * @param renditionDefinition the rendition definition
     * @return the found stored rendition, or {@code null} if not found
     */
    StoredRendition findStoredRendition(DocumentModel sourceDocument, RenditionDefinition renditionDefinition);

}
