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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.AsyncSession;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.services.bulk.AutomationBulkAction;
import org.nuxeo.ecm.automation.core.operations.services.bulk.BulkRunAction;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.automation.test:operation-contrib.xml")
@Deploy("org.nuxeo.ecm.automation.test:test-bindings.xml")
@Deploy("org.nuxeo.ecm.automation.test.test:test-page-provider.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class AsyncOperationAdapterTest {

    @Inject
    protected Session session;

    @Inject
    protected AsyncSession async;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void testAsyncOperation() throws Exception {
        Object r;

        r = async.newRequest(ReturnOperation.ID).setInput(Boolean.TRUE).execute();
        assertThat(r, is(Boolean.TRUE));

        // Document
        r = async.newRequest(FetchDocument.ID).set("value", "/").execute();
        assertThat(r, instanceOf(Document.class));

        // Documents
        r = async.newRequest(DocumentPaginatedQuery.ID).set("query", "SELECT * from Document").execute();
        assertThat(r, instanceOf(Documents.class));

        // Blob
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();

        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "Folder")
                                            .set("name", "asyncTest")
                                            .execute();

        Document file = (Document) session.newRequest(CreateDocument.ID)
                                          .setInput(folder)
                                          .set("type", "File")
                                          .set("name", "blobs")
                                          .execute();

        File tmp = Framework.createTempFile("async-operation-test-", ".xml");
        FileUtils.writeStringToFile(tmp, "<doc>mydoc1</doc>", UTF_8);

        FileBlob fb = new FileBlob(tmp);
        fb.setMimeType("text/xml");

        FileBlob blob = (FileBlob) session.newRequest(AttachBlob.ID)
                                          .setHeader(Constants.HEADER_NX_VOIDOP, "true")
                                          .setInput(fb)
                                          .set("document", file.getPath())
                                          .execute();

        r = async.newRequest(GetDocumentBlob.ID).setInput(file).execute();
        assertThat(r, instanceOf(Blob.class));
    }

    @Test
    public void testAsyncChain() throws Exception {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "Folder")
                                            .set("name", "chainTest")
                                            .execute();

        Document doc = (Document) async.newRequest("testchain").setInput(folder).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());
    }

    @Test
    public void testError() throws Exception {
        Object r = session.newRequest("Test.Exit").setInput("Error").execute();
        assertEquals("Error", r);

        try {
            r = async.newRequest("Test.Exit").set("error", true).set("rollback", true).execute();
            fail("expected error");
        } catch (RemoteException e) {
            assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getStatus());
            assertEquals("Failed to invoke operation Test.Exit", e.getMessage());
        }
    }

    @Test
    public void testAsyncBulkAction() throws Exception {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "Folder")
                                            .set("name", "test")
                                            .execute();

        // param for the automation operation
        HashMap<String, Serializable> automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=foo");

        // param for the automation bulk action
        HashMap<String, Serializable> actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, automationParams);

        Object r = async.newRequest(BulkRunAction.ID)
                        .set("action", AutomationBulkAction.ACTION_NAME)
                        .set("query", "SELECT * FROM Folder")
                        .set("bucketSize", "10")
                        .set("batchSize", "5")
                        .set("parameters", (new ObjectMapper()).writeValueAsString(actionParams))
                        .execute();
        assertNotNull(r);

        txFeature.nextTransaction();

        folder = (Document) session.newRequest(FetchDocument.ID).set("value", "/test").execute();

        assertEquals("foo", folder.getTitle());
    }

    /**
     * @throws IOException
     * @since 11.1
     */
    @Test
    public void testAsyncBulkActionWithPP() throws IOException {
        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        // create a folder and a file
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "Folder")
                                            .set("name", "test")
                                            .execute();
        Document file = (Document) session.newRequest(CreateDocument.ID)
                                          .setInput(folder)
                                          .set("type", "File")
                                          .set("name", "file")
                                          .execute();

        // param for the automation operation
        Map<String, Serializable> automationParams = new HashMap<>();
        automationParams.put("properties", "dc:title=foo");

        // param for the automation bulk action -> page provider with quick filter to only hit File document
        Map<String, Serializable> actionParams = new HashMap<>();
        actionParams.put(AutomationBulkAction.OPERATION_ID, "Document.Update");
        actionParams.put(AutomationBulkAction.OPERATION_PARAMETERS, (Serializable) automationParams);

        Object r = async.newRequest(BulkRunAction.ID)
                        .set("action", AutomationBulkAction.ACTION_NAME)
                        .set("providerName", "PageProvider")
                        .set("quickFilters", "FileOnly")
                        .set("bucketSize", "10")
                        .set("batchSize", "5")
                        .set("parameters", (new ObjectMapper()).writeValueAsString(actionParams))
                        .execute();
        assertNotNull(r);

        txFeature.nextTransaction();

        // expect only File doc to be modified
        folder = (Document) session.newRequest(FetchDocument.ID).set("value", "/test").execute();
        file = (Document) session.newRequest(FetchDocument.ID).set("value", "/test/file").execute();

        assertEquals("test", folder.getTitle());
        assertEquals("foo", file.getTitle());
    }
}
