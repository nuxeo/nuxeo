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
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

/**
 * Class that knows how to read NuxeoPrincipal from JSON
 *
 * @since 5.7.3
 */
@Provider
@Consumes({ "application/json+nxentity", "application/json" })
public class NuxeoPrincipalReader implements MessageBodyReader<NuxeoPrincipal> {

    protected static final Log log = LogFactory.getLog(NuxeoPrincipalReader.class);

    @Context
    JsonFactory factory;

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return NuxeoPrincipal.class.isAssignableFrom(type);
    }

    @Override
    public NuxeoPrincipal readFrom(Class<NuxeoPrincipal> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
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
     * @since 5.7.3
     */
    private NuxeoPrincipal readRequest(String content,
            MultivaluedMap<String, String> httpHeaders) {
        try {
            JsonParser jp = factory.createJsonParser(
                    content);
            return readJson(jp, httpHeaders);
        } catch (ClientException | IOException e) {
            throw new WebApplicationException(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param jp
     * @param httpHeaders
     * @param request2
     * @return
     * @throws IOException
     * @throws JsonParseException
     * @throws ClientException
     *
     * @since 5.7.3
     */
    static NuxeoPrincipal readJson(JsonParser jp,
            MultivaluedMap<String, String> httpHeaders)
            throws JsonParseException, IOException, ClientException {
        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        String id = null;

        UserManager um = Framework.getLocalService(UserManager.class);
        DocumentModel userDoc = null;
        String schema = um.getUserSchemaName();

        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("id".equals(key)) {
                id = jp.readValueAs(String.class);

                NuxeoPrincipal principal = um.getPrincipal(id);
                if (principal == null) {
                    userDoc = um.getBareUserModel();
                } else {
                    userDoc = principal.getModel();
                }
            } else if ("extendedGroups".equals(key)) {
                jp.readValueAsTree();
            } else if ("properties".equals(key)) {
                readProperties(jp, userDoc, schema);
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!"user".equals(entityType)) {
                    throw new WebApplicationException(
                            Response.Status.BAD_REQUEST);
                }
            }
            tok = jp.nextToken();
        }

        NuxeoPrincipal principal = new NuxeoPrincipalImpl(id);
        principal.setModel(userDoc);

        return principal;

    }

    protected static void readProperties(JsonParser jp, DocumentModel doc,
            String schemaName) throws PropertyException, ClientException,
            JsonParseException, IOException {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {

            String key = schemaName + ":" + jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                doc.setPropertyValue(key, (Serializable) readArrayProperty(jp));
            } else if (tok == JsonToken.VALUE_NULL) {
                doc.setPropertyValue(key, (String) null);
            } else {
                doc.setPropertyValue(key, jp.getText());
            }
            tok = jp.nextToken();
        }
    }

    protected static List<Serializable> readArrayProperty(JsonParser jp)
            throws JsonParseException, IOException {
        List<Serializable> list = new ArrayList<>();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            if (tok == JsonToken.START_ARRAY) {
                list.add((Serializable) readArrayProperty(jp));
            } else {
                list.add(jp.getText());
            }
            tok = jp.nextToken();
        }
        return list;
    }

}
