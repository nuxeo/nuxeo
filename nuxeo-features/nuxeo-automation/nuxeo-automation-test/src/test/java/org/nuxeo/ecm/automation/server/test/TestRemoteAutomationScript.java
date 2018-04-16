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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.automation.scripting")
@Deploy("org.nuxeo.ecm.automation.test.test:operation-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.test.test:chain-scripting-operation-contrib.xml")
@ServletContainer(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRemoteAutomationScript {

    @Inject
    Session session;

    @Inject
    AutomationService service;

    @Inject
    HttpAutomationClient client;

    protected Documents getDocuments() throws IOException {
        // Create a simple document
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        Document simple = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "File")
                                            .set("name", "Simple")
                                            .set("properties",
                                                    "dc:title=Simple\n")
                                            .execute();
        Documents docs = new Documents();
        docs.add(simple);
        docs.add(root);
        return docs;
    }

    @Test
    public void canHandleDocumentSentRemotely() throws IOException {
        Document document = (Document) session.newRequest("javascript.RemoteScriptWithDoc")
                                              .setInput(getDocuments().get(0))
                                              .execute();
        assertNotNull(document);
        assertEquals("Simple", document.getTitle());
    }

    @Test
    public void canHandleDocumentsSentRemotely() throws IOException {
        Documents documents = (Documents) session.newRequest("javascript.RemoteScriptWithDocs")
                                                 .setInput(getDocuments())
                                                 .execute();
        assertNotNull(documents);
        assertEquals(2, documents.size());
        assertEquals("Simple", documents.get(0).getTitle());
    }

    @Test
    public void testRemoteChainWithScriptingOp() throws Exception {
        OperationDocumentation opd = session.getOperation("testChain2");
        assertNotNull(opd);
        assertNotNull(service.getOperation("testChain2").getDocumentation().getOperations());
        Document doc = (Document) session.newRequest("testChain2").setInput(DocRef.newRef("/")).execute();
        assertNotNull(doc);

    }
}
