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
import java.io.Serializable;
import java.util.Map.Entry;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;

/**
 * @since 7.2
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class WorkflowWriter extends EntityWriter<DocumentRoute> {

    public static final String ENTITY_TYPE = "workflow";

    @Override
    protected void writeEntityBody(JsonGenerator jg, DocumentRoute item) throws IOException, ClientException {
        writeDocumentRoute(jg, item);
    }

    public static void writeDocumentRoute(JsonGenerator jg, DocumentRoute item) throws JsonGenerationException, IOException {
        jg.writeStringField("id", item.getDocument().getId());
        jg.writeStringField("name", item.getName());
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
            jg.writeArrayFieldStart("variables");
            for (Entry<String, Serializable> e : graphRoute.getVariables().entrySet()) {
                jg.writeStartObject();
                jg.writeStringField("key", e.getKey());
                jg.writeObjectField("value", e.getValue());
                jg.writeEndObject();
            }
            jg.writeEndArray();
        }
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
