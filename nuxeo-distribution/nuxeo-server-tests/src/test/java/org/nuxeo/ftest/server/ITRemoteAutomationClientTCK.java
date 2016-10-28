/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.server.test.business.client.BusinessBean;
import org.nuxeo.ecm.automation.test.RemoteAutomationServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.2 Automation TCK Tests Suite
 */
@RunWith(FeaturesRunner.class)
@Features(RemoteAutomationServerFeature.class)
public class ITRemoteAutomationClientTCK {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    Session session;

    @Inject
    HttpAutomationClient client;

    private Document folder1;

    private Document folder2;

    private Document folder3;

    private Document file;

    @Test
    public void testSuite() throws Exception {
        testCRUDSuite();
        testBlobSuite();
        testPaginationSuite();
        // TODO: NXP-17000 (reactivate when NXBT-902 is resolved)
        // testComplexPropertiesWithJSON();
        // testAutomationBusinessObjects();
    }

    public void testCRUDSuite() throws Exception {
        testCreateRoot();
        testCreateChild1();
        testCreateChild2();
        testUpdateChild2();
        testGetChildren();
        testGetChildrenWithWebAdapter();
    }

    public void testBlobSuite() throws Exception {
        testCreateBlobText();
        testAttachBlob();
        testGetBlob();
    }

    public void testPaginationSuite() throws Exception {
        testCreateAnotherRoot();
        testCreateChild("1");
        testCreateChild("2");
        testCreateChild("3");
        testQueryPage1();
        testQueryPage2();
    }

    /**
     * Create Read Update Delete
     */
    public void testCreateRoot() throws Exception {
        folder1 = (Document) session.newRequest("Document.Create")
                                    .setInput("/")
                                    .set("type", "Folder")
                                    .set("name", "TestFolder1")
                                    .set("properties", "dc:title=Test Folder2\ndc:description=Simple container")
                                    .execute();
        assertNotNull(folder1);
        assertEquals("/TestFolder1", folder1.getPath());
        assertEquals("Test Folder2", folder1.getTitle());
    }

    public void testCreateChild1() throws Exception {
        Document aFile = (Document) session.newRequest("Document.Create")
                                           .setInput("/TestFolder1")
                                           .set("type", "File")
                                           .set("name", "TestFile1")
                                           .execute();
        assertNotNull(aFile);
        assertEquals("/TestFolder1/TestFile1", aFile.getPath());
    }

    public void testCreateChild2() throws Exception {
        Document aFile = (Document) session.newRequest("Document.Create")
                                           .setInput("/TestFolder1")
                                           .set("type", "File")
                                           .set("name", "TestFile2")
                                           .execute();
        assertNotNull(aFile);
        assertEquals("/TestFolder1/TestFile2", aFile.getPath());
    }

    public void testUpdateChild2() throws Exception {
        Document aFile = (Document) session.newRequest("Document.Update")
                                           .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                           .setInput("/TestFolder1/TestFile2")
                                           .set("save", "true")
                                           .set("properties", "dc:description=Simple File\ndc:subjects=art,sciences")
                                           .execute();
        assertNotNull(aFile);
        assertEquals("Simple File", aFile.getProperties().get("dc:description"));
        assertEquals(2, ((PropertyList) aFile.getProperties().get("dc:subjects")).size());
    }

    public void testGetChildren() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set("value", "/TestFolder1").execute();
        assertNotNull(root);
        Documents children = (Documents) session.newRequest("Document.GetChildren").setInput(root.getPath()).execute();
        assertEquals(2, children.size());

    }

    public void testGetChildrenWithWebAdapter() throws Exception {
        JsonNode node = client.getRestClient().newRequest("/path/TestFolder1/@children").execute().asJson();

        assertEquals(2, node.get("resultsCount").getIntValue());

    }

    /**
     * Managing Pagination
     */

    public void testCreateAnotherRoot() throws Exception {
        folder2 = (Document) session.newRequest("Document.Create")
                                    .setInput("/")
                                    .set("type", "Folder")
                                    .set("name", "TestFolder2")
                                    .set("properties", "dc:title=Test Folder3\ndc:description=Simple container")
                                    .execute();
        assertNotNull(folder2);
    }

    public void testCreateChild(String id) throws Exception {
        String name = "TestFile" + id;
        Document aFile = (Document) session.newRequest("Document.Create")
                                           .setInput("/TestFolder2")
                                           .set("type", "File")
                                           .set("name", name)
                                           .execute();
        assertNotNull(aFile);
    }

    public void testQueryPage1() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set("value", "/TestFolder2").execute();
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                              .set("query",
                                                                      "select * from Document where ecm:parentId = ?")
                                                              .set("queryParams", root.getId())
                                                              .set("pageSize", "2")
                                                              .set("page", "0")
                                                              .execute();
        assertEquals(2, docs.size());
        assertEquals(2, docs.getPageSize());
        assertEquals(2, docs.getNumberOfPages());
        assertEquals(3, docs.getResultsCount());
    }

    public void testQueryPage2() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set("value", "/TestFolder2").execute();
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                              .set("query",
                                                                      "select * from Document where ecm:parentId = ?")
                                                              .set("queryParams", root.getId())
                                                              .set("pageSize", "2")
                                                              .set("page", "1")
                                                              .execute();
        assertEquals(1, docs.size());
        assertEquals(2, docs.getPageSize());
        assertEquals(2, docs.getNumberOfPages());
        assertEquals(3, docs.getResultsCount());
    }

    /**
     * Managing Blobs
     */
    protected File newFile(String content) throws IOException {
        File aFile = Framework.createTempFile("automation-test-", ".xml");
        FileUtils.writeFile(aFile, content);
        return aFile;
    }

    // Create documents from Blob using muti-part encoding
    public void testCreateBlobText() throws Exception {
        folder3 = (Document) session.newRequest("Document.Create")
                                    .setInput("/")
                                    .set("type", "Folder")
                                    .set("name", "FolderBlob")
                                    .execute();
        assertNotNull(folder3);
        assertEquals("/FolderBlob", folder3.getPath());
        File aFile = newFile("<doc>mydoc</doc>");
        FileBlob blob = new FileBlob(aFile);
        blob.setMimeType("text/xml");
        session.newRequest("FileManager.Import")
               .setInput(blob)
               .setContextProperty("currentDocument", folder3.getPath())
               .execute();
        Documents docs = (Documents) session.newRequest(DocumentPaginatedQuery.ID)
                                            .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                            .set("query",
                                                    "SELECT * from Document WHERE ecm:path STARTSWITH '/FolderBlob/'")
                                            .execute();
        assertEquals(1, docs.size());
        Document document = docs.get(0);
        // get the file content property
        PropertyMap map = document.getProperties().getMap("file:content");
        // get the data URL
        String path = map.getString("data");
        // download the file from its remote location
        blob = (FileBlob) session.getFile(path);
        assertNotNull(blob);
        assertEquals("text/xml", blob.getMimeType());
        assertEquals("<doc>mydoc</doc>", IOUtils.toString(blob.getStream(), "utf-8"));
    }

    // Test attaching blob
    public void testAttachBlob() throws Exception {
        // get the root
        Document root = (Document) session.newRequest("Document.Fetch").set("value", "/").execute();
        // create a file document
        file = (Document) session.newRequest("Document.Create")
                                 .setInput(root)
                                 .set("type", "File")
                                 .set("name", "myfile")
                                 .set("properties", "dc:title=My File")
                                 .execute();
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        FileBlob fb = new FileBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");
        session.newRequest("Blob.Attach")
               .setHeader("X-NXVoidOperation", "true")
               .setInput(fb)
               .set("document", "/myfile")
               .execute();
    }

    // Test attaching blob
    public void testGetBlob() throws Exception {
        // get the file document where blob was attached
        Document doc = (Document) session.newRequest("Document.Fetch")
                                         .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                         .set("value", "/myfile")
                                         .execute();
        // get the file content property
        PropertyMap map = doc.getProperties().getMap("file:content");
        // get the data URL
        String path = map.getString("data");

        // download the file from its remote location
        FileBlob blob = (FileBlob) session.getFile(path);
        // ... do something with the file
        // at the end delete the temporary file
        blob.getFile().delete();
    }

    /**
     * Managing Complex Properties
     */
    public void testComplexPropertiesWithJSON() throws Exception {
        // get the root
        Document root = (Document) session.newRequest("Document.Fetch").set("value", "/").execute();
        // Fill the document properties
        Map<String, Object> creationProps = new HashMap<>();
        creationProps.put("ds:tableName", "MyTable");
        creationProps.put("ds:attachments", attachments);

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        creationProps.put("ds:fields", fieldsDataAsJSon);
        creationProps.put("dc:title", "testDoc");

        // Document creation
        session.newRequest(CreateDocument.ID)
               .setInput(root)
               .set("type", "DataSet")
               .set("name", "testDoc")
               .set("properties", new PropertyMap(creationProps).toString())
               .execute();
        // Fetch the document
        Document document = (Document) session.newRequest("Document.GetChild")
                                              .setInput(new PathRef("/"))
                                              .set("name", "testDoc")
                                              .execute();

        // Send the fields representation as json

        // Read the json file
        fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);

        // Don't forget to replace CRLF or LF
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");

        // Set the json values to the related metadata
        document.set("ds:fields", fieldsDataAsJSon);

        // Document Update
        session.newRequest("Document.Update").setInput(document).set("properties", document).execute();
    }

    /**
     * Managing Business Objects
     */
    public void testAutomationBusinessObjects() throws Exception {
        // Test for pojo <-> adapter automation creation
        BusinessBean note = new BusinessBean("Note", "File description", "Note Content", "Note", new String("object"));

        // Marshaller for bean 'note' registration
        client.registerPojoMarshaller(note.getClass());

        note = (BusinessBean) session.newRequest("Business.BusinessCreateOperation")
                                     .setInput(note)
                                     .set("name", note.getTitle())
                                     .set("parentPath", "/")
                                     .execute();
        assertNotNull(note);
        // Test for pojo <-> adapter automation update
        note.setTitle("Update");
        note = (BusinessBean) session.newRequest("Business.BusinessUpdateOperation").setInput(note).execute();
        assertEquals("Update", note.getTitle());
    }

    @After
    public void teardown() throws IOException {
        Documents list = new Documents();
        list.add(folder1);
        list.add(folder2);
        list.add(folder3);
        list.add(file);
        session.newRequest("Document.Delete").setInput(list).execute();
    }
}
