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

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;

import javax.inject.Inject;

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

    @Test
    public void testSchemaWithoutPrefix() throws Exception {
        Schema schema = schemaManager.getSchema("common");
        JsonAssert json = jsonAssert(schema);
        json.properties(3);
        json.hasNot("prefix");
    }

}
