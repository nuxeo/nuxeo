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
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * JAX-RS reader for a DocumentModel. If an id is given, it tries to reattach
 * the document to the session. If not, it creates a ready to create
 * DocumentModel filled with the properties found.
 *
 * @since 5.7.2
 */
@Provider
@Consumes({ "application/json+nxentity", "application/json" })
public class JSONDocumentModelReader implements
        MessageBodyReader<DocumentModel> {

    // private static final String REQUEST_BATCH_ID = "batchId";

    protected static final Log log = LogFactory.getLog(JSONDocumentModelReader.class);

    @Context
    HttpServletRequest request;

    @Context
    JsonFactory factory;

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return DocumentModel.class.isAssignableFrom(type);
    }

    @Override
    public DocumentModel readFrom(Class<DocumentModel> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
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
            return readRequest(content, httpHeaders);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param content
     * @param httpHeaders
     * @return
     * @throws Exception
     *
     * @since 5.7.2
     */
    private DocumentModel readRequest(String content,
            MultivaluedMap<String, String> httpHeaders) throws Exception {
        return readRequest(content, httpHeaders, request);
    }

    /**
     * @param content
     * @param httpHeaders
     * @return
     * @throws Exception
     *
     * @since 5.7.2
     */
    protected DocumentModel readRequest(String content,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {
        JsonParser jp = factory.createJsonParser(content);
        return readJson(jp, httpHeaders, request);
    }

    /**
     * @param jp
     * @param httpHeaders
     * @param request2
     * @return
     *
     */
    public static DocumentModel readJson(JsonParser jp,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {
        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        DocumentModel tmp = new SimpleDocumentModel();
        String id = null;
        String type = null;
        String name = null;
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("uid".equals(key)) {
                id = jp.readValueAs(String.class);
            } else if ("properties".equals(key)) {
                Properties props = readProperties(jp);
                // Put null for CoreSession, only needed for ecm:acl... Won't be
                // supported for rest API
                DocumentHelper.setJSONProperties(null, tmp, props);
            } else if ("name".equals(key)) {
                name = jp.readValueAs(String.class);
            } else if ("type".equals(key)) {
                type = jp.readValueAs(String.class);
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!"document".equals(entityType)) {
                    throw new WebApplicationException(
                            Response.Status.BAD_REQUEST);
                }
            } else {
                log.debug("Unknown key: " + key);
                jp.skipChildren();
            }

            tok = jp.nextToken();
        }

        CoreSession session = SessionFactory.getSession(request);

        DocumentModel doc = getOrCreateDocumentModel(id, type, name, session);

        applyPropertyValues(tmp, doc);

        return doc;

    }

    private static void applyPropertyValues(DocumentModel src, DocumentModel dst)
            throws ClientException, PropertyException {
        for (String schema : src.getSchemas()) {
            DataModelImpl dataModel = (DataModelImpl) dst.getDataModel(schema);
            DataModel fromDataModel = src.getDataModel(schema);

            for (String field : fromDataModel.getDirtyFields()) {
                Serializable data = (Serializable) fromDataModel.getData(field);
                try {
                    if (isNotNull(data)) {

                        if (!(dataModel.getDocumentPart().get(field) instanceof BlobProperty)) {
                            dataModel.setData(field, data);
                        } else {
                            dataModel.setData(field, decodeBlob(data));
                        }
                    }
                } catch (PropertyNotFoundException e) {
                    log.warn(String.format(
                            "Trying to deserialize unexistent field : {%s}",
                            field));
                }
            }
        }
    }

    /**
     * Decodes a Serializable to make it a blob.
     * @param data
     * @return
     *
     * @since 5.9.1
     */
    private static Serializable decodeBlob(Serializable data) {
        if(data instanceof Blob) {
            return data;
        } else {
            return null;
        }
    }

    /**
     * Check that a serialized data is not null.
     * @param data
     * @return
     *
     * @since 5.9.1
     */
    private static boolean isNotNull(Serializable data) {
        return data != null && !"null".equals(data);
    }

    private static DocumentModel getOrCreateDocumentModel(String id,
            String type, String name, CoreSession session)
            throws ClientException {
        DocumentModel doc = null;
        if (StringUtils.isNotBlank(id)) {
            if (session.exists(new IdRef(id))) {
                doc = session.getDocument(new IdRef(id));
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } else {
            if (StringUtils.isNotBlank(type)) {
                doc = DocumentModelFactory.createDocumentModel(type);
                if (StringUtils.isNotBlank(name)) {
                    doc.setPathInfo(null, name);
                }
            } else {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }
        return doc;
    }

    static Properties readProperties(JsonParser jp) throws Exception {
        JsonNode node = jp.readValueAsTree();
        return new Properties(node);

    }

}
