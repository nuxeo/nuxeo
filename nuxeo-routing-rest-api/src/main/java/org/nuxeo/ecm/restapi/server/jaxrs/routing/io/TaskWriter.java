/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.routing.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Button;
import org.nuxeo.ecm.platform.routing.core.io.JsonEncodeDecodeUtils;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class TaskWriter extends EntityWriter<Task> {

    public static final String ENTITY_TYPE = "task";

    @Context
    HttpServletRequest request;

    @Context
    UriInfo uriInfo;

    @Override
    protected void writeEntityBody(JsonGenerator jg, Task item) throws IOException {
        writeTask(jg, item, request, uriInfo);
    }

    public static void writeTask(JsonGenerator jg, Task item, HttpServletRequest request, UriInfo uriInfo)
            throws JsonGenerationException, IOException {
        CoreSession session = null;
        GraphRoute workflowInstance = null;
        GraphNode node = null;
        String workflowInstanceId = item.getProcessId();
        final String nodeId = item.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (request != null) {
            session = SessionFactory.getSession(request);
        }
        if (session != null && StringUtils.isNotBlank(workflowInstanceId)) {
            NodeAccessRunner nodeAccessRunner = new NodeAccessRunner(session, workflowInstanceId, nodeId);
            nodeAccessRunner.runUnrestricted();
            workflowInstance = nodeAccessRunner.workflowInstance;
            node = nodeAccessRunner.node;
        }

        jg.writeStringField("id", item.getDocument().getId());
        jg.writeStringField("name", item.getName());
        jg.writeStringField("workflowInstanceId", workflowInstanceId);
        if (workflowInstance != null) {
            jg.writeStringField("workflowModelName", workflowInstance.getModelName());
        }
        jg.writeStringField("state", item.getDocument().getCurrentLifeCycleState());
        jg.writeStringField("directive", item.getDirective());
        jg.writeStringField("created", DateParser.formatW3CDateTime(item.getCreated()));
        jg.writeStringField("dueDate", DateParser.formatW3CDateTime(item.getDueDate()));
        jg.writeStringField("nodeName", item.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY));

        jg.writeArrayFieldStart("targetDocumentIds");
        for (String docId : item.getTargetDocumentsIds()) {
            jg.writeStartObject();
            jg.writeStringField("id", docId);
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("actors");
        for (String actorId : item.getActors()) {
            jg.writeStartObject();
            jg.writeStringField("id", actorId);
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("comments");
        for (TaskComment comment : item.getComments()) {
            jg.writeStartObject();
            jg.writeStringField("author", comment.getAuthor());
            jg.writeStringField("text", comment.getText());
            jg.writeStringField("date", DateParser.formatW3CDateTime(comment.getCreationDate().getTime()));
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeFieldName("variables");
        jg.writeStartObject();
        // add nodeVariables
        for (Entry<String, Serializable> e : node.getVariables().entrySet()) {
            JsonEncodeDecodeUtils.encodeVariableEntry(node.getDocument(), GraphNode.PROP_VARIABLES_FACET, e, jg,
                    request);
        }
        // add workflow variables
        if (workflowInstance != null) {
            final String transientSchemaName = DocumentRoutingConstants.GLOBAL_VAR_SCHEMA_PREFIX + node.getId();
            final SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            final Schema transientSchema = schemaManager.getSchema(transientSchemaName);
            for (Entry<String, Serializable> e : workflowInstance.getVariables().entrySet()) {
                if (transientSchema == null || transientSchema.hasField(e.getKey())) {
                    JsonEncodeDecodeUtils.encodeVariableEntry(workflowInstance.getDocument(),
                            GraphRoute.PROP_VARIABLES_FACET, e, jg, request);
                }
            }
        }
        jg.writeEndObject();

        if (session != null) {
            jg.writeFieldName("taskInfo");
            jg.writeStartObject();
            final ActionManager actionManager = Framework.getService(ActionManager.class);
            jg.writeArrayFieldStart("taskActions");
            for (Button button : node.getTaskButtons()) {
                if (StringUtils.isBlank(button.getFilter())
                        || actionManager.checkFilter(button.getFilter(), createActionContext(session))) {
                    jg.writeStartObject();
                    jg.writeStringField("name", button.getName());
                    jg.writeStringField("url", uriInfo.getBaseUri() + "api/v1/task/" + item.getDocument().getId() + "/"
                            + button.getName());
                    jg.writeStringField("label", button.getLabel());
                    jg.writeEndObject();
                }
            }
            jg.writeEndArray();

            jg.writeFieldName("layoutResource");
            jg.writeStartObject();
            jg.writeStringField("name", node.getTaskLayout());
            jg.writeStringField("url",
                    uriInfo.getBaseUri() + "/layout-manager/layouts/?layoutName=" + node.getTaskLayout());
            jg.writeEndObject();

            jg.writeArrayFieldStart("schemas");
            for (String schema : node.getDocument().getSchemas()) {
                // TODO only keep functional schema once adaptation done
                jg.writeStartObject();
                jg.writeStringField("name", schema);
                jg.writeStringField("url", uriInfo.getBaseUri() + "api/v1/config/schemas/" + schema);
                jg.writeEndObject();
            }
            jg.writeEndArray();

            jg.writeEndObject();
        }

    }

    protected static ActionContext createActionContext(CoreSession session) {
        ActionContext actionContext = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        actionContext.setDocumentManager(session);
        actionContext.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        return actionContext;
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
