/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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
 * Manages {@link StoredRendition}s on behalf of the {@link RenditionService}.
 *
 * @since 8.1
 */
public interface StoredRenditionManager {

    /**
     * Creates a {@link StoredRendition} for the live document or the version document if provided.
     *
     * @param liveDocument the live {@link DocumentModel}
     * @param versionDocument the version {@link DocumentModel}
     * @param renditionBlob the rendition {@link Blob}
     * @param renditionDefinition the {@link RenditionDefinition}
     * @return the created {@link StoredRendition}.
     */
    public StoredRendition createStoredRendition(DocumentModel liveDocument, DocumentModel versionDocument,
            Blob renditionBlob, RenditionDefinition renditionDefinition);

    /**
     * Finds the {@link StoredRendition} associated with a {@link DocumentModel}.
     *
     * @param sourceDocument the source {@link DocumentModel}
     * @param renditionDefinition the {@link RenditionDefinition}
     * @return the found {@link StoredRendition}
     */
    public StoredRendition findStoredRendition(DocumentModel sourceDocument, RenditionDefinition renditionDefinition);

}
