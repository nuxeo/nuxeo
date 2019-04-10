/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.diff.content.adapter.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterFactory;
import org.nuxeo.ecm.diff.content.adapter.base.ConverterBasedContentDiffAdapter;

/**
 * Content diff adapter factory for all documents that have the file schema.
 *
 * @author Antoine taillefer
 * @since 5.6
 */
public class FileBasedContentDiffAdapterFactory implements ContentDiffAdapterFactory {

    public ContentDiffAdapter getAdapter(DocumentModel doc) {
        ConverterBasedContentDiffAdapter adapter = new ConverterBasedContentDiffAdapter();
        adapter.setAdaptedDocument(doc);
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            if (doc.hasSchema("file")) {
                adapter.setDefaultContentDiffFieldXPath("file:content");
            } else {
                // Has "files" schema, set xpath to first blob as default
                adapter.setDefaultContentDiffFieldXPath("files:files/0/file");
            }
        }
        return adapter;
    }

}
