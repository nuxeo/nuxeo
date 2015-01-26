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

package org.nuxeo.ecm.core.io.marshallers.json.types;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link Schema} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing {@link Schema}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(Schema, JsonWriter)}.
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
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SchemaJsonWriter extends ExtensibleEntityJsonWriter<Schema> {

    public static final String ENTITY_TYPE = "schema";

    public SchemaJsonWriter() {
        super(ENTITY_TYPE, Schema.class);
    }

    @Override
    protected void writeEntityBody(Schema schema, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", schema.getName());
        String prefix = schema.getNamespace().prefix;
        if (StringUtils.isNotBlank(prefix)) {
            jg.writeStringField("prefix", prefix);
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
                    Type type = lt.getFieldType();
                    while (!(type instanceof PrimitiveType)) {
                        type = type.getSuperType();
                    }
                    jg.writeStringField(field.getName().getLocalName(), type.getName() + "[]");
                }
            } else {
                Type type = field.getType();
                while (!(type instanceof PrimitiveType)) {
                    type = type.getSuperType();
                }
                jg.writeStringField(field.getName().getLocalName(), type.getName());
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

}
