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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterFactory;
import org.nuxeo.ecm.platform.preview.adapter.base.NoteHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.adapter.base.PreprocessedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Preview adapter factory for the Note document type
 *
 * @author tiry
 */
public class NotePreviewAdapter implements PreviewAdapterFactory {

    @Override
    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        List<String> xpaths = new ArrayList<>();
        xpaths.add("note:note");
        PreprocessedHtmlPreviewAdapter adapter = new NoteHtmlPreviewAdapter(xpaths);
        adapter.setAdaptedDocument(doc);
        return adapter;
    }

}
