/*
 * (C) Copyright 2018-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services.search;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.getFromClause;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.search.SearchIndexingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Run Search indexing operation
 *
 * @since 2025.0
 */
@Operation(id = SearchIndexOperation.ID, category = Constants.CAT_SERVICES, label = "Indexing", since = "2025.0", description = "Enable to index Nuxeo documents.", addToStudio = false)
public class SearchIndexOperation {

    private static final Logger log = LogManager.getLogger(SearchIndexOperation.class);

    public static final String ID = "Search.Index";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    /**
     * @since 2025.3
     */
    @OperationMethod
    public Blob run(DocumentModel doc) throws IOException {
        checkAccess();
        String commandId = submitBulkCommand("Select * From %s where %s = '%s' or %s = '%s'".formatted( //
                getFromClause(), //
                NXQL.ECM_UUID, doc.getId(), //
                NXQL.ECM_ANCESTORID, doc.getId()));
        log.warn("Submitted index command: {} to index document {}.", commandId, doc.getId());
        return Blobs.createJSONBlobFromValue(Map.of("commandId", commandId));
    }

    @OperationMethod
    public Blob run() throws IOException {
        checkAccess();
        String commandId = submitBulkCommand("SELECT ecm:uuid FROM " + getFromClause());
        log.warn("Submitted index command: {} to index the entire {} repository.", commandId,
                session.getRepositoryName());
        return Blobs.createJSONBlobFromValue(Map.of("commandId", commandId));
    }

    protected String submitBulkCommand(String nxql) {
        String repository = ctx.getCoreSession().getRepositoryName();
        SearchIndexingService service = Framework.getService(SearchIndexingService.class);
        try {
            return isBlank(nxql) ? service.reindexRepository(repository) : service.reindexDocuments(repository, nxql);
        } catch (IllegalStateException e) {
            throw new ConcurrentUpdateException(e.getMessage(), e);
        }
    }

    protected void checkAccess() {
        NuxeoPrincipal principal = ctx.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new NuxeoException("Unauthorized access: " + principal);
        }
    }

    @OperationMethod
    public Blob run(String nxql) throws IOException {
        checkAccess();
        String commandId = submitBulkCommand(nxql);
        log.warn("Submitted index command: {} to reindex: {}.", commandId, nxql);
        return Blobs.createJSONBlobFromValue(Map.of("commandId", commandId));
    }

}
