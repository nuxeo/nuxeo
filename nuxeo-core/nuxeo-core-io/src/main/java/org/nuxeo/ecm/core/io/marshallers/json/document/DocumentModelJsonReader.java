/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
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

/**
 * Convert Json as {@link DocumentModel}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type": "document",
 *   "uid": "EXISTING_DOCUMENT_UID", <- use it to update an existing document
 *   "repository": "REPOSITORY_NAME" , <- explicitely specify the repository name
 *   "name": "DOCUMENT_NAME", <- use it to create an new document
 *   "type": "DOCUMENT_TYPE", <- use it to create an new document
 *   "properties": ...  <-- see {@link DocumentPropertiesJsonReader}
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelJsonReader extends EntityJsonReader<DocumentModel> {

    public static final String LEGACY_MODE_READER = "DocumentModelLegacyModeReader";

    public DocumentModelJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    public DocumentModel read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in) throws IOException {
        Reader<DocumentModel> reader = ctx.getParameter(LEGACY_MODE_READER);
        if (reader != null) {
            DocumentModel doc = reader.read(clazz, genericType, mediaType, in);
            return doc;
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
                simpleDoc.setPropertyValue(property.getName(), property.getValue());
            }
        }

        DocumentModel doc = null;

        String uid = getStringField(jn, "uid");
        if (StringUtils.isNotBlank(uid)) {
            try (SessionWrapper wrapper = ctx.getSession(null)) {
                doc = wrapper.getSession().getDocument(new IdRef(uid));
            }
            applyPropertyValues(simpleDoc, doc);
        } else {
            doc = simpleDoc;
        }

        return doc;
    }

    public static void applyPropertyValues(DocumentModel src, DocumentModel dst) throws ClientException {
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
                } catch (PropertyNotFoundException e) {
                    continue;
                }
            }
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
