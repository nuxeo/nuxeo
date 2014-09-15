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
package org.nuxeo.ecm.restapi.jaxrs.io.usermanager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
public class NuxeoGroupReader implements MessageBodyReader<NuxeoGroup> {

    @Context
    JsonFactory factory;


    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return NuxeoGroup.class.isAssignableFrom(type);
    }

    @Override
    public NuxeoGroup readFrom(Class<NuxeoGroup> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String content = IOUtils.toString(entityStream);
        if (content.isEmpty()) {
            throw new WebException("No content in request body", Response.Status.BAD_REQUEST.getStatusCode());
        }

        return readRequest(content, httpHeaders);

    }

    /**
     * @param content
     * @param httpHeaders
     * @return
     *
     */
    private NuxeoGroup readRequest(String json,
            MultivaluedMap<String, String> httpHeaders) {
        try {
            JsonParser jp = factory.createJsonParser(
                    json);
            return readJson(jp, httpHeaders);
        } catch (ClientException | IOException e) {
            throw new WebApplicationException(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param jp
     * @param httpHeaders
     * @return
     * @throws IOException
     * @throws JsonParseException
     * @throws ClientException
     *
     */
    private NuxeoGroup readJson(JsonParser jp,
            MultivaluedMap<String, String> httpHeaders)
            throws JsonParseException, IOException, ClientException {
        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        String id = null;

        UserManager um = Framework.getLocalService(UserManager.class);
        NuxeoGroup group = null;

        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("groupname".equals(key)) {
                id = jp.readValueAs(String.class);

                group = um.getGroup(id);
                if (group == null) {
                    group = new NuxeoGroupImpl(id);
                }
            } else if ("grouplabel".equals(key)) {
                group.setLabel(jp.readValueAs(String.class));
            } else if ("memberUsers".equals(key)) {
                tok = jp.nextToken();
                List<String> users = new ArrayList<>();
                while (tok != JsonToken.END_ARRAY) {
                    users.add(jp.readValueAs(String.class));
                    tok = jp.nextToken();
                }
                group.setMemberUsers(users);
            } else if ("memberGroups".equals(key)) {
                tok = jp.nextToken();
                List<String> groups = new ArrayList<>();
                while (tok != JsonToken.END_ARRAY) {
                    groups.add(jp.readValueAs(String.class));
                    tok = jp.nextToken();
                }
                group.setMemberGroups(groups);
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!NuxeoGroupWriter.ENTITY_TYPE.equals(entityType)) {
                    throw new WebApplicationException(
                            Response.Status.BAD_REQUEST);
                }
            }
            tok = jp.nextToken();
        }
        return group;

    }

}
