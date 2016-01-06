/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
