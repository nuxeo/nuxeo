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

package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

public class IndexingWrapperDocumentModelAdapterFactory implements DocumentAdapterFactory {

    protected static IndexingWrapperManagerService wrapperService;

    protected IndexingWrapperManagerService getWrapperService() {
        if (wrapperService == null) {
            wrapperService = Framework
                    .getLocalService(IndexingWrapperManagerService.class);
        }
        return wrapperService;
    }

    public Object getAdapter(DocumentModel doc, Class itf) {
        return getWrapperService().getIndexingWrapper(doc);
    }

}
