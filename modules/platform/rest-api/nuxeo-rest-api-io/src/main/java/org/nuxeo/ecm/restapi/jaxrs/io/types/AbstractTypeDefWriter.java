/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */
package org.nuxeo.ecm.restapi.jaxrs.io.types;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class AbstractTypeDefWriter {

    public AbstractTypeDefWriter() {
        super();
    }

    protected JsonGenerator getGenerator(OutputStream entityStream) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonGenerator jg = factory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
        jg.useDefaultPrettyPrinter();
        return jg;
    }

    protected void writeSchema(JsonGenerator jg, Schema schema) throws IOException {
        jg.writeObjectFieldStart(schema.getName());
        jg.writeStringField("@prefix", schema.getNamespace().prefix);
        for (Field field : schema.getFields()) {
            writeField(jg, field);
        }
        jg.writeEndObject();
    }

    protected void writeSchemaObject(JsonGenerator jg, Schema schema) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", schema.getName());
        jg.writeStringField("@prefix", schema.getNamespace().prefix);
        jg.writeObjectFieldStart("fields");
        for (Field field : schema.getFields()) {
            writeField(jg, field);
        }
        jg.writeEndObject();
        jg.writeEndObject();
    }

    protected void writeDocType(JsonGenerator jg, DocumentType docType, boolean expandSchemas) throws IOException {

        if (docType.getSuperType() != null) {
            jg.writeStringField("parent", docType.getSuperType().getName());
        } else {
            jg.writeStringField("parent", "None!!!");
        }

        jg.writeArrayFieldStart("facets");
        for (String facet : docType.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("schemas");
        if (expandSchemas) {
            for (Schema schema : docType.getSchemas()) {
                writeSchemaObject(jg, schema);
            }
        } else {
            for (String schema : docType.getSchemaNames()) {
                jg.writeString(schema);
            }
        }
        jg.writeEndArray();

    }

    protected void writeField(JsonGenerator jg, Field field) throws IOException {
        Type type = field.getType();
        if (!type.isComplexType()) {
            if (type.isListType()) {
                ListType lt = (ListType) type;
                if (lt.getFieldType().isComplexType()) {
                    if (lt.getFieldType().getName().equals("content")) {
                        jg.writeStringField(field.getName().getLocalName(), "blob[]");

                    } else {
                        jg.writeObjectFieldStart(field.getName().getLocalName());
                        buildComplexFields(jg, lt.getField());
                        jg.writeStringField("type", "complex[]");
                        jg.writeEndObject();
                    }
                } else {
                    Type fieldType = lt.getFieldType();
                    if (fieldType instanceof SimpleType) {
                        SimpleType stype = (SimpleType) fieldType;
                        fieldType = stype.getPrimitiveType();
                    }
                    jg.writeStringField(field.getName().getLocalName(), fieldType.getName() + "[]");
                }
            } else {
                if (type instanceof SimpleType) {
                    SimpleType stype = (SimpleType) type;
                    type = stype.getPrimitiveType();
                }
                jg.writeStringField(field.getName().getLocalName(), type.getName());
            }
        } else {
            if (type.getName().equals("content")) {
                jg.writeStringField(field.getName().getLocalName(), "blob");
            } else {

                jg.writeObjectFieldStart(field.getName().getLocalName());
                buildComplexFields(jg, field);
                jg.writeStringField("type", "complex");
                jg.writeEndObject();
            }
        }

    }

    protected void buildComplexFields(JsonGenerator jg, Field field) throws IOException {
        ComplexType cplXType = (ComplexType) field.getType();
        jg.writeObjectFieldStart("fields");
        for (Field subField : cplXType.getFields()) {
            writeField(jg, subField);
        }
        jg.writeEndObject();
    }

    protected void writeFacet(JsonGenerator jg, CompositeType facet, boolean expandSchemas) throws IOException {

        jg.writeStringField("name", facet.getName());
        if (facet.getSchemaNames() != null && facet.getSchemaNames().length > 0) {
            jg.writeArrayFieldStart("schemas");
            if (expandSchemas) {
                for (Schema schema : facet.getSchemas()) {
                    writeSchemaObject(jg, schema);
                }
            } else {
                for (String schemaName : facet.getSchemaNames()) {
                    jg.writeString(schemaName);
                }
            }
            jg.writeEndArray();
        }

    }
}
