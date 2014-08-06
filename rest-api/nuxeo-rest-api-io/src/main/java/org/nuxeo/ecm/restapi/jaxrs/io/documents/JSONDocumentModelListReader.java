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
package org.nuxeo.ecm.restapi.jaxrs.io.documents;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.webengine.WebException;

/**
 *
 *
 * @since 5.7.3
 */
public class JSONDocumentModelListReader implements
        MessageBodyReader<DocumentModelList> {

    @Context
    private HttpServletRequest request;

    @Context
    JsonFactory factory;

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return DocumentModelList.class.isAssignableFrom(type);
    }

    @Override
    public DocumentModelList readFrom(Class<DocumentModelList> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String content = IOUtils.toString(entityStream);
        if (content.isEmpty()) {
            if (content.isEmpty()) {
                throw new WebException("No content in request body",
                        Response.Status.BAD_REQUEST.getStatusCode());
            }

        }

        try {
            return readRequest(content, httpHeaders, request);
        } catch (Exception e) {
            throw new WebApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param content
     * @param httpHeaders
     * @return
     * @throws Exception
     * @since 5.7.3
     */
    public DocumentModelList readRequest(String content,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {

        JsonParser jp = factory.createJsonParser(content);
        return readRequest(jp, httpHeaders, request);

    }

    /**
     * @param jp
     * @param httpHeaders
     * @param request2
     * @return
     * @throws Exception
     * @since TODO
     */
    public static DocumentModelList readRequest(JsonParser jp,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {
        DocumentModelList result = null;
        jp.nextToken(); // skip {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("entries".equals(key)) {
                result = readDocumentEntriesFromJson(jp, httpHeaders, request);
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!"documents".equals(entityType)) {
                    throw new WebApplicationException(
                            Response.Status.BAD_REQUEST);
                }
            }
            tok = jp.nextToken();
        }

        if (result == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } else {
            return result;
        }
    }

    /**
     * @param jp
     * @param httpHeaders
     * @param request
     * @return
     * @throws Exception
     * @since 5.7.3
     */
    private static DocumentModelList readDocumentEntriesFromJson(JsonParser jp,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {

        DocumentModelList entries = new DocumentModelListImpl();

        // Skip the start of the array

        while (jp.nextToken() == JsonToken.START_OBJECT) {

            DocumentModel doc = JSONDocumentModelReader.readJson(jp,
                    httpHeaders, request);
            entries.add(doc);
        }

        return entries;

    }

}
