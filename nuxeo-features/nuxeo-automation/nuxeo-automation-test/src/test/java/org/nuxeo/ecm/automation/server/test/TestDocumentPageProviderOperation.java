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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@Deploy("org.nuxeo.ecm.automation.test.test:test-page-provider.xml")
public class TestDocumentPageProviderOperation {

    @Inject
    protected Session session;

    @Inject
    protected HttpAutomationClient client;

    @Test
    public void testDocumentPageProviderOperation() throws Exception {
        Documents documents = (Documents) session.newRequest(DocumentPageProviderOperation.ID)
                                                 .set("providerName", "PageProvider")
                                                 .execute();
        assertNotNull(documents);
        assertEquals(2, documents.size());
        documents = (Documents) session.newRequest(DocumentPageProviderOperation.ID)
                                       .set("providerName", "PageProvider")
                                       .set("quickFilters", "SectionRoot")
                                       .execute();
        assertNotNull(documents);
        assertEquals(1, documents.size());
    }

    @Test
    public void testDocumentPageProviderOperationWithOffset() throws IOException {
        // retrieve first page in order to check if offset is well taken into account

        Documents documents = (Documents) session.newRequest(DocumentPageProviderOperation.ID)
                                                 .set("providerName", "PageProvider")
                                                 .set("pageSize", "4")
                                                 .execute();
        assertNotNull(documents);
        int offset = 1;
        Documents documentsOffset = (Documents) session.newRequest(DocumentPageProviderOperation.ID)
                                                       .set("providerName", "PageProvider")
                                                       .set("offset", offset)
                                                       .execute();
        assertNotNull(documentsOffset);
        assertEquals(documents.get(offset + 1).toString(), documentsOffset.get(1).toString());
    }
}
