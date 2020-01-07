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

import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.core.schema.FacetNames.HAS_RELATED_TEXT;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
public class FacetJsonWriterTest extends AbstractJsonWriterTest.Local<FacetJsonWriter, CompositeType> {

    public FacetJsonWriterTest() {
        super(FacetJsonWriter.class, CompositeType.class);
    }

    @Inject
    private SchemaManager schemaManager;

    @Test
    public void testFacetWithoutSchema() throws Exception {
        CompositeType type = schemaManager.getFacet(FOLDERISH);
        JsonAssert json = jsonAssert(type);
        json.properties(2);
        json.has("entity-type").isEquals("facet");
        json.has("name").isEquals(FOLDERISH);
    }

    @Test
    public void testFacetWithSchema() throws Exception {
        var schemaByFacetName = Map.of(HAS_RELATED_TEXT, "relatedtext", COLD_STORAGE, "coldstorage");
        for (Map.Entry<String, String> entry : schemaByFacetName.entrySet()) {
            CompositeType type = schemaManager.getFacet(entry.getKey());
            JsonAssert json = jsonAssert(type);
            json.properties(3);
            json.has("entity-type").isEquals("facet");
            json.has("name").isEquals(entry.getKey());
            json = json.has("schemas").length(1).has(0);
            json.has("entity-type").isEquals("schema");
            json.has("name").isEquals(entry.getValue());
        }
    }

}
