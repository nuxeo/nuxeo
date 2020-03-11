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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class SchemaListJsonWriterTest extends AbstractJsonWriterTest.Local<SchemaListJsonWriter, List<Schema>> {

    public SchemaListJsonWriterTest() {
        super(SchemaListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, Schema.class));
    }

    @Inject
    private SchemaManager schemaManager;

    public List<Schema> getElements() {
        return Arrays.asList(schemaManager.getSchema("dublincore"), schemaManager.getSchema("common"));
    }

    @Test
    public void test() throws Exception {
        List<Schema> elements = getElements();
        JsonAssert json = jsonAssert(elements);
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("schemas");
        json = json.has("entries").length(elements.size());
        json.childrenContains("entity-type", "schema", "schema");
        json.childrenContains("name", "dublincore", "common");
    }

}
