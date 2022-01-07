/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.routing.core.io;

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader.DEFAULT_SCHEMA_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 8.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class TaskCompletionRequestJsonReader extends EntityJsonReader<TaskCompletionRequest> {

    public static final String ENTITY_TYPE = "task";

    private static final String USE_LEGACY_CONF_KEY = "nuxeo.document.routing.json.format.legacy";

    private Boolean useLegacy = null;

    @Inject
    SchemaManager schemaManager;

    public TaskCompletionRequestJsonReader() {
        super(TaskCompletionRequestJsonReader.ENTITY_TYPE);
        String prop = Framework.getProperty(USE_LEGACY_CONF_KEY);
        if (prop != null && Boolean.parseBoolean(prop)) {
            useLegacy = Boolean.TRUE;
        } else {
            useLegacy = Boolean.FALSE;
        }
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return !useLegacy && !ctx.getBooleanParameter(USE_LEGACY_CONF_KEY)
                && super.accept(clazz, genericType, mediatype);
    }

    @Override
    public TaskCompletionRequest readEntity(JsonNode jn) throws IOException {

        String comment = getStringField(jn, "comment");

        // get the task and workflow schema names
        Map<String, Serializable> variables = new HashMap<>();
        try (SessionWrapper closeable = ctx.getSession(null)) {
            CoreSession session = closeable.getSession();

            String taskId = getStringField(jn, "id");

            DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
            Task task = taskDoc.getAdapter(Task.class);
            String routeId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
            String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);

            NodeAccessRunner nar = new NodeAccessRunner(session, routeId, nodeId);
            nar.runUnrestricted();
            GraphRoute graphRoute = nar.getWorkflowInstance();
            GraphNode node = nar.getNode();

            // get the variables
            JsonNode variablesNode = jn.get("variables");
            if (variablesNode != null) {
                String workflowSchemaFacet = (String) graphRoute.getDocument()
                                                                .getPropertyValue(GraphRoute.PROP_VARIABLES_FACET);
                CompositeType type = schemaManager.getFacet(workflowSchemaFacet);
                variables.putAll(getVariables(variablesNode, type.getSchemas().stream().findFirst().get()));

                String nodeSchemaFacet = (String) node.getDocument().getPropertyValue(GraphNode.PROP_VARIABLES_FACET);
                type = schemaManager.getFacet(nodeSchemaFacet);
                variables.putAll(getVariables(variablesNode, type.getSchemas().stream().findFirst().get()));
            }

        }

        return new TaskCompletionRequest(comment, variables, false);
    }

    private Map<String, Serializable> getVariables(JsonNode variables, Schema schema) throws IOException {
        Map<String, Serializable> variable = new HashMap<>();
        String schemaName = schema.getNamespace() != null && schema.getNamespace().hasPrefix()
                ? schema.getNamespace().prefix
                : schema.getName();
        ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
        try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, schema.getName()).open()) {
            List<Property> properties = readEntity(List.class, genericType, variables);
            for (Property property : properties) {
                variable.put(property.getName().substring(schemaName.length() + 1), property.getValue());
            }
        }
        return variable;
    }

}
