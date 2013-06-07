/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.api.adapters;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_SCHEMA_NAME;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory instantiating {@link NoPictureResourceAdapter} adapter.
 */
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
