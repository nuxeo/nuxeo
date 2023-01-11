/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.search.core;

import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReaderTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonFactoryProvider;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

@Features(CoreFeature.class)
public class SavedSearchReaderTest extends AbstractJsonReaderTest.Local<SavedSearchRequestReader, SavedSearchRequest> {

    public SavedSearchReaderTest() {
        super(SavedSearchRequestReader.class, SavedSearchRequest.class);
    }

    // NXP-31456
    @Test
    public void testReadRequestWithNullParameter() throws IOException {
        String params = "{\"entity-type\":\"savedSearch\",\"params\":{\"dublincore_modified\":null}}";

        SavedSearchRequestReader reader = registry.getInstance(CtxBuilder.get(), SavedSearchRequestReader.class);
        SavedSearchRequest ssr;
        try (JsonParser jp = JsonFactoryProvider.get().createParser(params)) {
            JsonNode jn = jp.readValueAsTree();
            ssr = reader.read(jn);
        }

        assertNull(ssr.getNamedParams().get("dublincore_modified"));
    }
}
