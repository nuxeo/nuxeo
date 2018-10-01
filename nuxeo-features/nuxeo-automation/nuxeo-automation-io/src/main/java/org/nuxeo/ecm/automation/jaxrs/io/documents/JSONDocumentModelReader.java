/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.DocumentModelJsonReaderLegacy;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JAX-RS reader for a DocumentModel. If an id is given, it tries to reattach the document to the session. If not, it
 * creates a ready to create DocumentModel filled with the properties found.
 *
 * @since 5.7.2
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             {@link DocumentModelJsonReader} which is registered by default and available to marshal
 *             {@link DocumentModel} from the Nuxeo Rest API thanks to the JAX-RS marshaller {@link JsonCoreIODelegate}
 *             . On removal, need to remove also {@link DocumentModelJsonReaderLegacy} because it uses it using
 *             reflexion.
 */
@Deprecated
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONDocumentModelReader implements MessageBodyReader<DocumentModel> {

    // private static final String REQUEST_BATCH_ID = "batchId";

    protected static final Log log = LogFactory.getLog(JSONDocumentModelReader.class);

    @Context
    HttpServletRequest request;

    @Context
    JsonFactory factory;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DocumentModel.class.isAssignableFrom(type);
    }

    @Override
    public DocumentModel readFrom(Class<DocumentModel> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        String content = IOUtils.toString(entityStream);
        if (content.isEmpty()) {
            throw new NuxeoException("No content in request body", SC_BAD_REQUEST);

        }

        try {
            return readRequest(content, httpHeaders);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    private DocumentModel readRequest(String content, MultivaluedMap<String, String> httpHeaders) throws IOException {
        return readRequest(content, httpHeaders, request);
    }

    protected DocumentModel readRequest(String content, MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws IOException {
        JsonParser jp = factory.createJsonParser(content);
        return readJson(jp, httpHeaders, request);
    }

    public static DocumentModel readJson(JsonParser jp, MultivaluedMap<String, String> httpHeaders,
            HttpServletRequest request) throws IOException {
        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        SimpleDocumentModel simpleDoc = new SimpleDocumentModel();
        String type = null;
        String name = null;
        String uid = null;
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("properties".equals(key)) {
                DocumentHelper.setJSONProperties(null, simpleDoc, readProperties(jp));
            } else if ("name".equals(key)) {
                name = jp.readValueAs(String.class);
            } else if ("type".equals(key)) {
                type = jp.readValueAs(String.class);
            } else if ("uid".equals(key)) {
                uid = jp.readValueAs(String.class);
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!"document".equals(entityType)) {
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else {
                log.debug("Unknown key: " + key);
                jp.skipChildren();
            }

            tok = jp.nextToken();
        }

        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }

        if (StringUtils.isNotBlank(type)) {
            simpleDoc.setType(type);
        }

        if (StringUtils.isNotBlank(name)) {
            simpleDoc.setPathInfo(null, name);
        }

        // If a uid is specified, we try to get the doc from
        // the core session
        if (uid != null) {
            CoreSession session = SessionFactory.getSession(request);
            DocumentModel doc = session.getDocument(new IdRef(uid));
            applyPropertyValues(simpleDoc, doc);
            return doc;
        } else {
            return simpleDoc;
        }

    }

    static Properties readProperties(JsonParser jp) throws IOException {
        JsonNode node = jp.readValueAsTree();
        return new Properties(node);

    }

    /**
     * Decodes a Serializable to make it a blob.
     *
     * @since 5.9.1
     */
    private static Serializable decodeBlob(Serializable data) {
        if (data instanceof Blob) {
            return data;
        } else {
            return null;
        }
    }

    public static void applyPropertyValues(DocumentModel src, DocumentModel dst) {
        for (String schema : src.getSchemas()) {
            DataModelImpl dataModel = (DataModelImpl) dst.getDataModel(schema);
            DataModelImpl fromDataModel = (DataModelImpl) src.getDataModel(schema);

            for (String field : fromDataModel.getDirtyFields()) {
                Serializable data = (Serializable) fromDataModel.getData(field);
                try {
                    if (!(dataModel.getDocumentPart().get(field) instanceof BlobProperty)) {
                        dataModel.setData(field, data);
                    } else {
                        dataModel.setData(field, decodeBlob(data));
                    }
                    // }
                } catch (PropertyNotFoundException e) {
                    log.warn(String.format("Trying to deserialize unexistent field : {%s}", field));
                }
            }
        }
    }

}
