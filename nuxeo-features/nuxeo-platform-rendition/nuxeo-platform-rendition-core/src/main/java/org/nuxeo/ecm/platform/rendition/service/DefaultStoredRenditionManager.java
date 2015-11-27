/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.ecm.core.api.Blob;
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
        String sessionId = liveDocument.getCoreSession().getSessionId();
        return toStoredRendition(storedDoc, renditionDefinition, sessionId);
    }

    @Override
    public StoredRendition findStoredRendition(DocumentModel sourceDocument, RenditionDefinition renditionDefinition) {
        RenditionFinder finder = new RenditionFinder(sourceDocument, renditionDefinition);
        finder.runUnrestricted();
        DocumentModel storedDoc = finder.getStoredRendition();
        String sessionId = sourceDocument.getCoreSession().getSessionId();
        return toStoredRendition(storedDoc, renditionDefinition, sessionId);
    }

    /**
     * Wraps the re-attached stored document in a {@link StoredRendition}.
     *
     * @param storedDoc the stored document
     * @param def the rendition definition
     * @param sessionId the session id
     * @return the stored rendition
     */
    protected StoredRendition toStoredRendition(DocumentModel storedDoc, RenditionDefinition def, String sessionId) {
        if (storedDoc == null) {
            return null;
        }
        // re-attach the detached doc
        storedDoc.attach(sessionId);
        return new StoredRendition(storedDoc, def);
    }

}
