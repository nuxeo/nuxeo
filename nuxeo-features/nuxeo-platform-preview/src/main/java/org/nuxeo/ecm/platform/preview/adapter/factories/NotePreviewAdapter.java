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

    public HtmlPreviewAdapter getAdapter(DocumentModel doc) {
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("note:note");
        PreprocessedHtmlPreviewAdapter adapter = new NoteHtmlPreviewAdapter(xpaths);
        adapter.setAdaptedDocument(doc);
        return adapter;
    }

}
