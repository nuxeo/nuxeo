/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.search.backend.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.backend.impl.AbstractSearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.DocumentModelResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Search engine backend that delegates to the core.
 *
 * @author Florent Guillaume
 *
 */
public class CoreSearchBackend extends AbstractSearchEngineBackend {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreSearchBackend.class);

    public SearchServiceSession createSession() {
        // called by IndexingThreadImpl
        throw new UnsupportedOperationException();
    }

    public void closeSession(String sid) {
        // called by IndexingThreadImpl
        throw new UnsupportedOperationException();
    }

    public void saveAllSessions() {
        // ignore save
    }

    public void clear() throws IndexingException {
        throw new UnsupportedOperationException();
    }

    public void index(ResolvedResources resources) {
        // ignore indexing
    }

    public void deleteAggregatedResources(String key) {
        // ignore deletion
    }

    public void deleteAtomicResource(String key) {
        // ignore deletion
    }

    public ResultSet searchQuery(NativeQueryString queryString, int offset,
            int range) throws QueryException {
        throw new QueryException("Native query not implemented");
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws QueryException {
        throw new QueryException("Native query not implemented");
    }

    public ResultSet searchQuery(ComposedNXQuery nxQuery, int offset, int limit)
            throws SearchException {
        SearchPrincipal searchPrincipal = nxQuery.getSearchPrincipal();
        Serializable principal = getPrincipal(searchPrincipal);

        Repository repository;
        CoreSession session;
        try {
            repository = Framework.getService(RepositoryManager.class).getDefaultRepository();
            if (repository == null) {
                throw new ClientException("Cannot get default repository");
            }
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put("principal", principal);
            session = repository.open(context);
        } catch (Exception e) {
            throw new SearchException(e);
        }
        try {
            return searchQuery(nxQuery.getQuery(), offset, limit, session,
                    searchPrincipal);
        } catch (ClientException e) {
            throw new SearchException(e);
        } finally {
            try {
                repository.close(session);
            } catch (Exception e) {
                throw new SearchException(e);
            }
        }
    }

    protected ResultSet searchQuery(SQLQuery sqlQuery, int offset, int limit,
            CoreSession session, SearchPrincipal searchPrincipal)
            throws ClientException {
        String query = sqlQuery.toString();
        DocumentModelList documentModelList = session.query(query, null, limit,
                offset, true);
        int totalHits = (int) documentModelList.totalSize();
        int pageHits = documentModelList.size();
        List<ResultItem> resultItems = new ArrayList<ResultItem>(pageHits);
        for (DocumentModel doc : documentModelList) {
            if (doc == null) {
                log.error("Got null document from query: " + query);
                continue;
            }
            // detach the document so that we can use it beyond the session
            try {
                ((DocumentModelImpl) doc).detach(true);
            } catch (DocumentSecurityException e) {
                // no access to the document (why?)
                continue;
            }
            resultItems.add(new DocumentModelResultItem(doc));
        }
        return new ResultSetImpl(sqlQuery, searchPrincipal, offset, limit,
                resultItems, totalHits, pageHits);
    }

    protected static Serializable getPrincipal(SearchPrincipal searchPrincipal) {
        if (searchPrincipal == null) {
            return new UserPrincipal(SecurityConstants.SYSTEM_USERNAME, null,
                    false, true);
        }
        Serializable originalPrincipal = searchPrincipal.getOriginalPrincipal();
        if (originalPrincipal != null) {
            return originalPrincipal;
        }
        return new UserPrincipal(searchPrincipal.getName(),
                Arrays.asList(searchPrincipal.getGroups()), false,
                searchPrincipal.isAdministrator());
    }
}
