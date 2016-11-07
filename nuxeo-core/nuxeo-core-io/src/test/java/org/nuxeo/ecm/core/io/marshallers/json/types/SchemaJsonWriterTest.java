/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;

public class SchemaJsonWriterTest extends AbstractJsonWriterTest.Local<SchemaJsonWriter, Schema> {

    public SchemaJsonWriterTest() {
        super(SchemaJsonWriter.class, Schema.class);
    }

    @Inject
    private SchemaManager schemaManager;

    @Test
    public void test() throws Exception {
        Schema schema = schemaManager.getSchema("dublincore");
        JsonAssert json = jsonAssert(schema);
        json.properties(4);
        json.has("entity-type").isEquals("schema");
        json.has("name").isEquals("dublincore");
        json.has("prefix").isEquals("dc");
        json = json.has("fields").properties(schema.getFieldsCount());
        json.has("contributors").has("type").isEquals("string[]");
        json.has("coverage").has("type").isEquals("string");
        json.has("created").has("type").isEquals("date");
        json.has("creator").has("type").isEquals("string");
        json.has("description").has("type").isEquals("string");
        json.has("expired").has("type").isEquals("date");
        json.has("format").has("type").isEquals("string");
        json.has("issued").has("type").isEquals("date");
        json.has("language").has("type").isEquals("string");
        json.has("lastContributor").has("type").isEquals("string");
        json.has("modified").has("type").isEquals("date");
        json.has("nature").has("type").isEquals("string");
        json.has("publisher").has("type").isEquals("string");
        json.has("rights").has("type").isEquals("string");
        json.has("source").has("type").isEquals("string");
        json.has("subjects").has("type").isEquals("string[]");
        json.has("title").has("type").isEquals("string");
        json.has("valid").has("type").isEquals("date");
    }

    @Test
    public void testSchemaWithoutPrefix() throws Exception {
        Schema schema = schemaManager.getSchema("common");
        JsonAssert json = jsonAssert(schema);
        json.properties(3);
        json.hasNot("prefix");
    }

}
