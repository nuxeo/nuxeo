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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.impl.StoredRendition;

/**
 * The default @{link StoredRenditionManager} to Manage {@link StoredRendition}s on behalf of the
 * {@link RenditionService}.
 *
 * @since 8.1
 */
public class DefaultStoredRenditionManager implements StoredRenditionManager {

    @Override
    public StoredRendition createStoredRendition(DocumentModel liveDocument, DocumentModel versionDocument,
            Blob renditionBlob, RenditionDefinition renditionDefinition) {
        RenditionCreator rc = new RenditionCreator(liveDocument, versionDocument, renditionBlob, renditionDefinition);
        rc.runUnrestricted();
        DocumentModel storedDoc = rc.getDetachedRendition();
        CoreSession coreSession = liveDocument.getCoreSession();
        return toStoredRendition(storedDoc, renditionDefinition, coreSession);
    }

    @Override
    public StoredRendition findStoredRendition(DocumentModel sourceDocument, RenditionDefinition renditionDefinition) {
        RenditionFinder finder = new RenditionFinder(sourceDocument, renditionDefinition);
        finder.runUnrestricted();
        DocumentModel storedDoc = finder.getStoredRendition();
        CoreSession coreSession = sourceDocument.getCoreSession();
        return toStoredRendition(storedDoc, renditionDefinition, coreSession);
    }

    /**
     * Wraps the re-attached stored document in a {@link StoredRendition}.
     *
     * @param storedDoc the stored document
     * @param def the rendition definition
     * @param sessionId the session id
     * @return the stored rendition
     */
    protected StoredRendition toStoredRendition(DocumentModel storedDoc, RenditionDefinition def, CoreSession coreSession) {
        if (storedDoc == null) {
            return null;
        }
        // re-attach the detached doc
        storedDoc.attach(coreSession);
        return new StoredRendition(storedDoc, def);
    }

}
