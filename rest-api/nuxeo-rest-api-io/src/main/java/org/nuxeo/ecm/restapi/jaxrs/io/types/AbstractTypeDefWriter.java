/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 */
package org.nuxeo.ecm.restapi.jaxrs.io.types;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;

public class AbstractTypeDefWriter {

    public AbstractTypeDefWriter() {
        super();
    }

    protected JsonGenerator getGenerator(OutputStream entityStream)
            throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonGenerator jg = factory.createJsonGenerator(entityStream,
                JsonEncoding.UTF8);
        jg.useDefaultPrettyPrinter();
        return jg;
    }

    protected void writeSchema(JsonGenerator jg, Schema schema)
            throws Exception {
        jg.writeObjectFieldStart(schema.getName());
        jg.writeStringField("@prefix", schema.getNamespace().prefix);
        for (Field field : schema.getFields()) {
            writeField(jg, field);
        }
        jg.writeEndObject();
    }

    protected void writeSchemaObject(JsonGenerator jg, Schema schema)
            throws Exception {
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

    protected void writeDocType(JsonGenerator jg, DocumentType docType,
            boolean expandSchemas) throws Exception {

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

    protected void writeField(JsonGenerator jg, Field field) throws Exception {
        if (!field.getType().isComplexType()) {
            if (field.getType().isListType()) {
                ListType lt = (ListType) field.getType();
                if (lt.getFieldType().isComplexType()) {
                    if (lt.getFieldType().getName().equals("content")) {
                        jg.writeStringField(field.getName().getLocalName(),
                                "blob[]");

                    } else {
                        jg.writeObjectFieldStart(field.getName().getLocalName());
                        buildComplexFields(jg, lt.getField());
                        jg.writeStringField("type", "complex[]");
                        jg.writeEndObject();
                    }
                } else {

                    jg.writeStringField(field.getName().getLocalName(),
                            lt.getFieldType().getName() + "[]");
                }
            } else {
                jg.writeStringField(field.getName().getLocalName(),
                        field.getType().getName());
            }
        } else {
            if (field.getType().getName().equals("content")) {
                jg.writeStringField(field.getName().getLocalName(), "blob");
            } else {

                jg.writeObjectFieldStart(field.getName().getLocalName());
                buildComplexFields(jg, field);
                jg.writeStringField("type", "complex");
                jg.writeEndObject();
            }
        }

    }

    protected void buildComplexFields(JsonGenerator jg, Field field)
            throws Exception {
        ComplexType cplXType = (ComplexType) field.getType();
        jg.writeObjectFieldStart("fields");
        for (Field subField : cplXType.getFields()) {
            writeField(jg, subField);
        }
        jg.writeEndObject();
    }

    protected void writeFacet(JsonGenerator jg, CompositeType facet,
            boolean expandSchemas) throws Exception {

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