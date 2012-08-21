/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.api.operation;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.propertiesmapping.PropertiesMappingService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Applies the mapping passed as parameter on the input task document. The
 * sourceDoc in the mapping is the input document in the workflow.
 *
 * @since 5.6
 *
 */
@Operation(id = MapPropertiesOnTaskOperation.ID, category = Constants.CAT_WORKFLOW, label = "Apply mapping on input task doc", requires = Constants.WORKFLOW_CONTEXT, description = "Applies the mapping passed in parameter on the task document. "
        + "The sourceDoc in the mapping is the input document in the workflow. The operation throws a ClientException if the input document is not a Task.")
public class MapPropertiesOnTaskOperation {

    public static final String ID = "Context.ApplyMappingOnTask";

    private static Log log = LogFactory.getLog(MapPropertiesOnTaskOperation.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "mappingName", required = true)
    protected String mappingName;

    @Context
    protected DocumentRoutingService routing;

    @Context
    protected PropertiesMappingService mappingService;

    @OperationMethod
    public DocumentModel run(DocumentModel taskDoc) throws ClientException {
        Task task = taskDoc.getAdapter(Task.class);
        if (task == null) {
            throw new ClientException("Input document is not a Task");
        }
        List<DocumentModel> docs = routing.getWorkflowInputDocuments(session,
                task);
        if (docs.size() == 0) {
            throw new ClientException(
                    "Can not fetch the input documents in the related workflow instance");
        }
        if (docs.size() > 1) {
            log.warn("Using as mapping source only the first document in the input documents in the workflow");
        }
        mappingService.mapProperties(session, docs.get(0), taskDoc, mappingName);
        return taskDoc;
    }
}