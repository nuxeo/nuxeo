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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.automation.scripting")
@Deploy("org.nuxeo.ecm.automation.test.test:operation-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.test.test:chain-scripting-operation-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRemoteAutomationScript {

    @Inject
    HttpAutomationSession session;

    @Inject
    AutomationService service;

    protected List<JsonNode> getDocuments() throws IOException {
        // Create a simple document
        JsonNode root = session.newRequest(FetchDocument.ID) //
                               .set("value", "/")
                               .executeReturningDocument();
        JsonNode simple = session.newRequest(CreateDocument.ID)
                                 .setInput(root)
                                 .set("type", "File")
                                 .set("name", "Simple")
                                 .set("properties", "dc:title=Simple\n")
                                 .executeReturningDocument();
        return Arrays.asList(simple, root);
    }

    @Test
    public void canHandleDocumentSentRemotely() throws IOException {
        JsonNode document = session.newRequest("javascript.RemoteScriptWithDoc")
                                   .setInput(getDocuments().get(0))
                                   .executeReturningDocument();
        assertNotNull(document);
        assertEquals("Simple", document.get("title").asText());
    }

    @Test
    public void canHandleDocumentsSentRemotely() throws IOException {
        List<JsonNode> documents = session.newRequest("javascript.RemoteScriptWithDocs") //
                                          .setInput(getDocuments())
                                          .executeReturningDocuments();
        assertNotNull(documents);
        assertEquals(2, documents.size());
        assertEquals("Simple", documents.get(0).get("title").asText());
    }

    @Test
    public void testRemoteChainWithScriptingOp() throws IOException {
        JsonNode doc = session.newRequest("testChain2") //
                              .setInput("doc:/")
                              .executeReturningDocument();
        assertNotNull(doc);
    }

    @Test
    public void canCallHtmlEscapeViaJS() throws IOException {
        String escaped = session.newRequest("javascript.testHtmlEscape") //
                                .setInput(" cou&cou ")
                                .executeReturningStringEntity();
        assertNotNull(escaped);
        assertEquals(" cou&amp;cou ", escaped);
    }

    @Test
    public void canCallNxqlEscapeViaJS() throws IOException {
        String escaped = session.newRequest("javascript.testNxqlEscape") //
                                .setInput(" \n ")
                                .executeReturningStringEntity();
        assertNotNull(escaped);
        assertEquals(" \\n ", escaped);
    }
}
