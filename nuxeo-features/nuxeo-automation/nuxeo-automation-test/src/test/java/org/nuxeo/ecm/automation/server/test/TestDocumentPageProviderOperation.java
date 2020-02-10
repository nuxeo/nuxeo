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
}
