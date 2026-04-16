/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.management;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.Math.max;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.getFromClause;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.apache.commons.collections4.CollectionUtils;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.search.SearchIndex;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.ecm.core.search.SearchService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 2025.0
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "search")
@Produces(APPLICATION_JSON)
public class SearchObject extends AbstractResource<ResourceTypeImpl> {
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String DEFAULT_CHECK_SEARCH_NXQL = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND ecm:isTrashed = 0";

    protected static final Long DEFAULT_CHECK_SEARCH_PAGE_SIZE = 10L;

    protected static final String CHECK_SEARCH_NXQL_PP = "search_check_nxql";

    protected static final Integer DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * Performs indexing on documents matching the optional NXQL query on the default repository. An empty query means
     * reindex all using a new index.
     */
    @POST
    @Path("reindex")
    public BulkStatus doIndexing(@QueryParam("query") String query,
            @QueryParam("queryLimit") @DefaultValue("-1") Long queryLimit, @QueryParam("index") List<String> indexes) {
        return performIndexing(query, queryLimit, indexes);
    }

    /**
     * Performs indexing on the given document and its children.
     *
     * @param documentId the id of the document that will be indexed and his children recursively
     */
    @POST
    @Path("{documentId}/reindex")
    public BulkStatus doIndexingOnDocument(@PathParam("documentId") String documentId,
            @QueryParam("queryLimit") @DefaultValue("-1") Long queryLimit, @QueryParam("index") List<String> indexes) {
        String query = "Select * From %s where %s = '%s' or %s = '%s'".formatted( //
                getFromClause(), //
                NXQL.ECM_UUID, documentId, //
                NXQL.ECM_ANCESTORID, documentId);
        return performIndexing(query, queryLimit, indexes);
    }

    /**
     * Executes a refresh on document index of a given repository.
     */
    @POST
    @Path("refresh")
    public void doRefresh() {
        var searchService = Framework.getService(SearchService.class);
        var searchIndexingService = Framework.getService(SearchIndexingService.class);
        String repository = ctx.getCoreSession().getRepositoryName();
        searchService.getIndexNames(repository)
                     .stream()
                     .map(searchService::getSearchIndex)
                     .forEach(searchIndexingService::refresh);
    }

    /**
     * @deprecated since 2025.8 use {@link #performIndexing(String, long, List<String>)} instead
     */
    @Deprecated(since = "2025.8", forRemoval = true)
    protected BulkStatus performIndexing(String query) {
        return performIndexing(query, -1, null);
    }

    /**
     * Performs indexing on documents matching the optional NXQL query.
     */
    protected BulkStatus performIndexing(String query, long queryLimit, List<String> indexes) {
        String repository = ctx.getCoreSession().getRepositoryName();
        var searchIndexingService = Framework.getService(SearchIndexingService.class);
        BulkService bulkService = Framework.getService(BulkService.class);
        try {
            String commandId = isBlank(query) ? searchIndexingService.reindexRepository(repository, indexes)
                    : searchIndexingService.reindexDocuments(repository, query, queryLimit, indexes);
            return bulkService.getStatus(commandId);
        } catch (IllegalStateException e) {
            // exclusive bulk command
            throw new ConcurrentUpdateException(e.getMessage(), e);
        } catch (RuntimeServiceException e) {
            // unable to drop the index, security reason or alias
            throw new NuxeoException(e.getMessage(), SC_BAD_REQUEST);
        }
    }

    /**
     * Check discrepancies between search indexes.
     */
    @GET
    @Path("checkSearch")
    public String checkSearch(@QueryParam("nxql") String nxql, @QueryParam("pageSize") Long pageSize,
            @QueryParam("index") List<String> indexes) {
        if (nxql == null || nxql.isBlank()) {
            nxql = DEFAULT_CHECK_SEARCH_NXQL;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_CHECK_SEARCH_PAGE_SIZE;
        }
        SearchService service = Framework.getService(SearchService.class);
        String repository = ctx.getCoreSession().getRepositoryName();
        var repoIndexes = service.getIndexNames(repository);
        List<String> checkIndexes;
        if (CollectionUtils.isEmpty(indexes)) {
            checkIndexes = repoIndexes;
        } else {
            if (indexes.stream().filter(index -> !repoIndexes.contains(index)).count() > 0) {
                throw new NuxeoException("Unexisting index submitted for repository: " + repository, SC_BAD_REQUEST);
            }
            checkIndexes = indexes;
        }
        Map<String, Serializable> ret = new HashMap<>();
        ret.put("query", nxql);
        for (String index : checkIndexes) {
            var searchIndex = service.getSearchIndex(index);
            Map<String, Serializable> map = extractResultInfo(searchIndex, nxql, pageSize);
            ret.put("order", map.get("order"));
            map.remove("order");
            ret.put(searchIndex.client() + "/" + searchIndex.index(), (Serializable) map);
        }
        try {
            return MAPPER.writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            throw new NuxeoException(e);
        }
    }

    protected Map<String, Serializable> extractResultInfo(SearchIndex searchIndex, String nxql, long pageSize) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(CHECK_SEARCH_NXQL_PP);
        HashMap<String, Serializable> params = new HashMap<>();
        params.put(CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());
        var pp = (SearchServicePageProvider) pageProviderService.getPageProvider(CHECK_SEARCH_NXQL_PP, ppdef, null,
                null, pageSize, 0L, params);
        pp.setSearchIndexes(List.of(searchIndex));
        pp.setParameters(new String[] { nxql });
        long start = System.currentTimeMillis();
        List<DocumentModel> res = pp.getCurrentPage();
        long duration = System.currentTimeMillis() - start;
        Map<String, Serializable> ret = new HashMap<>();
        ret.put("pageProvider", CHECK_SEARCH_NXQL_PP);
        ret.put("pageSize", pageSize);
        ret.put("took", duration);
        ret.put("resultsCount", pp.getResultsCount());
        ret.put("resultsCountLimit", pp.getResultsCountLimit());
        ret.put("order", pp.getSortInfo());
        ret.put("results", (Serializable) res.stream().map(DocumentModel::getId).collect(Collectors.toList()));
        return ret;
    }

    /**
     * Wait for indexing and other services only for testing purpose. Returns no content (204) on success, timeout (408)
     * or errors (50x)
     */
    @POST
    @Path("wait")
    public void doWait(@QueryParam("waitForAudit") boolean waitForAudit,
            @QueryParam("waitForBulkService") boolean waitForBulkService, @QueryParam("refresh") boolean refresh,
            @QueryParam("timeoutSecond") Integer timeoutSecond) {
        int timeout = Objects.requireNonNullElse(timeoutSecond, DEFAULT_TIMEOUT_SECONDS);
        long start = System.currentTimeMillis();
        try {
            if (waitForBulkService) {
                // bulk service include work manager
                var service = Framework.getService(BulkService.class);
                if (service != null && !service.await(Duration.ofSeconds(timeout))) {
                    throw new NuxeoException("Timeout on bulk service, after " + timeout + "s", SC_REQUEST_TIMEOUT);
                }
            } else {
                var service = Framework.getService(WorkManager.class);
                if (service != null && !service.awaitCompletion(timeout, TimeUnit.SECONDS)) {
                    throw new NuxeoException("Timeout on work manager, after " + timeout + "s", SC_REQUEST_TIMEOUT);
                }
            }
            if (!Framework.getService(SearchIndexingService.class).await(computeRemainingDuration(timeout, start))) {
                throw new NuxeoException("Timeout on search service, after " + timeout + "s", SC_REQUEST_TIMEOUT);
            }
            // wait for audit
            if (waitForAudit) {
                var service = Framework.getService(AuditService.class);
                if (service != null && !service.await(computeRemainingDuration(timeout, start))) {
                    throw new NuxeoException("Timeout on audit service, after " + timeout + "s", SC_REQUEST_TIMEOUT);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException("Interrupted");
        }
        if (refresh) {
            var searchService = Framework.getService(SearchService.class);
            var searchIndexingService = Framework.getService(SearchIndexingService.class);
            String repository = ctx.getCoreSession().getRepositoryName();
            var searchIndex = searchService.getSearchIndex(searchService.getDefaultIndexName(repository));
            searchIndexingService.refresh(searchIndex);
        }
    }

    protected Duration computeRemainingDuration(long timeout, long start) {
        long elapsed = System.currentTimeMillis() - start;
        // at least one second
        return Duration.ofSeconds(max(timeout - TimeUnit.MILLISECONDS.toSeconds(elapsed), 1));
    }

}
