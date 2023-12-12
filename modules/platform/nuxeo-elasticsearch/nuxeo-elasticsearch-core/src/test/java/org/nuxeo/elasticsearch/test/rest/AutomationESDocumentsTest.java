/*
 * (C) Copyright 20202 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.elasticsearch.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.operations.DocumentInputResolver;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider2-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:pageprovider2-coretype-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:test-other-repository-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core.test:test-operations-multi-repositories.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AutomationESDocumentsTest {

    protected static final String SEARCH_ALL_REPOSITORIES_PP = "SEARCH_ALL_REPOSITORIES_PP";

    @Inject
    protected CoreSession session;

    @Inject
    protected HttpAutomationSession clientSession;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected TransactionalFeature txFeature;

    protected CoreSession sessionOther;

    protected DocumentModel docOther;

    @Before
    public void initRepo() {
        sessionOther = CoreInstance.getCoreSession("other");

        docOther = sessionOther.createDocumentModel("/", "folder_0", "Folder");
        docOther.setPropertyValue("dc:title", "Other repository folder");
        docOther = sessionOther.createDocument(docOther);
        txFeature.nextTransaction();
    }

    @After
    public void tearDown() {
        sessionOther.removeDocument(docOther.getRef());
    }

    @Test
    public void iCanPerformESQLPageProviderOperationOnRepository() throws Exception {
        Properties namedParameters = new Properties(Map.of("defaults:dc_nature_agg", "[\"article\"]"));
        JsonNode node = clientSession.newRequest(DocumentPageProviderOperation.ID)
                                     .set("namedParameters", namedParameters)
                                     .set("providerName", "default_search")
                                     .execute();
        assertEquals(node.get("pageSize").asInt(), 20);
        assertEquals(node.get("resultsCount").asInt(), 11);
    }

    @Test
    public void iCanSkipAggregatesOnESQLPageProviderOperationOnRepository() throws Exception {
        JsonNode node = clientSession.newRequest(DocumentPageProviderOperation.ID)
                                     .set("providerName", "aggregates_1")
                                     .execute();
        assertTrue(node.has("aggregations"));

        node = clientSession.newRequest(DocumentPageProviderOperation.ID)
                            .setHeader(PageProvider.SKIP_AGGREGATES_PROP, "true")
                            .set("providerName", "aggregates_1")
                            .execute();
        assertFalse(node.has("aggregations"));
    }

    // NXP-31487
    @Test
    @WithFrameworkProperty(name = DocumentInputResolver.BULK_DOWNLOAD_MULTI_REPOSITORIES, value = "true")
    public void iCanCallAutomationOnMultiRepositoryPageProviderResults() throws IOException {

        // call a PageProvder configured with the "searchAllRepositories" property to true
        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(SEARCH_ALL_REPOSITORIES_PP);
        assertNotNull(ppdef);
        var props = Map.of(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        var parameter = "/folder_0";
        PageProvider<?> pp = pageProviderService.getPageProvider(SEARCH_ALL_REPOSITORIES_PP, ppdef, null, null, 10L, 0L,
                props, parameter);

        @SuppressWarnings("unchecked")
        var page = (List<DocumentModel>) pp.getCurrentPage();
        assertEquals(2, pp.getResultsCount());
        assertNotNull(page);
        assertEquals(2, page.size());
        // document results are sorted by ascending dc:title
        // document from "test" repository
        DocumentModel docTestRepo = page.get(0);
        DocumentModel expected = session.getDocument(new PathRef("/folder_0"));
        checkDocumentModel(docTestRepo, "/folder_0", expected.getId(), "test", "Folder 0");
        // document from "other" repository
        DocumentModel docOtherRepo = page.get(1);
        checkDocumentModel(docOtherRepo, "/folder_0", docOther.getId(), "other", "Other repository folder");

        // call an operation with a list of documents from multiple repositories as an input
        // the input expected form is: "docs:repo1:docPath1,repo2:docPath2"
        var docRef1 = String.join(":", docTestRepo.getRepositoryName(), docTestRepo.getPathAsString());
        var docRef2 = String.join(":", docOtherRepo.getRepositoryName(), docOtherRepo.getPathAsString());
        var input = String.join(":", "docs", String.join(",", docRef1, docRef2));
        JsonNode node = clientSession.newRequest(MultiRepositoryDummyOperation.ID).setInput(input).execute();
        assertEquals("documents", node.get("entity-type").asText());
        JsonNode entries = node.get("entries");
        assertEquals(2, entries.size());
        checkDocumentJsonNode(entries.get(0), "/folder_0", docTestRepo.getId(), "test", "Folder 0");
        checkDocumentJsonNode(entries.get(1), "/folder_0", docOtherRepo.getId(), "other", "Other repository folder");
    }

    protected void checkDocumentModel(DocumentModel doc, String path, String id, String repositoryName, String title) {
        assertEquals(path, doc.getPathAsString());
        assertEquals(id, doc.getId());
        assertEquals(repositoryName, doc.getRepositoryName());
        assertEquals(title, doc.getTitle());
    }

    protected void checkDocumentJsonNode(JsonNode document, String path, String id, String repositoryName,
            String title) {
        assertEquals("document", document.get("entity-type").asText());
        assertEquals(path, document.get("path").asText());
        assertEquals(id, document.get("uid").asText());
        assertEquals(repositoryName, document.get("repository").asText());
        assertEquals(title, document.get("title").asText());
    }

}
