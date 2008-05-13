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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.runtime.api.Framework;

/**
 * Default resolver for SiteObjects : based on Core API and SearchService API
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultDocumentResolver implements DocumentResolver {

    public static final DefaultDocumentResolver INSTANCE = new DefaultDocumentResolver();

    //    public DocumentModel getRootDocument(SiteRoot siteRoot, String rootName, CoreSession session) throws Exception {
//        DocumentModel root = null;
//        String query = "Select * from Document where ecm:name='" + rootName+"'";
//        DocumentModelList result = session.query(query);
//        if (!result.isEmpty()) {
//            root = result.get(0);
//        }
//        return root;
//    }

    public DocumentModel getRootDocument(WebApplication app, String rootName, CoreSession session) throws Exception {
        DocumentModel root = null;
        SearchService searchService = Framework.getService(SearchService.class);

        String query = "Select * from Document where ecm:name='" + rootName + "'";
        if (searchService != null) {
            ResultSet result = searchService.searchQuery(new ComposedNXQueryImpl(query), 0, 1);
            if (result.isEmpty()) {
                return null;
            }
            ResultItem rootResult = result.get(0);
            String ref = (String) rootResult.get("ecm:uuid");
            root = session.getDocument(new IdRef(ref));
        } else {
            DocumentModelList result = session.query(query);
            if (!result.isEmpty()) {
                root = result.get(0);
            }
        }
        return root;
    }

    public DocumentModel getSiteSegment(WebApplication app, DocumentModel parent, String segment, CoreSession session) throws Exception {
        try {
            return session.getDocument(new PathRef(parent.getPathAsString() + '/' + segment));
        } catch (ClientException e) {
            return null;
        }
    }

}
