/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
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
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.scripting" })
@LocalDeploy({ "org.nuxeo.ecm.automation.test:operation-contrib.xml",
               "org.nuxeo.ecm.automation.test:chain-scripting-operation-contrib.xml" })
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRemoteAutomationScript {

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private PrintStream outStream;

    @Inject
    Session session;

    @Inject
    AutomationService service;

    @Inject
    HttpAutomationClient client;

    @Before
    public void setUpStreams() {
        outStream = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() throws IOException {
        outContent.close();
        System.setOut(outStream);
    }

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
        assertEquals("Simple" + System.lineSeparator(), outContent.toString());
    }

    @Test
    public void canHandleDocumentsSentRemotely() throws IOException {
        Documents documents = (Documents) session.newRequest("javascript.RemoteScriptWithDocs")
                                                 .setInput(getDocuments())
                                                 .execute();
        assertNotNull(documents);
        assertEquals("Simple" + System.lineSeparator(), outContent.toString());
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
