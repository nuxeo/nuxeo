/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.resolver;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SearchHelper {


    public static DocumentModel search(CoreSession session, String query) throws Exception {
        DocumentModel root = null;
        SearchService searchService = Framework.getService(SearchService.class);

        if (searchService != null) {
            ResultSet result = searchService.searchQuery(new ComposedNXQueryImpl(query), 0, 1);
            if (result.isEmpty()) {
                return null;
            }
            ResultItem rootResult = result.get(0);
            String ref = (String) rootResult.get("ecm:uuid");
            root = session.getDocument(new IdRef(ref));
        }
        return root;
    }

    public static DocumentModelList search(CoreSession session, String query, int pageOffset, int pageSize) throws Exception {
        SearchService searchService = Framework.getService(SearchService.class);
        ResultSet result = searchService.searchQuery(new ComposedNXQueryImpl(query), pageOffset, pageSize);
        DocumentModelList docs = new DocumentModelListImpl();
        for (ResultItem item : result) {
            String id = (String)item.get("ecm:uuid");
            DocumentModel doc = session.getDocument(new IdRef(id));
            docs.add(doc);
        }
        return docs;
    }

}
