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
package org.nuxeo.ecm.diff.detaileddiff.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory for the DocumentModelAdapter service.
 * <p>
 * Delegates the calls to a service dedicated to Detailed diff Adapter that
 * finds the right adapter implementation according to document type and to
 * registered custom adapters.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class DetailedDiffDocumentModelAdapterFactory implements
        DocumentAdapterFactory {

    protected static DetailedDiffAdapterManager ddaManager;

    protected DetailedDiffAdapterManager getDetailedDiffAdapterManager() {
        if (ddaManager == null) {
            ddaManager = Framework.getLocalService(DetailedDiffAdapterManager.class);
        }
        return ddaManager;
    }

    public Object getAdapter(DocumentModel doc, Class itf) {
        return getDetailedDiffAdapterManager().getAdapter(doc);
    }

}
