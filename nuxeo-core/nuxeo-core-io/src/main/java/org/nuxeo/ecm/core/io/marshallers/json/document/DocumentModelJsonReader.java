/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convert Json as {@link DocumentModel}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type": "document",
 *   "uid": "EXISTING_DOCUMENT_UID", <- use it to update an existing document
 *   "name": "DOCUMENT_NAME", <- use it to create an new document
 *   "type": "DOCUMENT_TYPE", <- use it to create an new document
 *   "changeToken": "CHANGE_TOKEN", <- pass the previous change token for optimistic locking
 *   "properties": ...  <-- see {@link DocumentPropertiesJsonReader}
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelJsonReader extends EntityJsonReader<DocumentModel> {

    private static final Logger log = LogManager.getLogger(DocumentModelJsonReader.class);

    public static final String LEGACY_MODE_READER = "DocumentModelLegacyModeReader";

    public DocumentModelJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    public DocumentModel read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in)
            throws IOException {
        Reader<DocumentModel> reader = ctx.getParameter(LEGACY_MODE_READER);
        if (reader != null) {
            return reader.read(clazz, genericType, mediaType, in);
        } else {
            return super.read(clazz, genericType, mediaType, in);
        }
    }

    @Override
    protected DocumentModel readEntity(JsonNode jn) throws IOException {

        SimpleDocumentModel simpleDoc = new SimpleDocumentModel();
        String name = getStringField(jn, "name");
        if (StringUtils.isNotBlank(name)) {
            simpleDoc.setPathInfo(null, name);
        }
        String type = getStringField(jn, "type");
        if (StringUtils.isNotBlank(type)) {
            simpleDoc.setType(type);
        }

        JsonNode propsNode = jn.get("properties");
        if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
            ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
            List<Property> properties = readEntity(List.class, genericType, propsNode);
            for (Property property : properties) {
                String propertyName = property.getName();
                // handle schema with no prefix
                if (!propertyName.contains(":")) {
                    propertyName = property.getField().getDeclaringType().getName() + ":" + propertyName;
                }
                simpleDoc.setPropertyValue(propertyName, property.getValue());
            }
        }

        DocumentModel doc;

        String uid = getStringField(jn, "uid");
        if (StringUtils.isNotBlank(uid)) {
            try (SessionWrapper wrapper = ctx.getSession(null)) {
                doc = wrapper.getSession().getDocument(new IdRef(uid));
            }
            applyDirtyPropertyValues(simpleDoc, doc);
            String changeToken = getStringField(jn, "changeToken");
            doc.putContextData(CoreSession.CHANGE_TOKEN, changeToken);
        } else if (StringUtils.isNotBlank(type)) {
            SimpleDocumentModel createdDoc = new SimpleDocumentModel();
            if (StringUtils.isNotBlank(name)) {
                createdDoc.setPathInfo(null, name);
            }
            createdDoc.setType(simpleDoc.getType());
            applyAllPropertyValues(simpleDoc, createdDoc);
            doc = createdDoc;
        } else {
            doc = simpleDoc;
        }

        return doc;
    }

    public static void applyPropertyValues(DocumentModel src, DocumentModel dst) {
        applyPropertyValues(src, dst, true);
        // copy change token
        dst.getContextData().putAll(src.getContextData());
    }

    public static void applyPropertyValues(DocumentModel src, DocumentModel dst, boolean dirtyOnly) {
        // if not "dirty only", it handles all the schemas for the given type
        // so it will trigger the default values initialization
        if (dirtyOnly) {
            applyDirtyPropertyValues(src, dst);
        } else {
            applyAllPropertyValues(src, dst);
        }
    }

    public static void applyDirtyPropertyValues(DocumentModel src, DocumentModel dst) {
        String[] schemas = src.getSchemas();
        for (String schema : schemas) {
            DataModelImpl dstDataModel = (DataModelImpl) dst.getDataModel(schema);
            DataModelImpl srcDataModel = (DataModelImpl) src.getDataModel(schema);
            for (String field : srcDataModel.getDirtyFields()) {
                applyPropertyValue(srcDataModel, dstDataModel, field);
            }
        }
    }

    public static void applyAllPropertyValues(DocumentModel src, DocumentModel dst) {
        SchemaManager service = Framework.getService(SchemaManager.class);
        DocumentType type = service.getDocumentType(src.getType());
        String[] schemas = type.getSchemaNames();
        for (String schemaName : schemas) {
            Schema schema = service.getSchema(schemaName);
            DataModelImpl dstDataModel = (DataModelImpl) dst.getDataModel(schemaName);
            DataModelImpl srcDataModel = (DataModelImpl) src.getDataModel(schemaName);
            for (Field field : schema.getFields()) {
                String fieldName = field.getName().getLocalName();
                applyPropertyValue(srcDataModel, dstDataModel, fieldName);
            }
        }
    }

    protected static void applyPropertyValue(DataModelImpl srcDataModel, DataModelImpl dstDataModel, String fieldName) {
        Serializable data = (Serializable) srcDataModel.getData(fieldName);
        try {
            if (!(dstDataModel.getDocumentPart().get(fieldName) instanceof BlobProperty)) {
                dstDataModel.setData(fieldName, data);
            } else {
                dstDataModel.setData(fieldName, decodeBlob(data));
            }
        } catch (PropertyNotFoundException e) {
            log.trace("Can't apply value: {} to src: {}", data, srcDataModel, e);
        }
    }

    private static Serializable decodeBlob(Serializable data) {
        if (data instanceof Blob) {
            return data;
        } else {
            return null;
        }
    }

}
