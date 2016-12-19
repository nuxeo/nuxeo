/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter showing the versioning aspects of documents.
 */
public class VersioningDocumentAdapter implements VersioningDocument {

    public final DocumentModel doc;

    public final VersioningService service;

    public VersioningDocumentAdapter(DocumentModel doc) {
        service = Framework.getService(VersioningService.class);
        this.doc = doc;
    }

    @Override
    public String getVersionLabel() {
        return service.getVersionLabel(doc);
    }

}
