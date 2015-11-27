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
