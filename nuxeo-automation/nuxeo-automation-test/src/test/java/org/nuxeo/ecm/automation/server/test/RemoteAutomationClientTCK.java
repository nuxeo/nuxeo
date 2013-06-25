package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.BusinessService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.server.test.business.client.BusinessBean;
import org.nuxeo.ecm.automation.test.RemoteAutomationServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.7.2 Automation TCK Tests Suite
 */
@RunWith(FeaturesRunner.class)
@Features(RemoteAutomationServerFeature.class)
public class RemoteAutomationClientTCK {

    @Inject
    Session session;

    @Inject
    HttpAutomationClient client;

    /**
     * Create Read Update Delete
     */
    @Test
    public void testSuite() throws Exception {
        testCreateDocument();
        testFetchDocument();
        testQuery();
        testUpdateDocument();
        testDeleteDocument();
        testAttachBlob();
        testGetBlob();
        testComplexProperties();
        testAutomationBusinessObjects();
    }

    public void testCreateDocument() throws Exception {
        Document folder = (Document) session.newRequest("Document.Create").setInput(
                "/").set("type", "Folder").set("name", "myfolder").set(
                "properties", "dc:title=My Folder").execute();
        // Assertions
        assertNotNull(folder);
        assertEquals("/myfolder", folder.getPath());
        assertEquals("My Folder", folder.getTitle());
    }

    public void testFetchDocument() throws Exception {
        Document folder = (Document) session.newRequest("Document.Fetch").set(
                "value", "/myfolder").execute();
        // Assertions
        assertNotNull(folder);
        assertEquals("/myfolder", folder.getPath());
        assertEquals("My Folder", folder.getTitle());
    }

    public void testQuery() throws Exception {
        Documents docs = (Documents) session.newRequest("Document.Query").set(
                "query", "SELECT * FROM Document").execute();
        // Assertions
        assertFalse(docs.isEmpty());
    }

    public void testUpdateDocument() throws Exception {
        Document folder = (Document) session.newRequest("Document.Update").setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(getFolder()).set(
                "properties", "dc:title=My Folder2\ndc:description=test").execute();
        // Assertions
        assertNotNull(folder);
        assertEquals("/myfolder", folder.getPath());
        assertEquals("My Folder2", folder.getTitle());
        assertEquals("test", folder.getProperties().getString("dc:description"));
    }

    public void testDeleteDocument() throws Exception {
        Document folder = (Document) session.newRequest(DeleteDocument.ID).setInput(
                getFolder()).execute();
        // Assertions
        assertNull(folder);
    }

    public Document getFolder() throws Exception {
        Document folder = (Document) session.newRequest("Document.Fetch").set(
                "value", "/myfolder").execute();
        return folder;
    }

    /**
     * Managing Blobs
     */

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

    public void testComplexProperties() throws Exception {

    }

    /**
     * Ignore until we got a complex type into nuxeo test distribution or by
     * default in the platform
     */
    public void testComplexPropertiesWithJSON() throws Exception {
        // Fetch the document
        Document document = (Document) session.newRequest("Document.GetChild").setInput(
                new PathRef("/")).set("name", "testDoc").execute();

        // Send the fields representation as json

        // Read the json file
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creation.json");
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);

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
                "Note Content", "Note");
        BusinessService businessService = session.getAdapter(BusinessService.class);
        assertNotNull(businessService);

        // adding BusinessBean marshaller
        JsonMarshalling.addMarshaller(PojoMarshaller.forClass(note.getClass()));

        // This request can be done directly with
        // Operation.BusinessCreateOperation ->
        // session.newRequest("Operation.BusinessCreateOperation").setInput(o).set("name",
        // name).set("type", type).set("parentPath",parentPath).execute();
        note = (BusinessBean) businessService.create(note, note.getTitle(), "/");
        assertNotNull(note);
        // Test for pojo <-> adapter automation update
        note.setTitle("Update");
        note = (BusinessBean) businessService.update(note, note.getId());
        assertEquals("Update", note.getTitle());
    }
}
