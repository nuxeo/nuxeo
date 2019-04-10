/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.propertiesmapping.PropertiesMappingService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Applies the mapping passed as parameter on the input task document. The sourceDoc in the mapping is the input
 * document in the workflow.
 *
 * @since 5.6
 */
@Operation(id = MapPropertiesOnTaskOperation.ID, category = Constants.CAT_WORKFLOW, label = "Apply mapping on input task doc", requires = Constants.WORKFLOW_CONTEXT, description = "Applies the mapping passed in parameter on the task document. "
        + "The sourceDoc in the mapping is the input document in the workflow. The operation throws a NuxeoException if the input document is not a Task.", aliases = { "Context.ApplyMappingOnTask" })
public class MapPropertiesOnTaskOperation {

    public static final String ID = "Task.ApplyDocumentMapping";

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
    public DocumentModel run(DocumentModel taskDoc) {
        Task task = taskDoc.getAdapter(Task.class);
        if (task == null) {
            throw new NuxeoException("Input document is not a Task");
        }
        List<DocumentModel> docs = routing.getWorkflowInputDocuments(session, task);
        if (docs.size() == 0) {
            throw new NuxeoException("Can not fetch the input documents in the related workflow instance");
        }
        if (docs.size() > 1) {
            log.warn("Using as mapping source only the first document in the input documents in the workflow");
        }
        mappingService.mapProperties(session, docs.get(0), taskDoc, mappingName);
        return taskDoc;
    }
}
