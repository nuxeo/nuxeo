/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.routing.io;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Button;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.webengine.app.DefaultContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebContext;
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

    @Override
    protected void writeEntityBody(JsonGenerator jg, Task item) throws IOException, ClientException {
        writeTask(jg, item, request);
    }

    public static void writeTask(JsonGenerator jg, Task item, HttpServletRequest request) throws JsonGenerationException, IOException {
        jg.writeStringField("id", item.getDocument().getId());
        jg.writeStringField("name", item.getName());
        jg.writeStringField("workflowId", item.getProcessId());
        jg.writeStringField("state", item.getDocument().getCurrentLifeCycleState());
        jg.writeStringField("directive", item.getDirective());
        jg.writeStringField("created", DateParser.formatW3CDateTime(item.getCreated()));
        jg.writeStringField("dueDate", DateParser.formatW3CDateTime(item.getDueDate()));
        jg.writeStringField("type", item.getType());
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
            jg.writeStringField("date", DateParser.formatW3CDateTime(comment.getCreationDate()));
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("variables");
        for (Entry<String, String> e : item.getVariables().entrySet()) {
            jg.writeStartObject();
            jg.writeStringField("key", e.getKey());
            jg.writeObjectField("value", e.getValue());
            jg.writeEndObject();
        }
        jg.writeEndArray();

        if (request != null) {
            CoreSession session = SessionFactory.getSession(request);
            jg.writeFieldName("taskInfo");
            jg.writeStartObject();
            DocumentModel doc = session.getDocument(new IdRef(item.getProcessId()));
            GraphRoute route = doc.getAdapter(GraphRoute.class);
            GraphNode node = route.getNode(item.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY));

            final ActionManager actionManager = Framework.getService(ActionManager.class);
            jg.writeArrayFieldStart("actions");
            for (Button button : node.getTaskButtons()) {
                if (StringUtils.isBlank(button.getFilter()) || actionManager.checkFilter(button.getFilter(), createActionContext(session))) {
                    jg.writeStartObject();
                    jg.writeStringField("url", button.getName());
                    jg.writeStringField("label", button.getLabel());
                    jg.writeEndObject();
                }
            }
            jg.writeEndArray();

            jg.writeEndObject();

            jg.writeStringField("layoutResource", node.getTaskLayout());
        }

    }

    protected static String getURL(HttpServletRequest request) {
        DefaultContext ctx = (DefaultContext) request.getAttribute(WebContext.class.getName());
        return ctx.getServerURL().toString();
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
