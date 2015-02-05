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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

/**
 * Convert {@link Property} to Json.
 * <p>
 * Format is:
 *
 * <pre>
 * "stringPropertyValue"  <-- for string property, each property may be fetched if a resolver is associated with that property and if a parameter fetch:document=propXPath is present, in this case, an object will be marshalled as value
 * or
 * true|false  <- for boolean property
 * or
 * 123  <- for int property
 * ...
 * {  <- for complex property
 *   "subProperty": ...,
 *    ...
 * },
 * [ ... ] <- for list property
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertyJsonWriter extends AbstractJsonWriter<Property> {

    private static final Log log = LogFactory.getLog(DocumentPropertyJsonWriter.class);

    @Override
    public void write(Property prop, JsonGenerator jg) throws IOException {
        writeProperty(jg, prop);
        jg.flush();
    }

    protected void writeProperty(JsonGenerator jg, Property prop) throws IOException {
        if (prop.isScalar()) {
            writeScalarProperty(jg, prop);
        } else if (prop.isList()) {
            writeListProperty(jg, prop);
        } else {
            if (prop.isPhantom()) {
                jg.writeNull();
            } else if (prop instanceof BlobProperty) { // a blob
                writeBlobProperty(jg, prop);
            } else { // a complex property
                writeComplexProperty(jg, prop);
            }
        }
    }

    protected void writeScalarProperty(JsonGenerator jg, Property prop) throws IOException {
        Type type = prop.getType();
        Object value = prop.getValue();
        if (!fetchProperty(jg, prop)) {
            writeScalarPropertyValue(jg, type, value);
        }
    }

    private void writeScalarPropertyValue(JsonGenerator jg, Type type, Object value) throws IOException {
        if (value == null) {
            jg.writeNull();
        } else if (type instanceof BooleanType) {
            jg.writeBoolean((Boolean) value);
        } else if (type instanceof LongType) {
            jg.writeNumber((Long) value);
        } else if (type instanceof DoubleType) {
            jg.writeNumber((Double) value);
        } else if (type instanceof IntegerType) {
            jg.writeNumber((Integer) value);
        } else if (type instanceof BinaryType) {
            jg.writeBinary((byte[]) value);
        } else {
            jg.writeString(type.encode(value));
        }
    }

    protected boolean fetchProperty(JsonGenerator jg, Property prop) throws IOException {
        if (prop.getValue() == null) {
            return false;
        }
        boolean fetched = false;
        PropertyObjectResolver resolver = prop.getObjectResolver();
        if (resolver != null) {
            String propertyPath = prop.getPath().replaceFirst("/", "");
            String genericPropertyPath = propertyPath.replaceAll("\\[[0-9]*\\]", "");
            Set<String> fetchElements = ctx.getFetched(ENTITY_TYPE);
            boolean fetch = false;
            for (String fetchElement : fetchElements) {
                if ("properties".equals(fetchElement) || propertyPath.startsWith(fetchElement)
                        || genericPropertyPath.startsWith(fetchElement)) {
                    fetch = true;
                    break;
                }
            }
            if (fetch) {
                Object object = resolver.fetch();
                if (object != null) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        writeEntity(object, baos);
                        jg.writeRawValue(baos.toString());
                        fetched = true;
                    } catch (MarshallingException e) {
                        log.error("Unable to marshall as json the entity referenced by the property " + prop.getPath(),
                                e);
                    }
                }
            }
        }
        return fetched;
    }

    protected void writeListProperty(JsonGenerator jg, Property prop) throws IOException {
        jg.writeStartArray();
        if (prop instanceof ArrayProperty) {
            Object[] ar = (Object[]) prop.getValue();
            if (ar == null) {
                jg.writeEndArray();
                return;
            }
            Type type = ((ListType) prop.getType()).getFieldType();
            for (Object o : ar) {
                writeScalarPropertyValue(jg, type, o);
            }
        } else {
            ListProperty listp = (ListProperty) prop;
            for (Property p : listp.getChildren()) {
                writeProperty(jg, p);
            }
        }
        jg.writeEndArray();
    }

    protected void writeComplexProperty(JsonGenerator jg, Property prop) throws IOException {
        jg.writeStartObject();
        for (Property p : prop.getChildren()) {
            jg.writeFieldName(p.getName());
            writeProperty(jg, p);
        }
        jg.writeEndObject();
    }

    protected void writeBlobProperty(JsonGenerator jg, Property prop) throws IOException {
        Blob blob = (Blob) prop.getValue();
        if (blob == null) {
            jg.writeNull();
            return;
        }
        jg.writeStartObject();
        String v = blob.getFilename();
        if (v == null) {
            jg.writeNullField("name");
        } else {
            jg.writeStringField("name", v);
        }
        v = blob.getMimeType();
        if (v == null) {
            jg.writeNullField("mime-type");
        } else {
            jg.writeStringField("mime-type", v);
        }
        v = blob.getEncoding();
        if (v == null) {
            jg.writeNullField("encoding");
        } else {
            jg.writeStringField("encoding", v);
        }
        v = blob.getDigest();
        if (v == null) {
            jg.writeNullField("digest");
        } else {
            jg.writeStringField("digest", v);
        }
        jg.writeStringField("length", Long.toString(blob.getLength()));

        String blobUrl = getBlobUrl(prop);
        if (blobUrl == null) {
            blobUrl = "";
        }
        jg.writeStringField("data", blobUrl);
        jg.writeEndObject();
    }

    /**
     * Gets the full URL of where a blob can be downloaded.
     *
     * @since 7.2
     */
    private String getBlobUrl(Property prop) {
        DocumentModel doc = ctx.getParameter(ENTITY_TYPE);
        if (doc == null) {
            return "";
        }
        StringBuilder blobUrlBuilder = new StringBuilder(ctx.getBaseUrl());
        blobUrlBuilder.append("nxbigfile/").append(doc.getRepositoryName()).append("/").append(doc.getId()).append("/");
        blobUrlBuilder.append(prop.getSchema().getName());
        blobUrlBuilder.append(":");
        String canonicalXPath = ComplexTypeImpl.canonicalXPath(prop.getPath().substring(1));
        blobUrlBuilder.append(canonicalXPath);
        blobUrlBuilder.append("/");
        String filename = ((Blob) prop.getValue()).getFilename();
        if (filename != null) {
            blobUrlBuilder.append(URIUtils.quoteURIPathComponent(filename, true));
        }
        return blobUrlBuilder.toString();
    }

}
