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

package org.nuxeo.ecm.webengine.resolver;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.webengine.WebApplication;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.runtime.api.Framework;

/**
 * Default resolver for SiteObjects : based on Core API and SearchService API
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("resolver")
public class DefaultDocumentResolver implements DocumentResolver {

    public final static DefaultDocumentResolver DEFAULT = new DefaultDocumentResolver(
            "Select * from Document where ecm:name='%s'", false);

    protected Map<String, PathRef> mappings;
    protected List<PathRef> roots;

    @XNode("query")
    protected String query;

    @XNode("query/@useCoreSearch")
    protected boolean useCoreSearch = false;


    /**
     *
     */
    public DefaultDocumentResolver() {
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public DefaultDocumentResolver(String query, boolean useCoreSearch) {
        this.query = query;
        this.useCoreSearch = useCoreSearch;
    }


    /**
     * @return the query.
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the roots.
     */
    public List<PathRef> getRoots() {
        return roots;
    }

    /**
     * @param roots the roots to set.
     */
    public void setRoots(List<PathRef> roots) {
        this.roots = roots;
    }

    /**
     * @return the mappings.
     */
    public Map<String, PathRef> getMappings() {
        return mappings;
    }

    /**
     * @param mappings the mappings to set.
     */
    public void setMappings(Map<String, PathRef> mappings) {
        this.mappings = mappings;
    }

    @XNodeMap(value="mappings/mapping", key="@id", componentType=String.class, type=HashMap.class)
    void setMappingsFromStrings(Map<String, String> mappings) {
        this.mappings = new HashMap<String, PathRef>();
        for (Map.Entry<String,String> entry : mappings.entrySet()) {
            this.mappings.put(entry.getKey(), new PathRef(entry.getValue()));
        }
    }

    @XNodeList(value="roots/root", componentType=String.class, type=ArrayList.class)
    void setRootsFromStrings(List<String> roots) {
        this.roots = new ArrayList<PathRef>();
        for (String root : roots) {
            this.roots.add(new PathRef(root));
        }
    }

    public DocumentModel getRootDocument(WebApplication app, String rootName, CoreSession session) throws WebException {
        try {
            if (mappings != null) {
                PathRef ref = mappings.get(rootName);
                // a mapping matched
                if (ref != null) {
                    return session.getDocument(ref);
                }
            }
            // scan through roots
            if (roots != null && !roots.isEmpty()) {
                for (PathRef root : roots) {
                    try {
                        return session.getChild(root, rootName);
                    } catch (DocumentSecurityException e) {
                        throw new WebSecurityException("You don't have permission to view "+rootName, e);
                    } catch (ClientException e) {
                        // do nothing
                    }
                }
            }

            if (query != null) {
                return search(session, String.format(query, rootName));
            }
        } catch (WebException e) {
            throw e;
        } catch (DocumentSecurityException e) {
            throw new WebSecurityException("You don't have permission to view "+rootName, e);
        } catch (Exception e) {
            throw WebException.wrap("Failed to get the root document for "+rootName, e);
        }
       // nothing matched
       return null;
    }


    public DocumentModel search(CoreSession session, String query) throws Exception {
        DocumentModel root = null;
        SearchService searchService = Framework.getService(SearchService.class);

        if (!useCoreSearch && searchService != null) {
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

}
