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

package org.nuxeo.ecm.core.io.marshallers.json.types;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link Schema} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing {@link Schema}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(Object, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"schema",
 *   "name": "SCHEMA_NAME",
 *   "prefix: "SCHEMA_PREFIX",  <- only if there's a prefix
 *   "fields", {
 *     "PRIMITIVE_FIELD_LOCAL_NAME": "FIELD_TYPE", <- where field type is {@link Type#getName()} (string, boolean, integer, ...)
 *     "PRIMITIVE_LIST_LOCAL_NAME": "FIELD_TYPE[]" <- where field type is {@link Type#getName()} (string, boolean, integer, ...)
 *     "COMPLEX_FIELD_LOCAL_NAME" : {
 *       "type": "complex",
 *       "fields": {
 *         loop the same format
 *       }
 *     },
 *     "COMPLEX_LIST_FIELD_LOCAL_NAME" : {
 *       "type": "complex[]",
 *       "fields": {
 *         loop the same format
 *       }
 *     },
 *     "CONTENT_FIELD": "blob",
 *     "CONTENT_LIST_FIELD": "blob[]",
 *     ...
 *   }
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SchemaJsonWriter extends ExtensibleEntityJsonWriter<Schema> {

    public static final String ENTITY_TYPE = "schema";

    /**
     * @since 8.10
     */
    public static final String FETCH_FIELDS = "fields";

    public SchemaJsonWriter() {
        super(ENTITY_TYPE, Schema.class);
    }

    @Override
    protected void writeEntityBody(Schema schema, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", schema.getName());
        String prefix = schema.getNamespace().prefix;
        if (StringUtils.isNotBlank(prefix)) {
            jg.writeStringField("prefix", prefix);
            // backward compat for old schema writers
            jg.writeStringField("@prefix", prefix);
        }
        jg.writeObjectFieldStart("fields");
        for (Field field : schema.getFields()) {
            writeField(jg, field);
        }
        jg.writeEndObject();
    }

    protected void writeField(JsonGenerator jg, Field field) throws IOException {
        if (!field.getType().isComplexType()) {
            if (field.getType().isListType()) {
                ListType lt = (ListType) field.getType();
                if (lt.getFieldType().isComplexType()) {
                    if (lt.getFieldType().getName().equals("content")) {
                        jg.writeStringField(field.getName().getLocalName(), "blob[]");
                    } else {
                        jg.writeObjectFieldStart(field.getName().getLocalName());
                        jg.writeStringField("type", "complex[]");
                        jg.writeObjectFieldStart("fields");
                        ComplexType cplXType = (ComplexType) lt.getField().getType();
                        for (Field subField : cplXType.getFields()) {
                            writeField(jg, subField);
                        }
                        jg.writeEndObject();
                        jg.writeEndObject();
                    }
                } else {
                    doWriteField(jg, field);
                }
            } else {
                doWriteField(jg, field);
            }
        } else {
            if (field.getType().getName().equals("content")) {
                jg.writeStringField(field.getName().getLocalName(), "blob");
            } else {
                jg.writeObjectFieldStart(field.getName().getLocalName());
                ComplexType cplXType = (ComplexType) field.getType();
                jg.writeObjectFieldStart("fields");
                for (Field subField : cplXType.getFields()) {
                    writeField(jg, subField);
                }
                jg.writeEndObject();
                jg.writeStringField("type", "complex");
                jg.writeEndObject();
            }
        }
    }

    /**
     * @since 8.10
     */
    protected void doWriteField(JsonGenerator jg, Field field) throws IOException {
        final boolean extended = ctx.getFetched(ENTITY_TYPE).contains(FETCH_FIELDS);
        String typeValue;
        Set<Constraint> itemConstraints = null;
        if (field.getType().isListType()) {
            ListType lt = (ListType) field.getType();
            Type type = lt.getFieldType();
            itemConstraints = type.getConstraints();
            while (!(type instanceof PrimitiveType)) {
                type = type.getSuperType();
            }
            typeValue = type.getName() + "[]";
        } else {
            Type type = field.getType();
            while (!(type instanceof PrimitiveType)) {
                type = type.getSuperType();
            }
            typeValue = type.getName();
        }
        if (extended) {
            jg.writeObjectFieldStart(field.getName().getLocalName());
            jg.writeStringField("type", typeValue);
            Writer<Constraint> constraintWriter = registry.getWriter(ctx, Constraint.class, APPLICATION_JSON_TYPE);
            OutputStream out = new OutputStreamWithJsonWriter(jg);
            jg.writeArrayFieldStart("constraints");
            for (Constraint c : field.getConstraints()) {
                constraintWriter.write(c, Constraint.class, Constraint.class, APPLICATION_JSON_TYPE, out);
            }
            jg.writeEndArray();
            if (itemConstraints != null) {
                jg.writeArrayFieldStart("itemConstraints");
                for (Constraint c : itemConstraints) {
                    constraintWriter.write(c, Constraint.class, Constraint.class, APPLICATION_JSON_TYPE, out);
                }
                jg.writeEndArray();
            }
            jg.writeEndObject();
        } else {
            jg.writeStringField(field.getName().getLocalName(), typeValue);
        }

    }

}
