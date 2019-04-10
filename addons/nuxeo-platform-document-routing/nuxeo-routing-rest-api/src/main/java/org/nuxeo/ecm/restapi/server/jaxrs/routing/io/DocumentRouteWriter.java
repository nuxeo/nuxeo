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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.routing.core.io.JsonEncodeDecodeUtils;

/**
 * @since 7.2
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class DocumentRouteWriter extends EntityWriter<DocumentRoute> {

    public static final String ENTITY_TYPE = "workflow";

    @Context
    HttpServletRequest request;

    @Context
    UriInfo uriInfo;

    @Override
    protected void writeEntityBody(JsonGenerator jg, DocumentRoute item) throws IOException {
        writeDocumentRoute(jg, item, request, uriInfo);
    }

    public static void writeDocumentRoute(JsonGenerator jg, DocumentRoute item, HttpServletRequest request,
            UriInfo uriInfo) throws JsonGenerationException, IOException {
        jg.writeStringField("id", item.getDocument().getId());
        jg.writeStringField("name", item.getName());
        jg.writeStringField("title", item.getTitle());
        jg.writeStringField("state", item.getDocument().getCurrentLifeCycleState());
        jg.writeStringField("workflowModelName", item.getModelName());
        jg.writeStringField("initiator", item.getInitiator());

        jg.writeArrayFieldStart("attachedDocumentIds");
        for (String docId : item.getAttachedDocuments()) {
            jg.writeStartObject();
            jg.writeStringField("id", docId);
            jg.writeEndObject();
        }
        jg.writeEndArray();

        if (item instanceof GraphRoute) {
            GraphRoute graphRoute = (GraphRoute) item;
            jg.writeFieldName("variables");
            jg.writeStartObject();
            for (Entry<String, Serializable> e : graphRoute.getVariables().entrySet()) {
                JsonEncodeDecodeUtils.encodeVariableEntry(item.getDocument(), GraphRoute.PROP_VARIABLES_FACET, e, jg,
                        request);
            }
            jg.writeEndObject();
            String graphResourceUrl = "";
            if (item.isValidated()) {
                // it is a model
                graphResourceUrl = uriInfo.getBaseUri() + "api/v1/workflowModel/" + item.getDocument().getName()
                        + "/graph";
            } else {
                // it is an instance
                graphResourceUrl = uriInfo.getBaseUri() + "api/v1/workflow/" + item.getDocument().getId() + "/graph";
            }
            jg.writeStringField("graphResource", graphResourceUrl);
        }
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
