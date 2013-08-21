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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

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

    private static final String REQUEST_BATCH_ID = "batchId";

    protected static final Log log = LogFactory.getLog(JSONDocumentModelReader.class);

    @Context
    HttpServletRequest request;

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
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        try {
            return readRequest(content, httpHeaders);
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
     *
     * @since TODO
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
    static protected DocumentModel readRequest(String content,
            MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws Exception {
        JsonParser jp = AutomationServerComponent.me.getFactory().createJsonParser(
                content);
        return readJson(jp, httpHeaders, request);
    }

    /**
     * @param jp
     * @param httpHeaders
     * @param request2
     * @return
     *
     * @since TODO
     */
    static DocumentModel readJson(JsonParser jp,
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
                readProperties(jp, tmp);
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
            }
            tok = jp.nextToken();
        }

        CoreSession session = SessionFactory.getSession(request);

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
                if(StringUtils.isNotBlank(name)) {
                    doc.setPathInfo(null, name);
                }
            } else {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }

        for (String schema : tmp.getSchemas()) {
            DataModelImpl dataModel = (DataModelImpl) doc.getDataModel(schema);
            DataModel fromDataModel = tmp.getDataModel(schema);

            for (String field : fromDataModel.getDirtyFields()) {
                Serializable data = (Serializable) fromDataModel.getData(field);
                try {

                    if (!(dataModel.getDocumentPart().get(field) instanceof BlobProperty)) {
                        if (data != null && !"null".equals(data)) {
                            dataModel.setData(field, data);
                        }
                    }
                } catch (PropertyNotFoundException e) {
                    log.warn(String.format(
                            "Trying to deserialize unexistent field : {%s}",
                            field));
                }
            }
        }

        // Get blob from batchId if X-Batch-Id in headers
        Blob blob = getRequestBlob(request);
        if (blob != null) {
            setBlobToDoc(doc, request, blob);
        }

        return doc;

    }

    private static void setBlobToDoc(DocumentModel doc,
            HttpServletRequest request, Blob blob) throws PropertyException,
            ClientException, PropertyNotFoundException {
        String xpath = request.getHeader("X-Blob-Property");
        if (xpath == null) {
            if (doc.hasSchema("file")) {
                xpath = "file:content";
            } else if (doc.hasSchema("files")) {
                xpath = "files:files";
            } else {
                throw new IllegalArgumentException(
                        "Missing request parameter named 'property' that specifies "
                                + "the blob property xpath to fetch");
            }
        }

        Property p = doc.getProperty(xpath);
        if (p.isList()) { // add the file to the list
            if ("files".equals(p.getSchema().getName())) { // treat the
                // files schema
                // separately
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                map.put("filename", blob.getFilename());
                map.put("file", (Serializable) blob);
                p.addValue(map);
            } else {
                p.addValue(blob);
            }
        } else {
            if ("file".equals(p.getSchema().getName())) { // for
                // compatibility
                // with deprecated
                // filename
                p.getParent().get("filename").setValue(blob.getFilename());
            }
            p.setValue(blob);
        }
    }

    private static Blob getRequestBlob(HttpServletRequest request) {
        String batchId = request.getHeader("X-Batch-Id");
        if (StringUtils.isNotBlank(batchId)) {
            final BatchManager bm = Framework.getLocalService(BatchManager.class);

            request.setAttribute(REQUEST_BATCH_ID, batchId);
            RequestContext.getActiveContext(request).addRequestCleanupHandler(
                    new RequestCleanupHandler() {
                        @Override
                        public void cleanup(HttpServletRequest req) {
                            String bid = (String) req.getAttribute(REQUEST_BATCH_ID);
                            bm.clean(bid);
                        }

                    });
            return bm.getBlob(batchId, "0");
        }
        return null;

    }

    protected static void readProperties(JsonParser jp, DocumentModel doc)
            throws Exception {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                doc.setPropertyValue(key, (Serializable) readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                doc.setPropertyValue(key, (Serializable) readObjectProperty(jp));
            } else if (tok == JsonToken.VALUE_NULL) {
                doc.setPropertyValue(key, (String) null);
            } else {
                doc.setPropertyValue(key, jp.getText());
            }
            tok = jp.nextToken();
        }
    }

    protected static Map<String, Serializable> readObjectProperty(JsonParser jp)
            throws Exception {
        Map<String, Serializable> map = new HashMap<>();
        readProperties(jp, map);
        return map;
    }

    /**
     * @param jp
     * @param hashMap
     * @throws Exception
     * @since TODO
     */
    protected static void readProperties(JsonParser jp,
            Map<String, Serializable> map) throws Exception {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                map.put(key, (Serializable) readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                map.put(key, (Serializable) readObjectProperty(jp));
            } else if (tok == JsonToken.VALUE_NULL) {
                map.put(key, (String) null);
            } else {
                map.put(key, jp.getText());
            }
            tok = jp.nextToken();
        }
    }

    protected static List<Serializable> readArrayProperty(JsonParser jp)
            throws Exception {
        List<Serializable> list = new ArrayList<>();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            if (tok == JsonToken.START_ARRAY) {
                list.add((Serializable) readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                list.add((Serializable) readObjectProperty(jp));
            } else {
                list.add(jp.getText());
            }
            tok = jp.nextToken();
        }
        return list;
    }

}
