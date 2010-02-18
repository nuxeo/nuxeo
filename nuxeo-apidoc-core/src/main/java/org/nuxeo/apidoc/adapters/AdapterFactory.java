/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.apidoc.adapters;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.documentation.DocumentationItemDocAdapter;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 *
 * Factory for DocumentModelAdapters
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class AdapterFactory implements DocumentAdapterFactory {

    public Object getAdapter(DocumentModel doc, Class adapterClass) {

        if (doc==null) {
            return null;
        }

        if (adapterClass.getSimpleName().equals(BundleGroup.class.getSimpleName())) {
            if (doc.getType().equals(BundleGroup.TYPE_NAME)) {
                return new BundleGroupDocAdapter(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(BundleInfo.class.getSimpleName())) {
            if (doc.getType().equals(BundleInfo.TYPE_NAME)) {
                return new BundleInfoDocAdapter(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(ComponentInfo.class.getSimpleName())) {
            if (doc.getType().equals(ComponentInfo.TYPE_NAME)) {
                return new ComponentInfoDocAdapter(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(ExtensionPointInfo.class.getSimpleName())) {
            if (doc.getType().equals(ExtensionPointInfo.TYPE_NAME)) {
                return new ExtensionPointInfoDocAdapter(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(ExtensionInfo.class.getSimpleName())) {
            if (doc.getType().equals(ExtensionInfo.TYPE_NAME)) {
                return new ExtensionInfoDocAdapter(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(DistributionSnapshot.class.getSimpleName())) {
            if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
                return new RepositoryDistributionSnapshot(doc);
            }
        }

        if (adapterClass.getSimpleName().equals(DocumentationItem.class.getSimpleName())) {
            if (doc.getType().equals(DocumentationItemDocAdapter.DOC_TYPE)) {
                return new DocumentationItemDocAdapter(doc);
            }
        }

        return null;
    }

}
