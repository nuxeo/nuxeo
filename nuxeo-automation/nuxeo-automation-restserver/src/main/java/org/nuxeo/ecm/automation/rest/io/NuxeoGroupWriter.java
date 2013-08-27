/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.rest.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;

/**
 *
 *
 * @since 5.7.3
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoGroupWriter implements MessageBodyWriter<NuxeoGroup> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return NuxeoGroup.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(NuxeoGroup t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(NuxeoGroup group, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        try {
            writeGroup(JsonWriter.createGenerator(entityStream), group);
        } catch (ClientException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * @param createGenerator
     * @param group
     * @throws IOException
     * @throws JsonGenerationException
     *
     */
    private void writeGroup(JsonGenerator jg, NuxeoGroup group) throws ClientException, JsonGenerationException, IOException{
        jg.writeStartObject();
        jg.writeStringField("entity-type", "group");
        jg.writeStringField("groupname", group.getName());

        jg.writeStringField("label", group.getLabel());

        jg.writeArrayFieldStart("memberUsers");
        for(String user : group.getMemberUsers()) {
            jg.writeString(user);
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("memberGroups");
        for(String user : group.getMemberGroups()) {
            jg.writeString(user);
        }
        jg.writeEndArray();



        jg.writeEndObject();

        jg.flush();
    }

}
