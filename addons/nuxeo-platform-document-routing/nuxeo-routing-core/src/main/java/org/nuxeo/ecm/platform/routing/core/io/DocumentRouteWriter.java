/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentRouteWriter extends ExtensibleEntityJsonWriter<DocumentRoute> {

    public static final String ATTACHED_DOCUMENT_IDS = "attachedDocumentIds";

    public static final String FETCH_ATTACHED_DOCUMENTS = ATTACHED_DOCUMENT_IDS;

    public static final String ENTITY_TYPE = "workflow";

    public static final String FETCH_INITATIOR = "initiator";

    @Inject
    UserManager userManager;

    @Inject
    private SchemaManager schemaManager;

    public DocumentRouteWriter() {
        super(ENTITY_TYPE, DocumentRoute.class);
    }

    @Override
    protected void writeEntityBody(DocumentRoute item, JsonGenerator jg) throws IOException {
        jg.writeStringField("id", item.getDocument().getId());
        jg.writeStringField("name", item.getName());
        jg.writeStringField("title", item.getTitle());
        jg.writeStringField("state", item.getDocument().getCurrentLifeCycleState());
        jg.writeStringField("workflowModelName", item.getModelName());
        if (ctx.getFetched(ENTITY_TYPE).contains(FETCH_INITATIOR)) {
            NuxeoPrincipal principal = userManager.getPrincipal(item.getInitiator());
            if (principal != null) {
                writeEntityField("initiator", principal, jg);
            } else {
                jg.writeStringField("initiator", item.getInitiator());
            }
        } else {
            jg.writeStringField("initiator", item.getInitiator());
        }

        jg.writeArrayFieldStart(ATTACHED_DOCUMENT_IDS);
        try (SessionWrapper wrapper = ctx.getSession(item.getDocument())) {
            final boolean isFetchAttachedDocumentIds = ctx.getFetched(ENTITY_TYPE).contains(FETCH_ATTACHED_DOCUMENTS);
            for (String docId : item.getAttachedDocuments()) {
                if (isFetchAttachedDocumentIds) {
                    IdRef idRef = new IdRef(docId);
                    if (wrapper.getSession().exists(idRef)) {
                        writeEntity(wrapper.getSession().getDocument(idRef), jg);
                        break;
                    }
                }
                jg.writeStartObject();
                jg.writeStringField("id", docId);
                jg.writeEndObject();
            }
        }
        jg.writeEndArray();

        if (item instanceof GraphRoute) {
            jg.writeFieldName("variables");
            jg.writeStartObject();

            writeVariables(item, jg, registry, ctx, schemaManager);

            jg.writeEndObject();
            String graphResourceUrl = getGraphResourceURL(item, ctx);
            jg.writeStringField("graphResource", graphResourceUrl);
        }
    }

    /**
     * @since 8.3
     */
    public static void writeVariables(DocumentRoute item, JsonGenerator jg, MarshallerRegistry registry,
            RenderingContext ctx, SchemaManager schemaManager) throws IOException, JsonGenerationException {
        String facet = (String) item.getDocument().getPropertyValue(GraphRoute.PROP_VARIABLES_FACET);
        if (StringUtils.isNotBlank(facet)) {

            CompositeType type = schemaManager.getFacet(facet);
            if (type != null) {
                boolean hasFacet = item.getDocument().hasFacet(facet);

                Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
                // provides the current route to the property marshaller
                try (Closeable resource = ctx.wrap()
                                             .with(DocumentModelJsonWriter.ENTITY_TYPE, item.getDocument())
                                             .open()) {
                    for (Field f : type.getFields()) {
                        String name = f.getName().getLocalName();
                        Property property = hasFacet ? item.getDocument().getProperty(name) : null;
                        OutputStream out = new OutputStreamWithJsonWriter(jg);
                        jg.writeFieldName(name);
                        propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
                    }
                }
            }
        }
    }

    /**
     * @since 10.1
     */
    public static String getGraphResourceURL(DocumentRoute route, RenderingContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.getBaseUrl());
        sb.append("api/v1/");
        if (route.isValidated()) {
            // it is a model
            sb.append("workflowModel/").append(route.getDocument().getName());
        } else {
            // it is an instance
            sb.append("workflow/").append(route.getDocument().getId());
        }
        sb.append("/graph");
        return sb.toString();
    }
}
