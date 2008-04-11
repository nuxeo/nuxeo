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

package org.nuxeo.ecm.platform.site.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.runtime.api.Framework;

/**
 * Default resolver for SiteObjects : based on Core API and SearchService API
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */

public class DefaultSiteResolver implements SiteResourceResolver {

    private boolean allowUnresolvedSubPath = true;

    public DefaultSiteResolver() {
    }

    public DefaultSiteResolver(boolean allowUnresolvedSubPath) {
        this.allowUnresolvedSubPath = allowUnresolvedSubPath;
    }

    public String getTargetRepositoryName(HttpServletRequest request) {
        return "default";
    }

    public List<DocumentModel> resolvePath(HttpServletRequest request, CoreSession coreSession)
            throws Exception {
        List<DocumentModel> docsInPath = new ArrayList<DocumentModel>();

        String URI = request.getRequestURI();

        // XXX
        URI = URI.replace("/nuxeo/site/", "");

        String[] pathParts = URI.split("/");

        if (pathParts.length == 0) {
            return null;
        }

        String siteRootName = pathParts[0];

        DocumentModel root = getSiteRoot(siteRootName, coreSession);

        docsInPath.add(root);

        DocumentModel parent = root;
        for (int i = 1; i < pathParts.length; i++) {
            String name = pathParts[i];

            DocumentModel child = null;
            try {
                child = coreSession.getDocument(new PathRef(parent.getPathAsString() + "/" + name));
            }
            catch (ClientException e) {
                if (allowUnresolvedSubPath) {
                    List<String> unresolvedSibPath = new ArrayList<String>();
                    unresolvedSibPath.addAll(Arrays.asList(pathParts).subList(i, pathParts.length));
                    request.setAttribute(SiteRequest.UNRESOLVED_SUBPATH, unresolvedSibPath);
                    return docsInPath;
                } else {
                    throw e;
                }
            }


            docsInPath.add(child);
            parent = child;
        }
        return docsInPath;
    }

    public DocumentModel getSiteRoot(String rootName, CoreSession session) throws Exception {

        SearchService searchService = Framework.getService(SearchService.class);

        if (searchService != null) {
            ResultSet result = searchService.searchQuery(new ComposedNXQueryImpl(
                    "Select * from Document where ecm:name='" + rootName + "'"), 0, 1);

            if (result.isEmpty()) {
                throw new ClientException("Unable to resolve root " + rootName);
            }
            ResultItem rootResult = result.get(0);

            String ref = (String) rootResult.get("ecm:uuid");

            return session.getDocument(new IdRef(ref));
        } else {
            DocumentModel root = resolveResourceViaCore(session.getRootDocument(), session,
                    rootName);
            if (root == null) {
                throw new ClientException("Unable to resolve root " + rootName);
            }
            return root;
        }
    }

    private DocumentModel resolveResourceViaCore(DocumentModel node, CoreSession session,
            String rootName) throws ClientException {
        if (rootName.equals(node.getName())) {
            return node;
        }

        if (node.isFolder()) {
            for (DocumentModel child : session.getChildren(node.getRef())) {
                if (rootName.equals(child.getName())) {
                    return child;
                }
            }

            for (DocumentModel child : session.getChildren(node.getRef())) {
                DocumentModel foundDoc = resolveResourceViaCore(child, session, rootName);
                if (foundDoc != null) {
                    return foundDoc;
                }
            }
        }

        return null;
    }
}
