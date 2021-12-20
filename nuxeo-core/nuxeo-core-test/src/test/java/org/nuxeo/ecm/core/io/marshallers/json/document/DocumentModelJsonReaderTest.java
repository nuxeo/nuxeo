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
    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
    public void testSetScalarWithWrongTypeThrowsException() throws Exception {
        testSetScalarWithWrongTypeThrowsException("{\"my:integer\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:integer\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:integer\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:integer\": {\"key\": true}}");
        testSetScalarWithWrongTypeThrowsException("{\"my:integer\": [0]}");

        testSetScalarWithWrongTypeThrowsException("{\"my:long\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:long\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:long\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:long\": {\"key\": true}}");
        testSetScalarWithWrongTypeThrowsException("{\"my:long\": [0]}");

        testSetScalarWithWrongTypeThrowsException("{\"my:boolean\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:boolean\": 1234}");
        testSetScalarWithWrongTypeThrowsException("{\"my:boolean\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:boolean\": {\"key\": true}}");
        testSetScalarWithWrongTypeThrowsException("{\"my:boolean\": [0]}");

        testSetScalarWithWrongTypeThrowsException("{\"my:double\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:double\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:double\": {\"key\": true}}");
        testSetScalarWithWrongTypeThrowsException("{\"my:double\": [0]}");

        testSetScalarWithWrongTypeThrowsException("{\"my:date\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:date\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:date\": 1234}");
        testSetScalarWithWrongTypeThrowsException("{\"my:date\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:date\": {\"key\": true}}");
        testSetScalarWithWrongTypeThrowsException("{\"my:date\": [0]}");

        testSetScalarWithWrongTypeThrowsException("{\"my:strings\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:strings\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:strings\": 1234}");
        testSetScalarWithWrongTypeThrowsException("{\"my:strings\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:strings\": {\"key\": true}}");

        // complex
        testSetScalarWithWrongTypeThrowsException("{\"my:name\": \"Some string\"}");
        testSetScalarWithWrongTypeThrowsException("{\"my:name\": true}");
        testSetScalarWithWrongTypeThrowsException("{\"my:name\": 1234}");
        testSetScalarWithWrongTypeThrowsException("{\"my:name\": 12.34}");
        testSetScalarWithWrongTypeThrowsException("{\"my:name\": [0]}");
    }

    protected void testSetScalarWithWrongTypeThrowsException(String properties)
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
            assertTrue(e.getMessage().startsWith("Wrong type for property:"));
        }
    }
}
