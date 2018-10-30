/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.automation.elasticsearch;

import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.bulk.IndexAction;

/**
 * Run Elasticsearch indexing operation using the Bulk Service
 *
 * @since 10.3
 */
@Operation(id = ElasticsearchBulkIndexOperation.ID, category = Constants.CAT_SERVICES, label = "Elasticsearch Indexing", since = "10.3", description = "Enable to index Nuxeo documents using the Bulk Service.")
public class ElasticsearchBulkIndexOperation {
    private static final Log log = LogFactory.getLog(ElasticsearchBulkIndexOperation.class);

    public static final String ID = "Elasticsearch.BulkIndex";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected ElasticSearchAdmin esa;

    @Context
    protected BulkService bulkService;

    @OperationMethod
    public Blob run() throws IOException {
        checkAccess();
        esa.dropAndInitRepositoryIndex(session.getRepositoryName(), false);
        String commandId = submitBulkCommand("SELECT ecm:uuid FROM Document", true);
        log.warn(String.format("Submitted index command: %s to index the entire %s repository.", commandId,
                session.getRepositoryName()));
        return Blobs.createJSONBlobFromValue(Collections.singletonMap("commandId", commandId));
    }

    protected String submitBulkCommand(String nxql, boolean syncAlias) {
        String username = session.getPrincipal().getName();
        return bulkService.submit(
                new BulkCommand.Builder(IndexAction.ACTION_NAME, nxql).param(INDEX_UPDATE_ALIAS_PARAM, syncAlias)
                                                                      .user(username)
                                                                      .build());
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
        String commandId = submitBulkCommand(nxql, false);
        return Blobs.createJSONBlobFromValue(Collections.singletonMap("commandId", commandId));
    }

}
