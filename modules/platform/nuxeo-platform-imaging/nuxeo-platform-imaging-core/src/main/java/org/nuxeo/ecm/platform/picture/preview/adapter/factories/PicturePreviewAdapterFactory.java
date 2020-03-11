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
package org.nuxeo.ecm.platform.picture.preview.adapter.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Preview adapter factory for the Picture document type
 *
 * @author qlamerand
 */
public class PicturePreviewAdapterFactory implements PreviewAdapterFactory {

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    /**
     * @deprecated since 7.2. The Original view does not exist anymore. See NXP-16070.
     */
    @Deprecated
    protected static final String ORIGINAL_VIEW_NAME = "Original";

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        ConverterBasedHtmlPreviewAdapter adapter = new ConverterBasedHtmlPreviewAdapter();
        adapter.setAdaptedDocument(doc);

        PictureResourceAdapter prAdapter = doc.getAdapter(PictureResourceAdapter.class);
        String xpath = prAdapter.getViewXPath(ORIGINAL_JPEG_VIEW_NAME);
        if (xpath != null) {
            adapter.setDefaultPreviewFieldXPath(xpath + "content");
        } else {
            adapter.setDefaultPreviewFieldXPath(prAdapter.getFirstViewXPath() + "content");
        }

        return adapter;
    }

}
