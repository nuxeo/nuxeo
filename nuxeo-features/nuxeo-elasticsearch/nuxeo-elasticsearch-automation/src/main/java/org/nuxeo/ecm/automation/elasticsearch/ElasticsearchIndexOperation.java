/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.automation.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

import java.util.Arrays;

/**
 * Run Elasticsearch indexing operation
 *
 * @since 8.1
 */
@Operation(id = ElasticsearchIndexOperation.ID, category = Constants.CAT_SERVICES, label = "Elasticsearch Indexing", since = "8.1",
        description = "Enable to index Nuxeo documents.")
public class ElasticsearchIndexOperation {

    public static final String ID = "Elasticsearch.Index";

    private static final Log log = LogFactory.getLog(Log.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected ElasticSearchIndexing esi;

    @Context
    protected ElasticSearchAdmin esa;

    @Context
    protected CoreSession repo;

    @OperationMethod
    public void run() {
        checkAccess();
        esa.dropAndInitRepositoryIndex(repo.getRepositoryName());
        run("SELECT ecm:uuid FROM Document");
    }

    private void checkAccess() {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        if (principal == null || ! principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }

    @OperationMethod
    public void run(String nxql) {
        checkAccess();
        esi.runReindexingWorker(repo.getRepositoryName(), nxql);
    }

    @OperationMethod
    public void run(DocumentModel doc) {
        checkAccess();
        // 1. delete existing index
        IndexingCommand cmd = new IndexingCommand(doc, IndexingCommand.Type.DELETE, false, true);
        esi.runIndexingWorker(Arrays.asList(cmd));
        // 2. index recursive from path
        cmd = new IndexingCommand(doc, IndexingCommand.Type.INSERT, false, true);
        esi.runIndexingWorker(Arrays.asList(cmd));
    }

}
