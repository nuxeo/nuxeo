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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WorkflowRequestJsonReader extends EntityJsonReader<WorkflowRequest> {

    protected static final Log log = LogFactory.getLog(WorkflowRequestJsonReader.class);

    public static final String ENTITY_TYPE = "workflow";

    private static final String USE_LEGACY_CONF_KEY = "nuxeo.document.routing.json.format.legacy";

    private Boolean useLegacy = null;

    @Inject
    SchemaManager schemaManager;

    @Inject
    DocumentRoutingService documentRoutingService;

    public WorkflowRequestJsonReader() {
        super(WorkflowRequestJsonReader.ENTITY_TYPE);
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
    protected WorkflowRequest readEntity(JsonNode jn) throws IOException {
        String workflowModelName = getStringField(jn, "workflowModelName");
        List<String> attachedDocumentIds = new ArrayList<String>();
        JsonNode attachedDocumentIdsNode = jn.get("attachedDocumentIds");
        if (attachedDocumentIdsNode != null) {
            for (Iterator<JsonNode> it = attachedDocumentIdsNode.getElements(); it.hasNext();) {
                attachedDocumentIds.add(it.next().getTextValue());
            }
        }
        Map<String, Serializable> variables = new HashMap<>();
        JsonNode variablesNode = jn.get("variables");
        if (variablesNode != null) {
            try (SessionWrapper closeable = ctx.getSession(null)) {
                CoreSession session = closeable.getSession();
                String workflowModelId = documentRoutingService.getRouteModelDocIdWithId(session, workflowModelName);
                DocumentModel model = session.getDocument(new IdRef(workflowModelId));
                String workflowSchemaFacet = (String) model.getPropertyValue(GraphRoute.PROP_VARIABLES_FACET);
                CompositeType type = schemaManager.getFacet(workflowSchemaFacet);
                String workflowSchema = type.getSchemaNames()[0];
                variables.putAll(getVariables(variablesNode, workflowSchema));
            }
        }

        return new WorkflowRequest(workflowModelName, attachedDocumentIds, variables);
    }

    private Map<String, Serializable> getVariables(JsonNode variables, String schemaName) throws IOException {
        Map<String, Serializable> variable = new HashMap<>();
        ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
        try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, schemaName).open()) {
            List<Property> properties = readEntity(List.class, genericType, variables);
            for (Property property : properties) {
                variable.put(property.getName().substring(schemaName.length() + 1), property.getValue());
            }
        }
        return variable;
    }

}
