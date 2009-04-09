/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SearchServiceBean.java 29870 2008-02-02 09:12:39Z janguenot $
 */

package org.nuxeo.ecm.platform.search.ejb;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.ejb.remote.SearchServiceRemote;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.core.search.service.SearchServiceImpl;
import org.nuxeo.ecm.platform.search.ejb.local.SearchServiceLocal;
import org.nuxeo.runtime.api.Framework;

/**
 * Search service session bean.
 *
 * <p>
 * This session bean expects the Nuxeo Runtime core search service available
 * locally (i.e : same JVM).
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Stateless
@Remote(SearchServiceRemote.class)
@Local(SearchServiceLocal.class)
public class SearchServiceBean implements SearchService {

    private static final long serialVersionUID = -589486174154627675L;

    private static final Log log = LogFactory.getLog(SearchServiceBean.class);

    private transient SearchService service;

    @Resource
    transient EJBContext context;

    private SearchService getSearchService() {
        if (service == null) {
            // NXSearch uses a direct access here since thus EJB has to be
            // deployed along with the Nuxeo Runtime core component within the
            // same JVM. Do not use platform service API to avoid for further
            // lookup here.
            service = Framework.getLocalService(SearchService.class);
        }
        return service;
    }

    public void deleteAggregatedResources(String key) throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().deleteAggregatedResources(key);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public void deleteAtomicResource(String key) throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().deleteAtomicResource(key);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public void index(IndexableResources sources, boolean fulltext)
            throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().index(sources, fulltext);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public void unindex(DocumentModel dm) throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().unindex(dm);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException, QueryException {
        ResultSet resultSet = null;
        if (getSearchService() != null) {
            // Set the search principal here.
            nativeQuery.setSearchPrincipal(getSearchPrincipal());
            resultSet = getSearchService().searchQuery(nativeQuery, offset,
                    range);
        } else {
            log.error("Cannot find core search service, returning an empty result set....");
        }
        return resultSet;
    }

    public ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset,
            int range) throws SearchException, QueryException {
        ResultSet resultSet = null;
        if (getSearchService() != null) {
            // Set the search principal here.
            nxqlQuery.setSearchPrincipal(getSearchPrincipal());
            resultSet = getSearchService().searchQuery(nxqlQuery, offset, range);
        } else {
            log.error("Cannot find core search service, returning an empty result set....");
        }
        return resultSet;
    }

    public ResultSet searchQuery(NativeQueryString queryString,
            String backendName, int offset, int range) throws SearchException,
            QueryException {
        ResultSet resultSet = null;
        if (getSearchService() != null) {
            // Set the search principal here.
            queryString.setSearchPrincipal(getSearchPrincipal());
            resultSet = getSearchService().searchQuery(queryString,
                    backendName, offset, range);
        } else {
            log.error("Cannot find core search service, returning an empty result set....");
        }
        return resultSet;
    }

    public List<String> getSupportedAnalyzersFor(String backendName) {
        List<String> capabilities = Collections.emptyList();
        if (getSearchService() != null) {
            capabilities = getSearchService().getSupportedAnalyzersFor(
                    backendName);
        } else {
            log.error("Cannot find core search service....");
        }
        return capabilities;
    }

    public List<String> getSupportedFieldTypes(String backendName) {
        List<String> capabilities = Collections.emptyList();
        if (getSearchService() != null) {
            capabilities = getSearchService().getSupportedFieldTypes(
                    backendName);
        } else {
            log.error("Cannot find core search service....");
        }
        return capabilities;
    }

    public IndexableResourceConf getIndexableResourceConfByName(String name,
            boolean full) {
        IndexableResourceConf conf = null;
        if (getSearchService() != null) {
            conf = getSearchService().getIndexableResourceConfByName(name, full);
        } else {
            log.error("Cannot find core search service....");
        }
        return conf;
    }

    public IndexableDocType getIndexableDocTypeFor(String docType) {
        IndexableDocType iDocType = null;
        if (getSearchService() != null) {
            iDocType = getSearchService().getIndexableDocTypeFor(docType);
        } else {
            log.error("Cannot find core search service....");
        }
        return iDocType;
    }

    public Map<String, IndexableResourceConf> getIndexableResourceConfs() {
        Map<String, IndexableResourceConf> confs;
        if (getSearchService() != null) {
            confs = getSearchService().getIndexableResourceConfs();
        } else {
            log.error("Cannot find core search service....");
            // Always return ar least an empty map.
            confs = new HashMap<String, IndexableResourceConf>();
        }
        return confs;
    }

    public String[] getAvailableBackendNames() {
        String[] names;
        if (getSearchService() != null) {
            names = getSearchService().getAvailableBackendNames();
        } else {
            log.error("Cannot find core search service....");
            // Always return ar least an empty map.
            names = new String[0];
        }
        return names;
    }

    protected SearchPrincipal getSearchPrincipal() {
        Principal principal = context.getCallerPrincipal();
        return getSearchPrincipal(principal);
    }

    public SearchPrincipal getSearchPrincipal(Principal principal) {
        SearchPrincipal sprincipal = null;
        if (getSearchService() != null) {
            sprincipal = getSearchService().getSearchPrincipal(principal);
        } else {
            log.error("Cannot find core search service....");
        }
        return sprincipal;
    }

    public IndexableResourceConf getIndexableResourceConfByPrefix(
            String prefix, boolean full) {
        if (getSearchService() != null) {
            return getSearchService().getIndexableResourceConfByPrefix(prefix,
                    full);
        }
        log.error("Cannot find core search service....");
        return null;
    }

    public boolean isEnabled() {
        if (getSearchService() != null) {
            return getSearchService().isEnabled();
        }
        log.error("Cannot find core search service....");
        return false;
    }

    public void setStatus(boolean active) {
        if (getSearchService() != null) {
            getSearchService().setStatus(active);
        }
        log.error("Cannot find core search service....");
    }

    public FulltextFieldDescriptor getFullTextDescriptorByName(
            String prefixedName) {
        if (getSearchService() != null) {
            return getSearchService().getFullTextDescriptorByName(prefixedName);
        }
        log.error("Cannot find core search service....");
        return null;
    }

    public IndexingEventConf getIndexingEventConfByName(String name) {
        if (getSearchService() != null) {
            return getSearchService().getIndexingEventConfByName(name);
        }
        log.error("Cannot find core search service....");
        return null;
    }

    public void invalidateComputedIndexableResourceConfs() {
        if (getSearchService() != null) {
            getSearchService().invalidateComputedIndexableResourceConfs();
        }
        log.error("Cannot find core search service....");
    }

    public void clear() throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().clear();
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public BlobExtractor getBlobExtractorByName(String name) {
        if (getSearchService() != null) {
            return getSearchService().getBlobExtractorByName(name);
        }
        log.error("Cannot find core search service....");
        return null;
    }

    public ResourceTypeDescriptor getResourceTypeDescriptorByName(String name) {
        if (getSearchService() != null) {
            return getSearchService().getResourceTypeDescriptorByName(name);
        }
        log.error("Cannot find core search service....");
        return null;
    }

    public void index(ResolvedResources sources) throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().index(sources);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public long getIndexingWaitingQueueSize() {
        if (getSearchService() != null) {
            return getSearchService().getIndexingWaitingQueueSize();
        } else {
            log.error("Cannot find core search service....");
            return 0;
        }
    }

    public int getNumberOfIndexingThreads() {
        if (getSearchService() != null) {
            return getSearchService().getNumberOfIndexingThreads();
        } else {
            log.error("Cannot find core search service....");
            return 0;
        }
    }

    public void closeSession(String sid) {
        if (getSearchService() != null) {
            getSearchService().closeSession(sid);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public SearchServiceSession openSession() {
        if (getSearchService() != null) {
            return getSearchService().openSession();
        } else {
            log.error("Cannot find core search service....");
            return null;
        }
    }

    public int getIndexingDocBatchSize() {
        if (getSearchService() != null) {
            return getSearchService().getIndexingDocBatchSize();
        } else {
            log.error("Cannot find core search service....");
            return SearchServiceImpl.DEFAULT_DOC_BATCH_SIZE;
        }
    }

    public void setIndexingDocBatchSize(int docBatchSize) {
        if (getSearchService() != null) {
            getSearchService().setIndexingDocBatchSize(docBatchSize);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public void setNumberOfIndexingThreads(int numberOfIndexingThreads) {
        if (getSearchService() != null) {
            getSearchService().setNumberOfIndexingThreads(
                    numberOfIndexingThreads);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public void saveAllSessions() throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().saveAllSessions();
        } else {
            log.error("Cannot find core search service....");
        }
    }

    @Deprecated
    public void reindexAll(String repoName, String path, boolean fulltext)
            throws IndexingException {
        if (getSearchService() != null) {
            getSearchService().reindexAll(repoName, path, fulltext);
        } else {
            log.error("Cannot find core search service....");
        }
    }

    public int getActiveIndexingTasks() {
        if (getSearchService() != null) {
            return getSearchService().getActiveIndexingTasks();
        } else {
            log.error("Cannot find core search service....");
            return 0;
        }
    }

    public long getTotalCompletedIndexingTasks() {
        if (getSearchService() != null) {
            return getSearchService().getTotalCompletedIndexingTasks();
        } else {
            log.error("Cannot find core search service....");
            return 0;
        }
    }

    public boolean isReindexingAll() {
        if (getSearchService() != null) {
            return getSearchService().isReindexingAll();
        } else {
            log.error("Cannot find core search service....");
            return false;
        }
    }

    public void setReindexingAll(boolean flag) {
        if (getSearchService() != null) {
            getSearchService().setReindexingAll(flag);
        } else {
            log.error("Cannot find core search service....");
        }
    }

}
