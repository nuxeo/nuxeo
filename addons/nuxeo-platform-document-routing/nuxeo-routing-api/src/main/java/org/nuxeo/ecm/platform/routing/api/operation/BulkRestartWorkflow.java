/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.routing.api.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Bulk operation to cancel and restart all the workflow instances of the workflow model with the id
 * <param>workflowId</param>. If the <param> nodeId</param> parameter is specified, then only the workflows suspened on
 * that node are restarted.
 *
 * @since 5.7
 */
@Operation(id = BulkRestartWorkflow.ID, category = Constants.CAT_WORKFLOW, label = "Bulk Restart Workflow", description = "Bulk operation to restart workflows.", aliases = { "BulkRestartWorkflow" })
public class BulkRestartWorkflow {

    public static final String ID = "WorkflowModel.BulkRestartInstances";

    private static final Log log = LogFactory.getLog(BulkRestartWorkflow.class);

    @Param(name = "workflowId", required = true)
    protected String workflowId;

    @Param(name = "nodeId", required = false)
    protected String nodeId;

    @Param(name = "reinitLifecycle", required = false)
    protected boolean reinitLifecycle;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize;

    @Context
    protected OperationContext ctx;

    public static final int DEFAULT_BATCH_SIZE = 1000;

    @OperationMethod
    public void run() {
        CloseableCoreSession session = null;
        boolean transactionStarted = false;
        Split split = SimonManager.getStopwatch(ID).start();
        try {
            session = CoreInstance.openCoreSession(null);

            // Fetching all routes
            // If the nodeId parameter is null, fetch all the workflow routes
            // with
            // the given workflowId
            String query = "Select %s from DocumentRoute where (ecm:name like '%s.%%' OR  ecm:name like '%s') and ecm:currentLifeCycleState = 'running'";
            String key = "ecm:uuid";
            if (StringUtils.isEmpty(nodeId)) {
                if (StringUtils.isEmpty(workflowId)) {
                    log.error("Need to specify either the workflowModelId either the nodeId to query the workflows");
                    return;
                }
                query = String.format(query, key, workflowId, workflowId);
            } else {
                query = "Select %s from RouteNode  where rnode:nodeId = '%s' and ecm:currentLifeCycleState = 'suspended'";
                key = "ecm:parentId";
                if (StringUtils.isEmpty(nodeId)) {
                    log.error("Need to specify either the workflowModelId either the nodeId to query the workflows");
                    return;
                }
                query = String.format(query, key, nodeId);
            }

            IterableQueryResult results = session.queryAndFetch(query, "NXQL");
            List<String> routeIds = new ArrayList<>();
            for (Map<String, Serializable> result : results) {
                routeIds.add(result.get(key).toString());
            }
            results.close();
            DocumentRoutingService routingService = Framework.getService(DocumentRoutingService.class);
            // Batching initialization
            if (batchSize == null) {
                batchSize = DEFAULT_BATCH_SIZE;
            }

            if (!TransactionHelper.isTransactionActive()) {
                TransactionHelper.startTransaction();
                transactionStarted = true;
            }
            long routesRestartedCount = 0;
            for (String routeId : routeIds) {
                try {
                    DocumentModel docRoute = session.getDocument(new IdRef(routeId));
                    DocumentRoute route = docRoute.getAdapter(DocumentRoute.class);
                    List<String> relatedDocIds = route.getAttachedDocuments();
                    route.cancel(session);

                    log.debug("Canceling workflow  " + route.getDocument().getName());

                    if (reinitLifecycle) {
                        reinitLifecycle(relatedDocIds, session);
                    }
                    routingService.createNewInstance(workflowId, relatedDocIds, session, true);
                    for (String string : relatedDocIds) {
                        log.debug("Starting workflow for " + string);
                    }
                    // removing old workflow instance
                    session.removeDocument(route.getDocument().getRef());

                    routesRestartedCount++;
                    if (routesRestartedCount % batchSize == 0) {
                        session.close();
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                        session = CoreInstance.openCoreSession(null);
                    }
                } catch (NuxeoException e) {
                    Throwable t = unwrapException(e);
                    log.error(t.getClass().getSimpleName() + ": " + t.getMessage());
                    log.error("Workflow with the docId '" + routeId + "' cannot be canceled. " + routesRestartedCount
                            + " workflows have been processed.");
                }
            }
        } finally {
            if (session != null) {
                session.close();
            }
            TransactionHelper.commitOrRollbackTransaction();
            if (!transactionStarted) {
                TransactionHelper.startTransaction();
            }
            split.stop();
            log.info(split.toString());
        }
    }

    public static Throwable unwrapException(Throwable t) {
        Throwable cause = null;
        if (t != null) {
            cause = t.getCause();
        }
        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    protected void reinitLifecycle(List<String> docIds, CoreSession session) {
        for (String docId : docIds) {
            session.reinitLifeCycleState(new IdRef(docId));
        }
    }

}
