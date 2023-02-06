/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@Deploy("org.nuxeo.ecm.automation.test.test:test-page-provider.xml")
public class TestDocumentPageProviderOperation {

    @Inject
    protected HttpAutomationSession session;

    @Test
    public void testDocumentPageProviderOperation() throws IOException {
        List<JsonNode> documents = session.newRequest(DocumentPageProviderOperation.ID)
                                          .set("providerName", "PageProvider")
                                          .executeReturningDocuments();
        assertNotNull(documents);
        assertEquals(2, documents.size());

        documents = session.newRequest(DocumentPageProviderOperation.ID)
                           .set("providerName", "PageProvider")
                           .set("quickFilters", "SectionRoot")
                           .executeReturningDocuments();
        assertNotNull(documents);
        assertEquals(1, documents.size());
    }

    @Test
    public void testDocumentPageProviderOperationWithOffset() throws IOException {
        // retrieve first page in order to check if offset is well taken into account
        List<JsonNode> documents = session.newRequest(DocumentPageProviderOperation.ID)
                                          .set("providerName", "PageProvider")
                                          .executeReturningDocuments();
        assertNotNull(documents);

        int offset = 1;
        List<JsonNode> documentsOffset = session.newRequest(DocumentPageProviderOperation.ID)
                                                .set("providerName", "PageProvider")
                                                .set("offset", offset)
                                                .executeReturningDocuments();
        assertNotNull(documentsOffset);
        assertEquals(documents.get(offset), documentsOffset.get(0));
    }

    // NXP-31595
    @Test
    @WithFrameworkProperty(name = "org.nuxeo.web.ui.pageprovider.method", value = "POST")
    public void testNXQLWithDocumentPageProviderOperation() throws IOException {
        List<JsonNode> documents = session.newRequest(DocumentPageProviderOperation.ID)
                                          .set("providerName", "NXQLPageProvider")
                                          .set("queryParams", "Select * FROM Domain, SectionRoot")
                                          .executeReturningDocuments();
        assertNotNull(documents);
        assertEquals(2, documents.size());
        assertTrue(documents.stream().anyMatch(doc -> "Domain".equals(doc.get("type").asText())));
        assertTrue(documents.stream().anyMatch(doc -> "SectionRoot".equals(doc.get("type").asText())));

        // Check we do not break other page provider with org.nuxeo.web.ui.pageprovider.method=POST
        documents = session.newRequest(DocumentPageProviderOperation.ID)
                           .set("providerName", "PageProvider")
                           .executeReturningDocuments();

        assertNotNull(documents);
        documents = session.newRequest(DocumentPageProviderOperation.ID)
                           .set("providerName", "QuickFilterPageProvider")
                           .executeReturningDocuments();
        assertNotNull(documents);
    }
}
