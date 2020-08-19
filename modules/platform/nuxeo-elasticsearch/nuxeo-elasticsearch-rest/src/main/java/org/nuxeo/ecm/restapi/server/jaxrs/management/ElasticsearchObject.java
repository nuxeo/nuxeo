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
import static org.nuxeo.elasticsearch.bulk.IndexAction.ACTION_NAME;
import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Endpoint to manage Elasticsearch.
 *
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "elasticsearch")
@Produces(APPLICATION_JSON)
public class ElasticsearchObject extends AbstractResource<ResourceTypeImpl> {

    public static final String GET_ALL_DOCUMENTS_QUERY = "SELECT * from Document";

    /**
     * Performs an ES indexing on documents matching the optional NXQL query.
     *
     * @see #performIndexing(String)
     */
    @POST
    @Path("reindex")
    public BulkStatus doIndexing(@QueryParam("query") String query) {
        return performIndexing(query);
    }

    /**
     * Performs an ES indexing on the given document and his children.
     *
     * @param documentId the id of the document that will be indexed and his children recursively
     * @see #performIndexing(String)
     */
    @POST
    @Path("{documentId}/reindex")
    public BulkStatus doIndexingOnDocument(@PathParam("documentId") String documentId) {
        String query = String.format("Select * From Document where %s = '%s' or %s = '%s'", //
                NXQL.ECM_UUID, documentId, //
                NXQL.ECM_ANCESTORID, documentId);

        return performIndexing(query);
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
     * Performs an ES indexing on documents matching the optional NXQL query.
     *
     * @param query the NXQL query that documents must match to be indexed, can be {@code null} or {@code empty}, in
     *            this case all documents of the given repository will be indexed {@link #GET_ALL_DOCUMENTS_QUERY}
     * @return the {@link BulkStatus} of the ES indexing
     */
    protected BulkStatus performIndexing(String query) {
        String nxql = StringUtils.defaultIfBlank(query, GET_ALL_DOCUMENTS_QUERY);
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(
                new BulkCommand.Builder(ACTION_NAME, nxql, SYSTEM_USERNAME)
                                                                           .repository(ctx.getCoreSession()
                                                                                          .getRepositoryName())
                                                                           .param(INDEX_UPDATE_ALIAS_PARAM, true)
                                                                           .build());
        return bulkService.getStatus(commandId);
    }
}
