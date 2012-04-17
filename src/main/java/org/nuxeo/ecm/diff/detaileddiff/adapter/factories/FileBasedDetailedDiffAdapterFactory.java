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
package org.nuxeo.ecm.diff.detaileddiff.adapter.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.diff.detaileddiff.HtmlDetailedDiffAdapter;
import org.nuxeo.ecm.diff.detaileddiff.adapter.DetailedDiffAdapterFactory;
import org.nuxeo.ecm.diff.detaileddiff.adapter.base.ConverterBasedHtmlDetailedDiffAdapter;

/**
 * Detailed diff adapter factory for all documents that have the file schema.
 *
 * @author Antoine taillefer
 * @since 5.6
 */
public class FileBasedDetailedDiffAdapterFactory implements
        DetailedDiffAdapterFactory {

    public HtmlDetailedDiffAdapter getAdapter(DocumentModel doc) {
        ConverterBasedHtmlDetailedDiffAdapter adapter = new ConverterBasedHtmlDetailedDiffAdapter();
        adapter.setAdaptedDocument(doc);
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            if (doc.hasSchema("file")) {
                adapter.setDefaultDetailedDiffFieldXPath("file:content");
            } else {
                // Has "files" schema, set xpath to first blob as default
                adapter.setDefaultDetailedDiffFieldXPath("files:files/0/file");
            }
        }
        return adapter;
    }

}
