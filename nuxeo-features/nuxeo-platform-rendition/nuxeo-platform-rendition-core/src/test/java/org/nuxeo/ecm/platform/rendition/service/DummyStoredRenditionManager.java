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
