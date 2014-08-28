/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier
 *
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import org.nuxeo.ecm.automation.core.operations.document.DocumentQuery;
import org.nuxeo.ecm.automation.server.test.business.client.BusinessBean;
import org.nuxeo.ecm.automation.test.RemoteAutomationServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.7.2 Automation TCK Tests Suite
 */
@RunWith(FeaturesRunner.class)
@Features(RemoteAutomationServerFeature.class)
public class RemoteAutomationClientTCK {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    Session session;

    @Inject
    HttpAutomationClient client;

    @Test
    public void testSuite() throws Exception {
        testCRUDSuite();
        testBlobSuite();
        testPaginationSuite();
        testComplexPropertiesWithJSON();
        testAutomationBusinessObjects();
    }

    public void testCRUDSuite() throws Exception {
        testCreateRoot();
        testCreateChild1();
        testCreateChild2();
        testUpdateChild2();
        testGetChildren();
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
        Document folder = (Document) session.newRequest("Document.Create").setInput(
                "/").set("type", "Folder").set("name", "TestFolder1").set(
                "properties",
                "dc:title=Test Folder2\ndc:description=Simple container").execute();
        assertNotNull(folder);
        assertEquals("/TestFolder1", folder.getPath());
        assertEquals("Test Folder2", folder.getTitle());
    }

    public void testCreateChild1() throws Exception {
        Document file = (Document) session.newRequest("Document.Create").setInput(
                "/TestFolder1").set("type", "File").set("name", "TestFile1").execute();
        assertNotNull(file);
        assertEquals("/TestFolder1/TestFile1", file.getPath());
    }

    public void testCreateChild2() throws Exception {
        Document file = (Document) session.newRequest("Document.Create").setInput(
                "/TestFolder1").set("type", "File").set("name", "TestFile2").execute();
        assertNotNull(file);
        assertEquals("/TestFolder1/TestFile2", file.getPath());
    }

    public void testUpdateChild2() throws Exception {
        Document file = (Document) session.newRequest("Document.Update").setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(
                "/TestFolder1/TestFile2").set("save", "true").set("properties",
                "dc:description=Simple File\ndc:subjects=subject1,subject2").execute();
        assertNotNull(file);
        assertEquals("Simple File", file.getProperties().get("dc:description"));
        assertEquals(2,
                ((PropertyList) file.getProperties().get("dc:subjects")).size());
    }

    public void testGetChildren() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/TestFolder1").execute();
        assertNotNull(root);
        Documents children = (Documents) session.newRequest(
                "Document.GetChildren").setInput(root.getPath()).execute();
        assertEquals(2, children.size());

    }

    /**
     * Managing Pagination
     */

    public void testCreateAnotherRoot() throws Exception {
        Document folder = (Document) session.newRequest("Document.Create").setInput(
                "/").set("type", "Folder").set("name", "TestFolder2").set(
                "properties",
                "dc:title=Test Folder3\ndc:description=Simple container").execute();
        assertNotNull(folder);
    }

    public void testCreateChild(String id) throws Exception {
        String name = "TestFile" + id;
        Document file = (Document) session.newRequest("Document.Create").setInput(
                "/TestFolder2").set("type", "File").set("name", name).execute();
        assertNotNull(file);
    }

    public void testQueryPage1() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/TestFolder2").execute();
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                "Document.PageProvider").set("query",
                "select * from Document where ecm:parentId = ?").set(
                "queryParams", root.getId()).set("pageSize", "2").set("page",
                "0").execute();
        assertEquals(2, docs.size());
        assertEquals(2, docs.getPageSize());
        assertEquals(2, docs.getNumberOfPages());
        assertEquals(3, docs.getResultsCount());
    }

    public void testQueryPage2() throws Exception {
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/TestFolder2").execute();
        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                "Document.PageProvider").set("query",
                "select * from Document where ecm:parentId = ?").set(
                "queryParams", root.getId()).set("pageSize", "2").set("page",
                "1").execute();
        assertEquals(1, docs.size());
        assertEquals(2, docs.getPageSize());
        assertEquals(2, docs.getNumberOfPages());
        assertEquals(3, docs.getResultsCount());
    }

    /**
     * Managing Blobs
     */

    protected File newFile(String content) throws IOException {
        File file = File.createTempFile("automation-test-", ".xml");
        Framework.trackFile(file, this);
        FileUtils.writeFile(file, content);
        return file;
    }

    // Create documents from Blob using muti-part encoding
    public void testCreateBlobText() throws Exception {
        Document folder = (Document) session.newRequest("Document.Create").setInput(
                "/").set("type", "Folder").set("name", "FolderBlob").execute();
        assertNotNull(folder);
        assertEquals("/FolderBlob", folder.getPath());
        File file = newFile("<doc>mydoc</doc>");
        FileBlob blob = new FileBlob(file);
        blob.setMimeType("text/xml");
        session.newRequest("FileManager.Import").setInput(blob).setContextProperty(
                "currentDocument", folder.getPath()).execute();
        Documents docs = (Documents) session.newRequest(DocumentQuery.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("query",
                "SELECT * from Document WHERE ecm:path STARTSWITH '/FolderBlob/'").execute();
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
        assertEquals("<doc>mydoc</doc>",
                IOUtils.toString(blob.getStream(), "utf-8"));
    }

    // Test attaching blob
    public void testAttachBlob() throws Exception {
        // get the root
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/").execute();
        // create a file document
        session.newRequest("Document.Create").setInput(root).set("type", "File").set(
                "name", "myfile").set("properties", "dc:title=My File").execute();
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        FileBlob fb = new FileBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");
        session.newRequest("Blob.Attach").setHeader("X-NXVoidOperation", "true").setInput(
                fb).set("document", "/myfile").execute();
    }

    // Test attaching blob
    public void testGetBlob() throws Exception {
        // get the file document where blob was attached
        Document doc = (Document) session.newRequest("Document.Fetch").setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/myfile").execute();
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
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/").execute();
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
        session.newRequest(CreateDocument.ID).setInput(root).set("type",
                "DataSet").set("name", "testDoc").set("properties",
                new PropertyMap(creationProps).toString()).execute();
        // Fetch the document
        Document document = (Document) session.newRequest("Document.GetChild").setInput(
                new PathRef("/")).set("name", "testDoc").execute();

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
        session.newRequest("Document.Update").setInput(document).set(
                "properties", document).execute();
    }

    /**
     * Managing Business Objects
     */

    public void testAutomationBusinessObjects() throws Exception {
        // Test for pojo <-> adapter automation creation
        BusinessBean note = new BusinessBean("Note", "File description",
                "Note Content", "Note", new String("object"));

        // Marshaller for bean 'note' registration
        client.registerPojoMarshaller(note.getClass());

        note = (BusinessBean) session.newRequest(
                "Business.BusinessCreateOperation").setInput(note).set("name",
                note.getTitle()).set("parentPath", "/").execute();
        assertNotNull(note);
        // Test for pojo <-> adapter automation update
        note.setTitle("Update");
        note = (BusinessBean) session.newRequest(
                "Business.BusinessUpdateOperation").setInput(note).execute();
        assertEquals("Update", note.getTitle());
    }
}
