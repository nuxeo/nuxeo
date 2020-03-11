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

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
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
        json.properties(5);
        json.has("entity-type").isEquals("schema");
        json.has("name").isEquals("dublincore");
        json.has("prefix").isEquals("dc");
        json = json.has("fields").properties(schema.getFieldsCount());
        json.has("contributors").isEquals("string[]");
        json.has("coverage").isEquals("string");
        json.has("created").isEquals("date");
        json.has("creator").isEquals("string");
        json.has("description").isEquals("string");
        json.has("expired").isEquals("date");
        json.has("format").isEquals("string");
        json.has("issued").isEquals("date");
        json.has("language").isEquals("string");
        json.has("lastContributor").isEquals("string");
        json.has("modified").isEquals("date");
        json.has("nature").isEquals("string");
        json.has("publisher").isEquals("string");
        json.has("rights").isEquals("string");
        json.has("source").isEquals("string");
        json.has("subjects").isEquals("string[]");
        json.has("title").isEquals("string");
        json.has("valid").isEquals("date");
    }

    /**
     * @since 9.1
     */
    @Test
    public void testWithFetchFields() throws IOException {
        Schema schema = schemaManager.getSchema("dublincore");
        JsonAssert json = jsonAssert(schema,
                CtxBuilder.fetch(SchemaJsonWriter.ENTITY_TYPE, SchemaJsonWriter.FETCH_FIELDS).get());
        json = json.has("fields").properties(schema.getFieldsCount());
        json.has("contributors").get("type").isEquals("string[]");
        json.has("contributors").get("constraints").isArray();
    }

    @Test
    public void testSchemaWithoutPrefix() throws Exception {
        Schema schema = schemaManager.getSchema("common");
        JsonAssert json = jsonAssert(schema);
        json.properties(3);
        json.hasNot("prefix");
    }

}
