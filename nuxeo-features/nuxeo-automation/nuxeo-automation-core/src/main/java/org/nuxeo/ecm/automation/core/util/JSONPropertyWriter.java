/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Bogdan Stefanescu
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

/**
 * Helper to marshaling properties into JSON.
 *
 * @since 7.1
 */
public class JSONPropertyWriter {

    /** Utility class. */
    private JSONPropertyWriter() {
    }

    /**
     * Converts the value of the given core property to JSON. The given filesBaseUrl is the baseUrl that can be used to
     * locate blob content and is useful to generate blob URLs.
     */
    public static void writePropertyValue(JsonGenerator jg, Property prop, DateTimeFormat dateTimeFormat,
            String filesBaseUrl) throws PropertyException, JsonGenerationException, IOException {
        if (prop.isScalar()) {
            writeScalarPropertyValue(jg, prop, dateTimeFormat);
        } else if (prop.isList()) {
            writeListPropertyValue(jg, prop, dateTimeFormat, filesBaseUrl);
        } else {
            if (prop.isPhantom()) {
                jg.writeNull();
            } else if (prop instanceof BlobProperty) { // a blob
                writeBlobPropertyValue(jg, prop, filesBaseUrl);
            } else { // a complex property
                writeMapPropertyValue(jg, (ComplexProperty) prop, dateTimeFormat, filesBaseUrl);
            }
        }
    }

    @SuppressWarnings("boxing")
    protected static void writeScalarPropertyValue(JsonGenerator jg, Property prop, DateTimeFormat dateTimeFormat)
            throws PropertyException, IOException {
        org.nuxeo.ecm.core.schema.types.Type type = prop.getType();
        Object v = prop.getValue();
        if (v == null) {
            jg.writeNull();
        } else {
            if (type instanceof BooleanType) {
                jg.writeBoolean((Boolean) v);
            } else if (type instanceof LongType) {
                jg.writeNumber((Long) v);
            } else if (type instanceof DoubleType) {
                jg.writeNumber((Double) v);
            } else if (type instanceof IntegerType) {
                jg.writeNumber((Integer) v);
            } else if (type instanceof BinaryType) {
                jg.writeBinary((byte[]) v);
            } else if (type instanceof DateType && dateTimeFormat == DateTimeFormat.TIME_IN_MILLIS) {
                if (v instanceof Date) {
                    jg.writeNumber(((Date) v).getTime());
                } else if (v instanceof Calendar) {
                    jg.writeNumber(((Calendar) v).getTimeInMillis());
                } else {
                    throw new PropertyException("Unknown class for DateType: " + v.getClass().getName() + ", " + v);
                }
            } else {
                jg.writeString(type.encode(v));
            }
        }
    }

    protected static void writeListPropertyValue(JsonGenerator jg, Property prop, DateTimeFormat dateTimeFormat,
            String filesBaseUrl) throws PropertyException, JsonGenerationException, IOException {
        jg.writeStartArray();
        if (prop instanceof ArrayProperty) {
            Object[] ar = (Object[]) prop.getValue();
            if (ar == null) {
                jg.writeEndArray();
                return;
            }
            org.nuxeo.ecm.core.schema.types.Type type = ((ListType) prop.getType()).getFieldType();
            for (Object o : ar) {
                jg.writeString(type.encode(o));
            }
        } else {
            ListProperty listp = (ListProperty) prop;
            for (Property p : listp.getChildren()) {
                writePropertyValue(jg, p, dateTimeFormat, filesBaseUrl);
            }
        }
        jg.writeEndArray();
    }

    protected static void writeMapPropertyValue(JsonGenerator jg, ComplexProperty prop, DateTimeFormat dateTimeFormat,
            String filesBaseUrl) throws JsonGenerationException, IOException, PropertyException {
        jg.writeStartObject();
        for (Property p : prop.getChildren()) {
            jg.writeFieldName(p.getName());
            writePropertyValue(jg, p, dateTimeFormat, filesBaseUrl);
        }
        jg.writeEndObject();
    }

    protected static void writeBlobPropertyValue(JsonGenerator jg, Property prop, String filesBaseUrl)
            throws PropertyException, JsonGenerationException, IOException {
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
        jg.writeNumberField("length", blob.getLength());
        if (filesBaseUrl != null) {
            jg.writeStringField("data", getBlobUrl(prop, filesBaseUrl));
        }
        jg.writeEndObject();
    }

    /**
     * Gets the full URL of where a blob can be downloaded.
     *
     * @since 5.9.3
     */
    private static String getBlobUrl(Property prop, String filesBaseUrl) throws UnsupportedEncodingException,
            PropertyException {
        StringBuilder blobUrlBuilder = new StringBuilder(filesBaseUrl);
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
