/*
 * (C) Copyright 2013-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.api.adapters;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_SCHEMA_NAME;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory instantiating {@link NoPictureAdapter} adapter.
 *
 * @deprecated since 11.1, doesn't seem used
 */
@Deprecated(since = "11.1")
public class NoPictureResourceAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> cls) {
        if (doc.hasFacet(PICTURE_FACET) || doc.hasSchema(PICTURE_SCHEMA_NAME)) {
            PictureResourceAdapter adapter = new NoPictureAdapter();
            adapter.setDocumentModel(doc);
            return adapter;
        }
        return null;
    }

}
