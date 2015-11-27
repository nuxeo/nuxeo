/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.impl.StoredRendition;

/**
 * @since 8.1
 */
public class DummyStoredRenditionManager extends DefaultStoredRenditionManager {

    @Override
    public StoredRendition createStoredRendition(DocumentModel liveDocument, DocumentModel versionDocument,
            Blob renditionBlob, RenditionDefinition renditionDefinition) {
        DocumentModel doc = versionDocument == null ? liveDocument : versionDocument;
        String string = (String) doc.getPropertyValue("dc:description");
        Blob blob = Blobs.createBlob(string, renditionBlob.getMimeType());
        return super.createStoredRendition(liveDocument, versionDocument, blob, renditionDefinition);
    }

}
