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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.task.Task;

/**
 * @since 7.1
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class TaskListWriter extends EntityListWriter<Task> {

    private static final Log log = LogFactory.getLog(DocumentRouteListWriter.class);

    @Context
    JsonFactory factory;

    @Context
    protected HttpHeaders headers;

    @Context
    protected HttpServletRequest request;

    @Override
    protected String getEntityType() {
        return "tasks";
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        if (!List.class.isAssignableFrom(arg0) && !(arg1 instanceof ParameterizedType)) {
            return false;
        }
        Class<?> o = (Class<?>) ((ParameterizedType) arg1).getActualTypeArguments()[0];
        return Task.class.isAssignableFrom(o);
    }

    @Override
    protected void writeItem(JsonGenerator jg, Task item) throws ClientException, IOException {
     // do nothing, everything is done in #writeTo
    }

    @Override
    public void writeTo(List<Task> tasks, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try {
            JsonGenerator jg = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeStringField("entity-type", "tasks");
            jg.writeArrayFieldStart("entries");
            for (Task docRoute : tasks) {
                jg.writeStartObject();
                jg.writeStringField("entity-type", "task");
                TaskWriter.writeTask(jg, docRoute);
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
            jg.flush();
        } catch (IOException e) {
            log.error("Failed to serialize task list", e);
            throw new WebApplicationException(500);
        }
    }
}
