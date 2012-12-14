/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.routing.api.operation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Bulk operation to cancel and restart all the workflow instances of the
 * workflow model with the id <param>workflowId</param>. If the <param>
 * nodeId</param> parameter is specified, then only the workflows suspened on
 * that node are restarted.
 *
 * @since 5.6
 */
@Operation(id = BulkRestartWorkflow.ID, category = Constants.CAT_WORKFLOW, label = "BulkRestartWorkflow", description = "Bulk operation to restart workflows.")
public class BulkRestartWorkflow {

    public static final String ID = "BulkRestartWorkflow";

    private static final Log log = LogFactory.getLog(BulkRestartWorkflow.class);

    @Context
    protected CoreSession session;

    @Param(name = "workflowId", required = true)
    protected String workflowId;

    @Param(name = "nodeId", required = false)
    protected String nodeId;

    @Param(name = "reinitLifecycle", required = false)
    protected boolean reinitLifecycle;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize;

    public static final int DEFAULT_BATCH_SIZE = 1000;

    @OperationMethod
    public void run() throws ClientException {
        Split split = SimonManager.getStopwatch(ID).start();
        // Fetching all routes
        // If the nodeId parameter is null, fetch all the workflow routes with
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
        List<String> routeIds = new ArrayList<String>();
        for (Map<String, Serializable> result : results) {
            routeIds.add(result.get(key).toString());
        }
        results.close();
        DocumentRoutingService routingService = Framework.getLocalService(DocumentRoutingService.class);
        // Batching initialization
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        boolean transactionStarted = false;
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
            transactionStarted = true;
        }
        try {
            long routesRestartedCount = 0;
            for (String routeId : routeIds) {
                try {
                    DocumentModel docRoute = session.getDocument(new IdRef(
                            routeId));
                    DocumentRoute route = docRoute.getAdapter(DocumentRoute.class);
                    List<String> relatedDocIds = route.getAttachedDocuments();
                    route.cancel(session);

                    log.debug("Canceling workflow  "
                            + route.getDocument().getName());

                    if (reinitLifecycle) {
                        reinitLifecycle(relatedDocIds, session);
                    }
                    routingService.createNewInstance(workflowId, relatedDocIds,
                            session, true);
                    for (String string : relatedDocIds) {
                        log.debug("Starting workflow for " + string);
                    }
                    // removing old workflow instance
                    session.removeDocument(route.getDocument().getRef());

                    routesRestartedCount++;
                    if (routesRestartedCount % batchSize == 0) {
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();
                    }
                } catch (Exception e) {
                    Throwable t = unwrapException(e);
                    log.error(t.getClass().getSimpleName() + ": "
                            + t.getMessage());
                    log.error("Workflow with the docId '" + routeId
                            + "' cannot be canceled. " + routesRestartedCount
                            + " workflows have been processed.");
                }
            }
        } finally {
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
        if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }
        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    protected void reinitLifecycle(List<String> docIds, CoreSession session)
            throws ClientException {
        for (String docId : docIds) {
            session.reinitLifeCycleState(new IdRef(docId));
        }
    }

}
