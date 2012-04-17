/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffAdapter;
import org.nuxeo.ecm.diff.detaileddiff.adapter.DetailedDiffAdapterFactory;
import org.nuxeo.ecm.diff.detaileddiff.adapter.base.ConverterBasedDetailedDiffAdapter;

/**
 * Detailed diff adapter factory for all documents that have a blob holder
 * adapter.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class BlobHolderDetailedDiffAdapterFactory implements
        DetailedDiffAdapterFactory {

    public DetailedDiffAdapter getAdapter(DocumentModel doc) {
        ConverterBasedDetailedDiffAdapter adapter = new ConverterBasedDetailedDiffAdapter();
        adapter.setAdaptedDocument(doc);
        return adapter;
    }

}
