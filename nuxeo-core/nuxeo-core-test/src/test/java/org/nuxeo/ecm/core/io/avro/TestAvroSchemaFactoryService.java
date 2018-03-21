/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.io.avro;

import static org.apache.avro.Schema.Type.BYTES;
import static org.apache.avro.Schema.Type.LONG;
import static org.apache.avro.Schema.Type.STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.SchemaNormalization;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;
import org.nuxeo.runtime.avro.AvroSchemaFactoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.runtime.stream")
public class TestAvroSchemaFactoryService {

    protected static Collection<String> FORBIDDEN = Arrays.asList("-", "__tutu__", "-gru__du__buk-");

    protected static final String FIELD = "field";

    protected static final Map<String, String> TYPES_MAPPING = new HashMap<>();

    static {
        TYPES_MAPPING.put("date", "timestamp-millis");
        TYPES_MAPPING.put("binary", "bytes");
    }

    @Inject
    public AvroSchemaFactoryService service;

    @Inject
    public SchemaManager schemaManager;

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testComplexDocAvroSchemaFields() {
        DocumentType type = schemaManager.getDocumentType("ComplexDoc");
        AvroSchemaFactoryContext context = service.createContext();
        // schema creation
        Schema avro = context.createSchema(type);
        assertNotNull(avro);
        // 3 root schemas
        Field common = avro.getField("common");
        assertNotNull(common);
        Field dublincore = avro.getField("dublincore");
        assertNotNull(dublincore);
        Field complexschema = avro.getField("complexschema");
        assertNotNull(complexschema);
        // exploration of complexschema
        assertEquals("cmpf", complexschema.schema().getNamespace());
        Field attachedFile = complexschema.schema().getField("attachedFile");
        Schema fileext = attachedFile.schema();
        assertEquals("fileext", fileext.getName());
        assertSame(STRING, fileext.getField("name").schema().getType());
        Schema vignettes = fileext.getField("vignettes").schema();
        assertEquals("array", vignettes.getType().getName());
        Schema vignette = vignettes.getElementType();
        assertSame(STRING, vignette.getField("label").schema().getType());
        assertSame(LONG, vignette.getField("width").schema().getType());
        assertSame(LONG, vignette.getField("height").schema().getType());
        Schema content = vignette.getField("content").schema();
        assertNull(content.getField("mime-type"));
        assertSame(STRING, content.getField("name").schema().getType());
        assertSame(STRING, content.getField("digest").schema().getType());
        assertSame(STRING, content.getField("encoding").schema().getType());
        assertSame(STRING, content.getField(context.replaceForbidden("mime-type")).schema().getType());
        assertSame(LONG, content.getField("length").schema().getType());
        assertSame(BYTES, content.getField("data").schema().getType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testComplexDocJsonCompare() throws IOException {
        DocumentType type = schemaManager.getDocumentType("ComplexDoc");
        AvroSchemaFactoryContext context = service.createContext();
        // schema creation
        Schema avro = context.createSchema(type);
        String ugly = SchemaNormalization.toParsingForm(avro);
        assertEquals(getContent("ComplexDocAvroSchema.json"), ugly);
        String pretty = avro.toString(true);
        assertEquals(getContent("ComplexDocAvroSchemaPretty.json"), pretty);
    }

    @Test
    public void testDocumentTypeFactory() {
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            AvroSchemaFactoryContext context = service.createContext();
            Schema avro = context.createSchema(documentType);
            assertNotNull(avro);
            assertEquals(documentType.getSchemas().size(), avro.getFields().size());
            // the toString(boolean pretty) call here is to assert no exception is thrown during the call
            // in some cases the schema can be built but not serialized
            avro.toString(true);
        }
    }

    @Test
    public void testForbiddenCharsConvertion() {
        SchemaImpl nuxeo = new SchemaImpl("testSchema", new Namespace("hhtp://test.com/schema/test", "tst"));
        for (String forbidden : FORBIDDEN) {
            nuxeo.addField(forbidden, StringType.INSTANCE, null, 0, null);
            nuxeo.addField(forbidden + FIELD + forbidden, StringType.INSTANCE, null, 0, null);
            nuxeo.addField(FIELD + forbidden + FIELD, StringType.INSTANCE, null, 0, null);
        }
        // tricks the QName.valueOf() that takes the prefix before the first :
        nuxeo.addField("dc:dc:title", StringType.INSTANCE, null, 0, null);
        nuxeo.addField("icon-expanded", StringType.INSTANCE, null, 0, null);
        AvroSchemaFactoryContext context = service.createContext();
        Schema avro = context.createSchema(nuxeo);
        assertNotNull(avro);
        for (String forbidden : FORBIDDEN) {
            String single = context.replaceForbidden(forbidden);
            assertEquals(single, avro.getField(single).name());
            String black = context.replaceForbidden(FIELD + forbidden + FIELD);
            assertEquals(black, avro.getField(black).name());
            String white = context.replaceForbidden(forbidden + FIELD + forbidden);
            assertEquals(white, avro.getField(white).name());
        }
        assertNull(avro.getField("title"));
        assertNull(avro.getField("dc:title"));
        assertNull(avro.getField("dc:dc:title"));
        assertNotNull(avro.getField("dc__colon__title"));
        assertNull(avro.getField("icon-expanded"));
        assertNotNull(avro.getField("icon__dash__expanded"));
    }

    @Test
    public void testForbiddenReplacementRightPriorities() {
        assertNotNull(getReplacedField());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/avro-contrib-tests.xml")
    public void testForbiddenReplacementWrongPriorities() {
        assertNull(getReplacedField());
    }

    @Test
    public void testSchemaFactory() {
        for (org.nuxeo.ecm.core.schema.types.Schema nuxeo : schemaManager.getSchemas()) {
            AvroSchemaFactoryContext context = service.createContext();
            Schema avro = context.createSchema(nuxeo);
            assertNotNull(avro);
            assertEquals(nuxeo.getFields().size(), avro.getFields().size());
            // the toString(boolean pretty) call here is to assert no exception is thrown during the call
            // in some cases the schema can be built but not serialized
            avro.toString(true);
            deepAssert(nuxeo, avro);
        }
    }

    protected void deepAssert(org.nuxeo.ecm.core.schema.types.ComplexType nuxeo, Schema avro) {
        AvroSchemaFactoryContext context = service.createContext();
        for (org.nuxeo.ecm.core.schema.types.Field nuxeoF : nuxeo.getFields()) {
            String cleanedNuxeoName = context.replaceForbidden(nuxeoF.getName().getLocalName());
            Field avroF = avro.getField(cleanedNuxeoName);
            assertEquals(cleanedNuxeoName, avroF.name());
            if (nuxeoF.getType().isComplexType()) {
                deepAssert((ComplexType) nuxeoF.getType(), avroF.schema());
            } else {
                String actualAvroTypeName = getActualAvroNameType(avroF);
                String actualNuxeoTypeName = getActuelNuxeoNameType(nuxeoF.getType());
                assertEquals(actualNuxeoTypeName, actualAvroTypeName);
            }
        }
    }

    protected String getActualAvroNameType(Field avroF) {
        String actualAvroTypeName = avroF.schema().getLogicalType() != null
                ? avroF.schema().getLogicalType().getName()
                : avroF.schema().getName();
        if (actualAvroTypeName.equals("array")) {
            actualAvroTypeName = avroF.schema().getElementType().getName();
        }
        return actualAvroTypeName;
    }

    protected String getActuelNuxeoNameType(Type type) {
        if (type.isListType()) {
            ListType list = (ListType) type;
            Type fieldType = list.getFieldType();
            String fieldTypeName = fieldType.getName().contains("anonymous")
                    ? fieldType.getSuperType().getName()
                    : fieldType.getName();
            return TYPES_MAPPING.getOrDefault(fieldTypeName, fieldTypeName);
        } else {
            return TYPES_MAPPING.getOrDefault(type.getName(), type.getName());
        }
    }

    protected String getContent(String fileName) throws IOException {
        // this is the value Avro is using
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining(System.getProperty("line.separator")));
        }
    }

    protected Field getReplacedField() {
        SchemaImpl nuxeo = new SchemaImpl("testSchema", new Namespace("hhtp://test.com/schema/test", "tst"));
        nuxeo.addField("test-test", StringType.INSTANCE, null, 0, null);
        Schema avro = service.createContext().createSchema(nuxeo);
        return avro.getField("test__dash__test");
    }

}
