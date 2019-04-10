/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.diff.pictures;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterFactory;

/**
 * @since 7.4
 */
public class PictureContentDiffAdapterFactory implements ContentDiffAdapterFactory {

    public ContentDiffAdapter getAdapter(DocumentModel doc) {
        ImageMagickContentDiffAdapter adapter = new ImageMagickContentDiffAdapter();
        adapter.setAdaptedDocument(doc);
        return adapter;
    }
}
