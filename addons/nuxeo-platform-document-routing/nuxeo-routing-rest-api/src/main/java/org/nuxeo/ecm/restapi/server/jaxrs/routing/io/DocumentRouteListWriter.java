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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;

/**
 * @since 7.2
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class DocumentRouteListWriter extends EntityListWriter<DocumentRoute> {

    private static final Log log = LogFactory.getLog(DocumentRouteListWriter.class);

    @Context
    JsonFactory factory;

    @Context
    protected HttpHeaders headers;

    @Context
    protected HttpServletRequest request;

    @Context
    UriInfo uriInfo;

    @Override
    protected String getEntityType() {
        return "workflows";
    }

    @Override
    protected void writeItem(JsonGenerator jg, DocumentRoute item) throws IOException {
        // do nothing, everything is done in #writeTo
    }

    @Override
    public void writeTo(List<DocumentRoute> docRoutes, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            JsonGenerator jg = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeStringField("entity-type", "worflows");
            jg.writeArrayFieldStart("entries");
            for (DocumentRoute docRoute : docRoutes) {
                jg.writeStartObject();
                jg.writeStringField("entity-type", "workflow");
                DocumentRouteWriter.writeDocumentRoute(jg, docRoute, request, uriInfo);
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
            jg.flush();
        } catch (IOException e) {
            log.error("Failed to serialize document route list", e);
            throw new WebApplicationException(500);
        }
    }
}
