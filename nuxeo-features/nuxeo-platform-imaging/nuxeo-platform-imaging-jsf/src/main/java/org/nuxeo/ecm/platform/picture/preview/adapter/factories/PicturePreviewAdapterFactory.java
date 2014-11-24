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
package org.nuxeo.ecm.platform.picture.preview.adapter.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 *
 * Preview adapter factory for the Picture document type
 *
 * @author qlamerand
 *
 */
public class PicturePreviewAdapterFactory implements PreviewAdapterFactory {

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    protected static final String ORIGINAL_VIEW_NAME = "Original";

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        ConverterBasedHtmlPreviewAdapter adapter = new ConverterBasedHtmlPreviewAdapter();
        adapter.setAdaptedDocument(doc);

        PictureResourceAdapter prAdapter = doc.getAdapter(PictureResourceAdapter.class);
        String xpath = prAdapter.getViewXPath(ORIGINAL_JPEG_VIEW_NAME);
        if (xpath == null) {
            xpath = prAdapter.getViewXPath(ORIGINAL_VIEW_NAME);
        }
        if (xpath != null) {
            adapter.setDefaultPreviewFieldXPath(xpath + "content");
        } else {
            adapter.setDefaultPreviewFieldXPath(prAdapter.getFirstViewXPath()
                    + "content");
        }

        return adapter;
    }

}
