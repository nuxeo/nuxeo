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
package org.nuxeo.ecm.platform.preview.adapter.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Preview adapter factory for all documents that have the file schema.
 *
 * @author tiry
 */
public class FileBasedPreviewAdapterFactory implements PreviewAdapterFactory {

    private static final String FIRST_FILE_IN_FILES_PROPERTY = "files:files/0/file";

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        ConverterBasedHtmlPreviewAdapter adapter = new ConverterBasedHtmlPreviewAdapter();
        adapter.setAdaptedDocument(doc);
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            if (doc.hasSchema("file")) {
                adapter.setDefaultPreviewFieldXPath("file:content");
            } else {
                // Has "files" schema, set xpath to first blob as default
                try {
                    doc.getProperty(FIRST_FILE_IN_FILES_PROPERTY);
                    adapter.setDefaultPreviewFieldXPath(FIRST_FILE_IN_FILES_PROPERTY);
                } catch (PropertyException e) {
                    // the property does not exist for this document, then return null
                    return null;
                }
            }
        }
        return adapter;
    }

}
