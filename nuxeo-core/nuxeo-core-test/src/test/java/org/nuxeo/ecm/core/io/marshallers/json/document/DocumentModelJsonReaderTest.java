/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.JsonFactoryProvider;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/defaultvalue-docTypes.xml")
public class DocumentModelJsonReaderTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public DocumentModelJsonReaderTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myNote", "Note");
        document.setPropertyValue("dc:title", "My Note");
        document = session.createDocument(document);
    }

    @Test
    public void testDefault() throws Exception {
        RenderingContext renderingContext = CtxBuilder.get();
        renderingContext.setExistingSession(session);
        DocumentModelJsonReader reader = registry.getInstance(renderingContext, DocumentModelJsonReader.class);
        JsonAssert json = jsonAssert(document);
        DocumentModel doc = reader.read(json.getNode());
        assertTrue(doc instanceof DocumentModelImpl);
        assertEquals("myNote", doc.getName());
        assertEquals("My Note", doc.getPropertyValue("dc:title"));
    }

    @Test
    public void testReadSchemaWithoutPrefix() throws IOException {
        String noteJson = "{ \"entity-type\": \"document\",\n" + //
                "  \"type\": \"Note\",\n" + //
                "  \"name\": \"aNote\",\n" + //
                "  \"properties\":\n" + //
                "   { \"dc:title\": \"A note\",\n" + //
                "     \"note:note\": \"note content\" } }";

        DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(noteJson);
        JsonNode jn = jp.readValueAsTree();
        DocumentModel noteDocument = reader.read(jn);
        assertNotNull(noteDocument);
        assertTrue(noteDocument instanceof SimpleDocumentModel);
        assertEquals("note content", noteDocument.getPropertyValue("note:note"));
    }

    @Test
    public void testScalarCreatedWithDefaultValue() throws Exception {
        // given a doc json with a property with a default value not modified
        String noteJson = "{ \"entity-type\": \"document\", \"type\": \"DocDefaultValue\", \"name\": \"aDoc\" }";

        // when I parse it it
        DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(noteJson);
        JsonNode jn = jp.readValueAsTree();
        DocumentModel noteDocument = reader.read(jn);

        // then the default value must be set
        String[] schemas = noteDocument.getSchemas();
        assertEquals(1, schemas.length);
        assertEquals("defaultvalue", schemas[0]);
        Map<String, Object> values = noteDocument.getDataModel("defaultvalue").getMap();
        assertNull(null, values.get("dv:simpleWithoutDefault"));
        assertEquals("value", values.get("dv:simpleWithDefault"));
    }

    @Test
    public void testScalarSetOnNullDontSetDefaultValueAgain() throws Exception {
        // given a doc json with a property with a default value set to null
        String noteJson = "{ \"entity-type\": \"document\", \"type\": \"DocDefaultValue\", \"name\": \"aDoc\", \"properties\": {\"dv:simpleWithDefault\":null} }";

        // when I parse it it
        DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(noteJson);
        JsonNode jn = jp.readValueAsTree();
        DocumentModel noteDocument = reader.read(jn);

        // then the property with the default value must null
        Map<String, Object> values = noteDocument.getDataModel("defaultvalue").getMap();
        assertNull(values.get("dv:simpleWithDefault"));
    }

    @Test
    public void testMultiCreatedWithDefaultValue() throws Exception {
        // given a doc json with a property with a default value not modified
        String noteJson = "{ \"entity-type\": \"document\", \"type\": \"DocDefaultValue\", \"name\": \"aDoc\" }";

        // when I parse it
        DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(noteJson);
        JsonNode jn = jp.readValueAsTree();
        DocumentModel noteDocument = reader.read(jn);

        // then the default value must be set
        String[] schemas = noteDocument.getSchemas();
        assertEquals(1, schemas.length);
        assertEquals("defaultvalue", schemas[0]);
        Map<String, Object> values = noteDocument.getDataModel("defaultvalue").getMap();
        assertNull(null, values.get("dv:multiWithoutDefault"));
        assertArrayEquals(new String[] { "value1", "value2" }, (String[]) values.get("dv:multiWithDefault"));
    }

    @Test
    public void testMultiSetOnNullDontSetDefaultValueAgain() throws Exception {
        // given a doc json with a property with a default value not modified
        String noteJson = "{ \"entity-type\": \"document\", \"type\": \"DocDefaultValue\", \"name\": \"aDoc\", \"properties\": {\"dv:multiWithDefault\":null} }";

        // when I parse it
        DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(noteJson);
        JsonNode jn = jp.readValueAsTree();
        DocumentModel noteDocument = reader.read(jn);

        // then the property with the default value must null
        Map<String, Object> values = noteDocument.getDataModel("defaultvalue").getMap();
        assertNull(values.get("dv:multiWithDefault"));
    }

    // NXP-30680
    // NXP-30806
    // NXP-31199
    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testPropertyValuePossibilities() throws Exception {
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": \"Some string\"}", "my:string", "Some string");
        testPropertyWithAcceptedRepresentationWorks(String.format("{\"my:string\": %s}", Long.MAX_VALUE), "my:string",
                String.valueOf(Long.MAX_VALUE));
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": 1234}", "my:string", "1234");
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": 12.34}", "my:string", "12.34");
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": true}", "my:string", "true");
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": null}", "my:string", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:string\": \"\"}", "my:string", "");
        testPropertyWithWrongRepresentationThrowsException("{\"my:string\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:string\": [0]}");

        // numbers are always handled as Long in Nuxeo
        testPropertyWithAcceptedRepresentationWorks("{\"my:integer\": 1234}", "my:integer", 1234L);
        testPropertyWithAcceptedRepresentationWorks(String.format("{\"my:integer\": %s}", Long.MAX_VALUE), "my:integer",
                Long.MAX_VALUE);
        testPropertyWithAcceptedRepresentationWorks("{\"my:integer\": \"1234\"}", "my:integer", 1234L);
        testPropertyWithAcceptedRepresentationWorks("{\"my:integer\": null}", "my:integer", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:integer\": \"\"}", "my:integer", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": \"12.34\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:integer\": [0]}");

        testPropertyWithAcceptedRepresentationWorks(String.format("{\"my:long\": %s}", Long.MAX_VALUE), "my:long",
                Long.MAX_VALUE);
        testPropertyWithAcceptedRepresentationWorks("{\"my:long\": 1234}", "my:long", 1234L);
        testPropertyWithAcceptedRepresentationWorks("{\"my:long\": \"1234\"}", "my:long", 1234L);
        testPropertyWithAcceptedRepresentationWorks("{\"my:long\": null}", "my:long", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:long\": \"\"}", "my:long", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": \"12.34\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:long\": [0]}");

        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": true}", "my:boolean", true);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": \"true\"}", "my:boolean", true);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": \"Some string\"}", "my:boolean", false);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": 1234}", "my:boolean", true);
        testPropertyWithAcceptedRepresentationWorks(String.format("{\"my:boolean\": %s}", Long.MAX_VALUE), "my:boolean",
                true);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": 1}", "my:boolean", true);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": 0}", "my:boolean", false);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": null}", "my:boolean", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:boolean\": \"\"}", "my:boolean", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:boolean\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:boolean\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:boolean\": [0]}");

        testPropertyWithAcceptedRepresentationWorks("{\"my:double\": 1234}", "my:double", 1234.0);
        testPropertyWithAcceptedRepresentationWorks(String.format("{\"my:double\": %s}", Long.MAX_VALUE), "my:double",
                Long.valueOf(Long.MAX_VALUE).doubleValue());
        testPropertyWithAcceptedRepresentationWorks("{\"my:double\": 12.34}", "my:double", 12.34);
        testPropertyWithAcceptedRepresentationWorks("{\"my:double\": \"12.34\"}", "my:double", 12.34);
        testPropertyWithAcceptedRepresentationWorks("{\"my:double\": null}", "my:double", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:double\": \"\"}", "my:double", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:double\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:double\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:double\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:double\": [0]}");

        Date date = DateParser.parseW3CDateTime("2022-01-18T17:20:21.123");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        testPropertyWithAcceptedRepresentationWorks("{\"my:date\": \"2022-01-18T17:20:21.123\"}", "my:date", cal);
        testPropertyWithAcceptedRepresentationWorks("{\"my:date\": null}", "my:date", null);
        testPropertyWithAcceptedRepresentationWorks("{\"my:date\": \"\"}", "my:date", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": 1234}");
        testPropertyWithWrongRepresentationThrowsException(String.format("{\"my:date\": %s}", Long.MAX_VALUE));
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": {\"key\": true}}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:date\": [0]}");

        testPropertyWithAcceptedRepresentationWorks("{\"my:strings\": [\"Some string\"]}", "my:strings",
                new String[] { "Some string" });
        testPropertyWithAcceptedRepresentationWorks("{\"my:strings\": null}", "my:strings", null);
        testPropertyWithWrongRepresentationThrowsException("{\"my:strings\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:strings\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:strings\": 1234}");
        testPropertyWithWrongRepresentationThrowsException(String.format("{\"my:strings\": %s}", Long.MAX_VALUE));
        testPropertyWithWrongRepresentationThrowsException("{\"my:strings\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:strings\": {\"key\": true}}");
        testPropertyWithAcceptedRepresentationWorks("{\"my:longs\": []}", "my:longs", new Long[] {});
        testPropertyWithAcceptedRepresentationWorks("{\"my:longs\": [1, 2, 3]}", "my:longs", new Long[] { 1L, 2L, 3L });
        testPropertyWithAcceptedRepresentationWorks("{\"my:longs\": [2147483648, 9223372036854775807]}", "my:longs",
                new Long[] { 2147483648L, 9223372036854775807L });
        testPropertyWithAcceptedRepresentationWorks("{\"my:integers\": []}", "my:integers", new Long[] {});
        testPropertyWithAcceptedRepresentationWorks("{\"my:integers\": [4, 5, 6]}", "my:integers",
                new Long[] { 4L, 5L, 6L });
        testPropertyWithAcceptedRepresentationWorks("{\"my:doubles\": []}", "my:doubles", new Double[] {});
        testPropertyWithAcceptedRepresentationWorks("{\"my:doubles\": [7, 8, 9]}", "my:doubles",
                new Double[] { 7D, 8D, 9D });
        testPropertyWithAcceptedRepresentationWorks("{\"my:doubles\": [7.8, 8.8, 9.8]}", "my:doubles",
                new Double[] { 7.8D, 8.8D, 9.8D });
        testPropertyWithAcceptedRepresentationWorks("{\"my:doubles\": [9223372036854775807]}", "my:doubles",
                new Double[] { 9223372036854775807D });

        // complex
        Map<String, String> map = new HashMap<>();
        map.put("FirstName", "foo");
        map.put("LastName", "bar");
        testPropertyWithAcceptedRepresentationWorks("{\"my:name\": {\"FirstName\":\"foo\", \"LastName\":\"bar\"}}",
                "my:name", map);
        testPropertyWithAcceptedRepresentationWorks("{\"my:name\": null}", "my:name", Collections.emptyMap());
        testPropertyWithWrongRepresentationThrowsException("{\"my:name\": \"Some string\"}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:name\": true}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:name\": 1234}");
        testPropertyWithWrongRepresentationThrowsException(String.format("{\"my:name\": %s}", Long.MAX_VALUE));
        testPropertyWithWrongRepresentationThrowsException("{\"my:name\": 12.34}");
        testPropertyWithWrongRepresentationThrowsException("{\"my:name\": [0]}");
    }

    protected void testPropertyWithWrongRepresentationThrowsException(String properties)
            throws IOException {
        String json = '{' + //
                "\"entity-type\": \"document\", " + //
                "\"type\": \"MyDocType\", " + //
                "\"name\": \"myDoc\", " + //
                "\"properties\": " + properties + " " + //
                '}';
        try (JsonParser jp = JsonFactoryProvider.get().createParser(json)) {
            JsonNode jn = jp.readValueAsTree();

            DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
            reader.read(jn);
            fail("Read should have failed due to wrong type");
        } catch (NuxeoException e) {
            assertTrue(e instanceof MarshallingException);
            assertTrue(e.getMessage().startsWith("Unable to deserialize property:"));
        }
    }

    protected void testPropertyWithAcceptedRepresentationWorks(String properties, String expectedProperty,
            Object expectedValue) throws IOException {
        String json = '{' + //
                "\"entity-type\": \"document\", " + //
                "\"type\": \"MyDocType\", " + //
                "\"name\": \"myDoc\", " + //
                "\"properties\": " + properties + " " + //
                '}';
        try (JsonParser jp = JsonFactoryProvider.get().createParser(json)) {
            JsonNode jn = jp.readValueAsTree();

            DocumentModelJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentModelJsonReader.class);
            DocumentModel doc = reader.read(jn);
            if (expectedValue instanceof Object[]) {
                assertArrayEquals((Object[]) expectedValue, (Object[]) doc.getPropertyValue(expectedProperty));
            } else {
                assertEquals(expectedValue, doc.getPropertyValue(expectedProperty));
            }
        }
    }
}
