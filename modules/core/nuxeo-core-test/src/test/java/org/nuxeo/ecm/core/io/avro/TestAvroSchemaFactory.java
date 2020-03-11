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
import static org.apache.avro.Schema.Type.NULL;
import static org.apache.avro.Schema.Type.RECORD;
import static org.apache.avro.Schema.Type.STRING;
import static org.apache.avro.Schema.Type.UNION;
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
import org.nuxeo.runtime.avro.AvroService;
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
public class TestAvroSchemaFactory {

    protected static Collection<String> FORBIDDEN = Arrays.asList("-", "__tutu__", "-gru__du__buk-");

    protected static final String FIELD = "field";

    protected static final Map<String, String> TYPES_MAPPING = new HashMap<>();

    static {
        TYPES_MAPPING.put("date", "timestamp-millis");
        TYPES_MAPPING.put("binary", "bytes");
    }

    @Inject
    public AvroService service;

    @Inject
    public SchemaManager schemaManager;

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testComplexDocAvroSchemaFields() {
        DocumentType type = schemaManager.getDocumentType("ComplexDoc");
        // schema creation
        Schema avro = service.createSchema(type);
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
        Schema union = attachedFile.schema();
        assertSame(UNION, union.getType());
        assertSame(NULL, union.getTypes().get(0).getType());
        assertEquals("fileext", union.getTypes().get(1).getName());
        Schema fileext = union.getTypes().get(1);
        assertSame(RECORD, fileext.getType());
        Schema vignettes = fileext.getField("vignettes").schema();
        assertEquals(UNION, vignettes.getType());
        Schema vignette = vignettes.getTypes().get(1).getElementType();
        assertSame(UNION, vignette.getField("label").schema().getType());
        assertSame(LONG, getSchema(vignette.getField("width").schema()).getType());
        assertSame(LONG, getSchema(vignette.getField("height").schema()).getType());
        assertSame(STRING, getSchema(vignette.getField("label").schema()).getType());
        assertSame(UNION, vignette.getField("content").schema().getType());
        Schema content = getSchema(vignette.getField("content").schema());
        assertNull(content.getField("mime-type"));
        assertSame(STRING, getSchema(content.getField("name").schema()).getType());
        assertSame(STRING, getSchema(content.getField("digest").schema()).getType());
        assertSame(STRING, getSchema(content.getField("encoding").schema()).getType());
        assertSame(STRING, getSchema(content.getField(service.encodeName("mime-type")).schema()).getType());
        assertSame(BYTES, getSchema(content.getField("data").schema()).getType());
        assertSame(LONG, getSchema(content.getField("length").schema()).getType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testComplexDocJsonCompare() throws IOException {
        DocumentType type = schemaManager.getDocumentType("ComplexDoc");
        Schema avro = service.createSchema(type);
        assertEquals(getContent("ComplexDocAvroSchema.json"), SchemaNormalization.toParsingForm(avro));
        assertEquals(getContent("ComplexDocAvroSchemaPretty.json"), avro.toString(true));
    }

    @Test
    public void testDocumentTypeFactory() {
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            Schema avro = service.createSchema(documentType);
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
        Schema avro = service.createSchema(nuxeo);
        assertNotNull(avro);
        for (String forbidden : FORBIDDEN) {
            String single = service.encodeName(forbidden);
            assertEquals(single, avro.getField(single).name());
            String black = service.encodeName(FIELD + forbidden + FIELD);
            assertEquals(black, avro.getField(black).name());
            String white = service.encodeName(forbidden + FIELD + forbidden);
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
            Schema avro = service.createSchema(nuxeo);
            assertNotNull(avro);
            assertEquals(nuxeo.getFields().size(), avro.getFields().size());
            // the toString(boolean pretty) call here is to assert no exception is thrown during the call
            // in some cases the schema can be built but not serialized
            avro.toString(true);
            deepAssert(nuxeo, avro);
        }
    }

    protected void deepAssert(org.nuxeo.ecm.core.schema.types.ComplexType nuxeo, Schema avro) {
        for (org.nuxeo.ecm.core.schema.types.Field nuxeoF : nuxeo.getFields()) {
            String cleanedNuxeoName = service.encodeName(nuxeoF.getName().getLocalName());
            Field avroF = avro.getField(cleanedNuxeoName);
            assertEquals(cleanedNuxeoName, avroF.name());
            if (nuxeoF.getType().isComplexType()) {
                deepAssert((ComplexType) nuxeoF.getType(), getSchema(avroF.schema()));
            } else {
                String actualAvroTypeName = getActualAvroNameType(avroF);
                String actualNuxeoTypeName = getActuelNuxeoNameType(nuxeoF.getType());
                assertEquals(actualNuxeoTypeName, actualAvroTypeName);
            }
        }
    }

    protected String getActualAvroNameType(Field avroF) {
        Schema schema = getSchema(avroF.schema());
        String actualAvroTypeName = schema.getLogicalType() != null
                ? schema.getLogicalType().getName()
                : schema.getName();
        if (actualAvroTypeName.equals("array") || actualAvroTypeName.equals("list")) {
            actualAvroTypeName = schema.getElementType().getName();
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
        return service.createSchema(nuxeo).getField("test__dash__test");
    }

    protected Schema getSchema(Schema schema) {
        if (schema.getType() == org.apache.avro.Schema.Type.UNION
                && schema.getTypes().get(0).getType() == org.apache.avro.Schema.Type.NULL) {
            return schema.getTypes().get(1);
        }
        return schema;
    }

}
