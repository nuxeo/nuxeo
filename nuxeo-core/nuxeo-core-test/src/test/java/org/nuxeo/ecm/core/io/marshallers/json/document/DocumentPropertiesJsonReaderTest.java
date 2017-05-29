/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.junit.Test;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonFactoryProvider;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.types.Schema;

public class DocumentPropertiesJsonReaderTest
        extends AbstractJsonWriterTest.Local<DocumentPropertyJsonWriter, Property> {

    public DocumentPropertiesJsonReaderTest() {
        super(DocumentPropertyJsonWriter.class, List.class, TypeUtils.parameterize(List.class, Property.class));
    }

    /*
     * NXP-22436
     */
    @Test
    public void testReadSchemaWithoutPrefix() throws IOException {
        String propertiesJson = "{ \"dc:title\": \"A note\",\n" + //
                "  \"note:note\": \"note content\" }";

        DocumentPropertiesJsonReader reader = registry.getInstance(CtxBuilder.get(), DocumentPropertiesJsonReader.class);
        JsonParser jp = JsonFactoryProvider.get().createJsonParser(propertiesJson);
        JsonNode jn = jp.readValueAsTree();
        List<Property> properties = reader.read(jn);
        assertNotNull(properties);
        assertEquals(2, properties.size());
        Schema titleSchema = properties.get(0).getSchema();
        assertNotNull(titleSchema);
        assertEquals("dublincore", titleSchema.getName());
        Schema noteSchema = properties.get(1).getSchema();
        assertNotNull(noteSchema);
        assertEquals("note", noteSchema.getName());
    }

}
