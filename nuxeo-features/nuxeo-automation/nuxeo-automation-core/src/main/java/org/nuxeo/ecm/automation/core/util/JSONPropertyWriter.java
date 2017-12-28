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

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Helper to marshaling properties into JSON.
 *
 * @since 7.1
 */
public class JSONPropertyWriter {

    /**
     * The date time format.
     *
     * @since 9.1
     */
    protected DateTimeFormat dateTimeFormat = DateTimeFormat.W3C;

    /**
     * The baseUrl that can be used to locate blob content.
     *
     * @since 9.1
     */
    protected String filesBaseUrl;

    /**
     * The prefix to append to field name.
     *
     * @since 9.1
     */
    protected String prefix;

    /**
     * Whether or not this writer write null values.
     *
     * @since 9.1
     */
    protected boolean writeNull = true;

    /**
     * Whether or not this writer write empty list or object.
     *
     * @since 9.1
     */
    protected boolean writeEmpty = true;

    /**
     * Instantiate a JSONPropertyWriter.
     */
    protected JSONPropertyWriter() {
        // Default constructor
    }

    /**
     * Copy constructor.
     *
     * @since 9.1
     */
    protected JSONPropertyWriter(JSONPropertyWriter writer) {
        this.dateTimeFormat = writer.dateTimeFormat;
        this.filesBaseUrl = writer.filesBaseUrl;
        this.prefix = writer.prefix;
        this.writeNull = writer.writeNull;
    }

    /**
     * @return a {@link JSONPropertyWriter} instance with {@link DateTimeFormat#W3C} as date time formatter.
     * @since 9.1
     */
    public static JSONPropertyWriter create() {
        return new JSONPropertyWriter();
    }

    /**
     * @return this {@link JSONPropertyWriter} filled with the previous configuration and the input dateTimeFormat.
     * @since 9.1
     */
    public JSONPropertyWriter dateTimeFormat(DateTimeFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        return this;
    }

    /**
     * @param filesBaseUrl the baseUrl that can be used to locate blob content
     * @return this {@link JSONPropertyWriter} filled with the previous configuration and the input filesBaseUrl.
     * @since 9.1
     */
    public JSONPropertyWriter filesBaseUrl(String filesBaseUrl) {
        this.filesBaseUrl = filesBaseUrl;
        if (this.filesBaseUrl != null && !this.filesBaseUrl.endsWith("/")) {
            this.filesBaseUrl += "/";
        }
        return this;
    }

    /**
     * @param prefix the prefix to append for each property
     * @return this {@link JSONPropertyWriter} filled with the previous configuration and the input prefix.
     * @since 9.1
     */
    public JSONPropertyWriter prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * @param writeNull whether or not this writer might write null values
     * @return this {@link JSONPropertyWriter} filled with the previous configuration and the input writeNull value.
     * @since 9.1
     */
    public JSONPropertyWriter writeNull(boolean writeNull) {
        this.writeNull = writeNull;
        return this;
    }

    /**
     * @param writeEmpty whether or not this writer might write empty array/list/object
     * @return this {@link JSONPropertyWriter} filled with the previous configuration and the input writeEmpty value.
     * @since 9.1
     */
    public JSONPropertyWriter writeEmpty(boolean writeEmpty) {
        this.writeEmpty = writeEmpty;
        return this;
    }

    /**
     * Converts the value of the given core property to JSON.
     * <p />
     * CAUTION: this method will write the field name to {@link JsonGenerator} with its prefix without writing the start
     * and the end of object.
     *
     * @since 9.1
     */
    public void writeProperty(JsonGenerator jg, Property prop)
            throws PropertyException, JsonGenerationException, IOException {
        PropertyConsumer fieldNameWriter;
        if (prefix == null) {
            fieldNameWriter = (j, p) -> j.writeFieldName(p.getName());
        } else {
            fieldNameWriter = (j, p) -> j.writeFieldName(prefix + ':' + p.getField().getName().getLocalName());
        }
        writeProperty(jg, prop, fieldNameWriter);
    }

    /**
     * Converts the value of the given core property to JSON.
     *
     * @param fieldNameWriter the field name writer is used to write the field name depending on writer configuration,
     *            this parameter also allows us to handle different cases: field with prefix, field under complex
     *            property, or nothing for arrays and lists
     */
    protected void writeProperty(JsonGenerator jg, Property prop, PropertyConsumer fieldNameWriter)
            throws PropertyException, JsonGenerationException, IOException {
        if (prop.isScalar()) {
            writeScalarProperty(jg, prop, fieldNameWriter);
        } else if (prop.isList()) {
            writeListProperty(jg, prop, fieldNameWriter);
        } else {
            if (prop.isPhantom()) {
                if (writeNull) {
                    fieldNameWriter.accept(jg, prop);
                    jg.writeNull();
                }
            } else if (prop instanceof BlobProperty) { // a blob
                writeBlobProperty(jg, prop, fieldNameWriter);
            } else { // a complex property
                writeMapProperty(jg, (ComplexProperty) prop, fieldNameWriter);
            }
        }
    }

    protected void writeScalarProperty(JsonGenerator jg, Property prop, PropertyConsumer fieldNameWriter)
            throws PropertyException, JsonGenerationException, IOException {
        Type type = prop.getType();
        Object v = prop.getValue();
        if (v == null) {
            if (writeNull) {
                fieldNameWriter.accept(jg, prop);
                jg.writeNull();
            }
        } else {
            fieldNameWriter.accept(jg, prop);
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

    protected void writeListProperty(JsonGenerator jg, Property prop, PropertyConsumer fieldNameWriter)
            throws PropertyException, JsonGenerationException, IOException {
        // test if array/list is empty - don't write empty case
        if (!writeEmpty && (prop == null || (prop instanceof ArrayProperty && prop.getValue() == null)
                || (prop instanceof ListProperty && prop.getChildren().isEmpty()))) {
            return;
        }
        fieldNameWriter.accept(jg, prop);
        jg.writeStartArray();
        if (prop instanceof ArrayProperty) {
            Object[] ar = (Object[]) prop.getValue();
            if (ar == null) {
                jg.writeEndArray();
                return;
            }
            Type type = ((ListType) prop.getType()).getFieldType();
            for (Object o : ar) {
                jg.writeString(type.encode(o));
            }
        } else {
            for (Property p : prop.getChildren()) {
                // it's a list of complex object, don't write field names
                writeProperty(jg, p, PropertyConsumer.nothing());
            }
        }
        jg.writeEndArray();
    }

    protected void writeMapProperty(JsonGenerator jg, ComplexProperty prop, PropertyConsumer fieldNameWriter)
            throws PropertyException, JsonGenerationException, IOException {
        if (!writeEmpty && (prop == null || prop.getChildren().isEmpty())) {
            return;
        }
        fieldNameWriter.accept(jg, prop);
        jg.writeStartObject();
        PropertyConsumer childFieldWriter = (j, p) -> j.writeFieldName(p.getName());
        for (Property p : prop.getChildren()) {
            writeProperty(jg, p, childFieldWriter);
        }
        jg.writeEndObject();
    }

    protected void writeBlobProperty(JsonGenerator jg, Property prop, PropertyConsumer fieldNameWriter)
            throws PropertyException, JsonGenerationException, IOException {
        Blob blob = (Blob) prop.getValue();
        if (blob == null) {
            if (writeNull) {
                fieldNameWriter.accept(jg, prop);
                jg.writeNull();
            }
        } else {
            fieldNameWriter.accept(jg, prop);
            jg.writeStartObject();
            String v = blob.getFilename();
            if (v == null) {
                if (writeNull) {
                    jg.writeNullField("name");
                }
            } else {
                jg.writeStringField("name", v);
            }
            v = blob.getMimeType();
            if (v == null) {
                if (writeNull) {
                    jg.writeNullField("mime-type");
                }
            } else {
                jg.writeStringField("mime-type", v);
            }
            v = blob.getEncoding();
            if (v == null) {
                if (writeNull) {
                    jg.writeNullField("encoding");
                }
            } else {
                jg.writeStringField("encoding", v);
            }
            v = blob.getDigest();
            if (v == null) {
                if (writeNull) {
                    jg.writeNullField("digest");
                }
            } else {
                jg.writeStringField("digest", v);
            }
            jg.writeNumberField("length", blob.getLength());
            if (filesBaseUrl != null) {
                jg.writeStringField("data", getBlobUrl(prop, filesBaseUrl));
            }
            jg.writeEndObject();
        }
    }

    /**
     * Gets the full URL of where a blob can be downloaded.
     *
     * @since 5.9.3
     */
    private static String getBlobUrl(Property prop, String filesBaseUrl)
            throws UnsupportedEncodingException, PropertyException {
        StringBuilder blobUrlBuilder = new StringBuilder(filesBaseUrl);
        String xpath = prop.getXPath();
        if (!xpath.contains(":")) {
            // if no prefix, use schema name as prefix:
            xpath = prop.getSchema().getName() + ":" + xpath;
        }
        blobUrlBuilder.append(xpath);
        blobUrlBuilder.append("/");
        String filename = ((Blob) prop.getValue()).getFilename();
        if (filename != null) {
            blobUrlBuilder.append(URIUtils.quoteURIPathComponent(filename, true));
        }
        return blobUrlBuilder.toString();
    }

    /**
     * Converts the value of the given core property to JSON. The given filesBaseUrl is the baseUrl that can be used to
     * locate blob content and is useful to generate blob URLs.
     */
    public static void writePropertyValue(JsonGenerator jg, Property prop, DateTimeFormat dateTimeFormat,
            String filesBaseUrl) throws PropertyException, JsonGenerationException, IOException {
        JSONPropertyWriter writer = create().dateTimeFormat(dateTimeFormat).filesBaseUrl(filesBaseUrl);
        // as we just want to write property value, give a nothing consumer
        writer.writeProperty(jg, prop, PropertyConsumer.nothing());
    }

    @FunctionalInterface
    public interface PropertyConsumer {

        void accept(JsonGenerator jg, Property prop) throws JsonGenerationException, IOException;

        static PropertyConsumer nothing() {
            return (jg, prop) -> {
            };
        }

    }

}
