/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.platform.search.core;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.search.core")
public class SavedSearchWriterTest extends AbstractJsonWriterTest.External<SavedSearchWriter, SavedSearch> {

    public static final String SEARCH_TYPE_NAME = "DefaultSearch";

    public static final String SEACH_TITLE = "My Search";

    public SavedSearchWriterTest() {
        super(SavedSearchWriter.class, SavedSearch.class);
    }

    protected SavedSearch search;

    @Inject
    protected CoreSession session;

    @Before
    public void setup() {
        DocumentModel searchDocument = session.createDocumentModel("/", "mySearch", SEARCH_TYPE_NAME);
        searchDocument = session.createDocument(searchDocument);
        search = searchDocument.getAdapter(SavedSearch.class);
        search.setTitle(SEACH_TITLE);
    }

    @Test
    public void testDefault() throws Exception {
        JsonAssert json = jsonAssert(search);
        json.isObject();
        json.properties(14);
        json.has("entity-type").isEquals("savedSearch");
        json.has("id").isEquals(search.getId());
        json.has("title").isEquals(SEACH_TITLE);
        json.has("query").isNull();
        json.has("queryParams").isNull();
        json.has("queryLanguage").isNull();
        json.has("pageProviderName").isNull();
        json.has("pageSize").isNull();
        json.has("currentPageIndex").isNull();
        json.has("maxResults").isNull();
        json.has("sortBy").isNull();
        json.has("sortOrder").isNull();
        json.has("contentViewData").isNull();
        json.has("params").isObject().properties(0);
    }

    @Test
    public void testWithProperties() throws Exception {
        JsonAssert json = jsonAssert(search, RenderingContext.CtxBuilder.properties("saved_search").get());
        json.isObject();
        json.properties(14);
        json.has("entity-type").isEquals("savedSearch");
        json.has("params").isObject().properties(12);

        json = jsonAssert(search, RenderingContext.CtxBuilder.properties("*").get());
        json.isObject();
        json.properties(14);
        json.has("entity-type").isEquals("savedSearch");
        JsonAssert properties = json.has("params").isObject();
        assertTrue(properties.getNode().size() > 12);
        properties.get("saved:providerName").isNull();
    }

}
