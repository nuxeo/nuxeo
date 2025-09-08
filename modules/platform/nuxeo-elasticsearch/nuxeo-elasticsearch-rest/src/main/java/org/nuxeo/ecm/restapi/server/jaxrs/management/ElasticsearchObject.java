/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;
import static org.nuxeo.elasticsearch.bulk.IndexAction.ACTION_NAME;
import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Endpoint to manage Elasticsearch.
 *
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "elasticsearch")
@Produces(APPLICATION_JSON)
public class ElasticsearchObject extends AbstractResource<ResourceTypeImpl> {

    private static final Logger log = LogManager.getLogger(ElasticsearchObject.class);

    public static final String GET_ALL_DOCUMENTS_QUERY = "SELECT * from Document";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String DEFAULT_CHECK_SEARCH_NXQL = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND ecm:isTrashed = 0";

    protected static final Long DEFAULT_CHECK_SEARCH_PAGE_SIZE = 10L;

    /**
     * Performs an ES indexing on documents matching the optional NXQL query.
     *
     * @see #performIndexing(String)
     */
    @POST
    @Path("reindex")
    public BulkStatus doIndexing(@QueryParam("query") String query,
            @QueryParam("queryLimit") @DefaultValue("-1") Long queryLimit) {
        return performIndexing(query, queryLimit);
    }

    /**
     * Performs an ES indexing on the given document and his children.
     *
     * @param documentId the id of the document that will be indexed and his children recursively
     * @see #performIndexing(String)
     */
    @POST
    @Path("{documentId}/reindex")
    public BulkStatus doIndexingOnDocument(@PathParam("documentId") String documentId,
            @QueryParam("queryLimit") @DefaultValue("-1") Long queryLimit) {
        String query = String.format("Select * From Document where %s = '%s' or %s = '%s'", //
                NXQL.ECM_UUID, documentId, //
                NXQL.ECM_ANCESTORID, documentId);

        return performIndexing(query, queryLimit);
    }

    /**
     * Executes an ES flush on document index of a given repository.
     */
    @POST
    @Path("flush")
    public void doFlush() {
        Framework.doPrivileged(() -> Framework.getService(ElasticSearchAdmin.class)
                                              .flushRepositoryIndex(ctx.getCoreSession().getRepositoryName()));
    }

    /**
     * Executes an ES optimize on document index of a given repository.
     */
    @POST
    @Path("optimize")
    public void doOptimize() {
        Framework.doPrivileged(() -> Framework.getService(ElasticSearchAdmin.class)
                                              .optimizeRepositoryIndex(ctx.getCoreSession().getRepositoryName()));
    }

    /**
     * Check discrepancies between elastic and repository search.
     *
     * @since 2023.7
     */
    @GET
    @Path("checkSearch")
    public String checkSearch(@QueryParam("nxql") String nxql, @QueryParam("pageSize") Long pageSize) {
        if (nxql == null || nxql.isBlank()) {
            nxql = DEFAULT_CHECK_SEARCH_NXQL;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_CHECK_SEARCH_PAGE_SIZE;
        }
        Map<String, Serializable> repoSearch = extractResultInfo("nxql_repo_search", nxql, pageSize);
        Map<String, Serializable> elasticSearch = extractResultInfo("nxql_elastic_search", nxql, pageSize);
        Map<String, Serializable> ret = new HashMap<>();
        ret.put("query", nxql);
        ret.put("order", repoSearch.get("order"));
        repoSearch.remove("order");
        elasticSearch.remove("order");
        ret.put("repo", (Serializable) repoSearch);
        ret.put("elastic", (Serializable) elasticSearch);
        try {
            return MAPPER.writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            throw new NuxeoException(e);
        }
    }

    protected Map<String, Serializable> extractResultInfo(String ppName, String nxql, long pageSize) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(ppName);
        HashMap<String, Serializable> params = new HashMap<>();
        params.put(CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());
        PageProvider<?> pp = pageProviderService.getPageProvider(ppName, ppdef, null, null, pageSize, 0L, params);
        String[] patternParams = { nxql };
        pp.setParameters(patternParams);
        long start = System.currentTimeMillis();
        List<DocumentModel> res = (List<DocumentModel>) pp.getCurrentPage();
        long duration = System.currentTimeMillis() - start;
        Map<String, Serializable> ret = new HashMap<>();
        ret.put("pageProvider", ppName);
        ret.put("pageSize", pageSize);
        ret.put("took", duration);
        ret.put("resultsCount", pp.getResultsCount());
        ret.put("resultsCountLimit", pp.getResultsCountLimit());
        ret.put("order", pp.getSortInfo());
        ret.put("results", (Serializable) res.stream().map(DocumentModel::getId).collect(Collectors.toList()));
        return ret;
    }

    /**
     * @deprecated since 2023.36 use {@link #performIndexing(String, long)} instead
     */
    @Deprecated(since = "2023.36", forRemoval = true)
    protected BulkStatus performIndexing(String query) {
        return performIndexing(query, -1);
    }

    /**
     * Performs an ES indexing on documents matching the optional NXQL query.
     *
     * @param query the NXQL query that documents must match to be indexed, can be {@code null} or {@code empty}, in
     *            this case all documents of the given repository will be indexed {@link #GET_ALL_DOCUMENTS_QUERY}
     * @param queryLimit the limit of documents to index, {@code -1} for no limit, not taken into account for full
     *            reindex
     * @since 2023.36
     * @return the {@link BulkStatus} of the ES indexing
     */
    protected BulkStatus performIndexing(String query, long queryLimit) {
        boolean fullReindex = StringUtils.isBlank(query);
        String nxql = StringUtils.defaultIfBlank(query, GET_ALL_DOCUMENTS_QUERY);
        String repository = ctx.getCoreSession().getRepositoryName();
        BulkService bulkService = Framework.getService(BulkService.class);
        try {
            var builder = new BulkCommand.Builder(ACTION_NAME, nxql, SYSTEM_USERNAME).repository(repository)
                                                                                     .setExclusive(fullReindex)
                                                                                     .param(INDEX_UPDATE_ALIAS_PARAM,
                                                                                             true);
            if (queryLimit <= 0 || fullReindex) {
                builder.queryUnlimited();
            } else {
                builder.queryLimit(queryLimit);
            }
            String commandId = bulkService.submit(builder.build());
            if (fullReindex) {
                ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
                esa.initRepositoryIndexWithAliases(repository);
                log.warn("Submitted index command: {} to index the entire {} repository.", commandId, repository);
            }
            return bulkService.getStatus(commandId);
        } catch (IllegalStateException e) {
            throw new ConcurrentUpdateException(e.getMessage(), e);
        }
    }
}
