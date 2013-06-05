/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.IdRef;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.server.jaxrs.io.ObjectCodecService;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core",
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.notification.core:OSGI-INF/NotificationService.xml" })
@LocalDeploy({ "org.nuxeo.ecm.automation.server:test-bindings.xml",
        "org.nuxeo.ecm.automation.server:test-mvalues.xml",
        "org.nuxeo.ecm.automation.server:core-types-contrib.xml" })
@Features(EmbeddedAutomationServerFeature.class)
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class EmbeddedAutomationClientTest extends AbstractAutomationClientTest {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    UserManager userManager;

    @BeforeClass
    public static void setupCodecs() throws OperationException {
        Framework.getLocalService(ObjectCodecService.class).addCodec(
                new MyObjectCodec());
        Framework.getLocalService(AutomationService.class).putOperation(
                MyObjectOperation.class);
    }

    @Before
    public void setupComplexDocuments() throws Exception {
        Document root = (Document) super.session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        createDocumentWithComplexProperties(root);
    }

    /**
     * Create document with complex properties (from json file)
     */
    public void createDocumentWithComplexProperties(Document root)
            throws Exception {
        // Fill the document properties
        Map<String, Object> creationProps = new HashMap<String, Object>();
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
    }

    @BeforeClass
    public static void addDataCapsuleOperation() throws OperationException {
        Framework.getLocalService(AutomationService.class).putOperation(
                TestDataCapsule.class);
    }

    @Test
    public void testBadAccess() throws Exception {
        try {
            session.newRequest(FetchDocument.ID).set("value", "/foo").execute();
            fail("no exception caught");
        } catch (RemoteException e) {
            // expected
        }
    }

    @Test
    public void testBlobSummaries() throws Exception {
        Blob blob = (Blob) session.newRequest(TestDataCapsule.ID).execute();
        assertEquals("TestDataCapsule - application/json - 25 B",
                blob.toString());
    }

    @Test
    public void testCodecs() throws Exception {
        JsonMarshalling.addMarshaller(new MyObjectMarshaller());
        MyObject msg = (MyObject) session.newRequest(MyObjectOperation.ID).execute();
        assertEquals("hello world", msg.getMessage());
    }

    @Test
    public void testMultiValued() throws Exception {
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();

        Document note = (Document) session.newRequest(CreateDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(root).set("type",
                "MV").set("name", "pfff").set("properties",
                "mv:sl=s1,s2\nmv:ss=s1,s2\nmv:bl=true,false\nmv:b=true\n").execute();
        checkHasCorrectMultiValues(note);

        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                DocumentPageProviderOperation.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("query",
                "SELECT * from MV").set("pageSize", 2).execute();

        assertThat(docs, notNullValue());
        assertThat(docs.size(), is(1));
        checkHasCorrectMultiValues(docs.get(0));
    }

    private void checkHasCorrectMultiValues(Document note) {
        assertThat(note, notNullValue());
        PropertyMap properties = note.getProperties();
        assertThat(properties, notNullValue());

        PropertyList sl = properties.getList("mv:sl");
        assertThat(sl, notNullValue());
        List<Object> slValues = sl.list();
        assertThat(slValues, hasItem((Object) "s1"));
        assertThat(slValues, hasItem((Object) "s2"));

        PropertyList ss = properties.getList("mv:ss");
        assertThat(ss, notNullValue());
        List<Object> ssValues = ss.list();
        assertThat(ssValues, hasItem((Object) "s1"));
        assertThat(ssValues, hasItem((Object) "s2"));

        Boolean b = properties.getBoolean("mv:b");
        assertThat(b, is(true));

        PropertyList bl = properties.getList("mv:bl");
        assertThat(bl, notNullValue());
        List<Object> blValues = bl.list();
        assertThat(blValues, hasItem((Object) "true"));
        assertThat(blValues, hasItem((Object) "false"));
        assertThat(bl.getBoolean(0), is(Boolean.TRUE));
        assertThat(bl.getBoolean(1), is(Boolean.FALSE));
    }

    /**
     * test a chain invocation
     */
    @Test
    public void testChain() throws Exception {
        OperationDocumentation opd = session.getOperation("testchain");
        assertNotNull(opd);

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID).setInput(
                root).set("type", "Folder").set("name", "chainTest").execute();

        Document doc = (Document) session.newRequest("testchain").setInput(
                folder).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());

        // fetch again the note
        doc = (Document) session.newRequest(FetchDocument.ID).set("value", doc).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());
    }

    /**
     * test security on a chain - only disable flag is tested - TODO more tests
     * to test each security filter
     */
    @Test
    public void testChainSecurity() throws Exception {
        OperationDocumentation opd = session.getOperation("principals");
        assertNotNull(opd);
        try {
            session.newRequest("principals").setInput(DocRef.newRef("/")).execute();
            fail("chains invocation is supposed to fail since it is disabled - should return 404");
        } catch (RemoteException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test(expected = RemoteException.class)
    public void testTxTimeout() throws Exception {
        session.newRequest(WaitForTxTimeoutOperation.ID).setHeader(
                ServletHelper.TX_TIMEOUT_HEADER_KEY, "1").execute();
    }

    /**
     * test a chain rollback
     */
    @Test
    public void testChainRollback() throws Exception {

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        // 1. create a note and exit gracefully
        Document doc = (Document) session.newRequest("exitNoRollback").setInput(
                root).execute();
        assertEquals("/test-exit1", doc.getPath());
        Document note = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/test-exit1").execute();
        assertEquals(doc.getPath(), note.getPath());

        // 2. create a note and exit with rollback
        doc = (Document) session.newRequest("exitRollback").setInput(root).execute();
        assertEquals("/test-exit2", doc.getPath());
        try {
            note = (Document) session.newRequest(FetchDocument.ID).set("value",
                    "/test-exit2").execute();
            fail("document should not exist");
        } catch (RemoteException e) {
            // do nothing
        }

        // 3. create a note and exit with error (+rollback)
        try {
            doc = (Document) session.newRequest("exitError").setInput(root).execute();
            fail("expected error");
        } catch (RemoteException t) {
            assertTrue(t.getRemoteStackTrace().contains("termination error"));
        }
        // test the note was not created
        try {
            note = (Document) session.newRequest(FetchDocument.ID).set("value",
                    "/test-exit3").execute();
            fail("document should not exist");
        } catch (RemoteException e) {
            // do nothing
        }

    }

    @Test
    public void testSendMail() throws Exception {

        // Set bad SMTP configuration
        File file = new File(Environment.getDefault().getConfig(),
                "mail.properties");
        file.getParentFile().mkdirs();
        List<String> mailProperties = new ArrayList<String>();
        mailProperties.add(String.format("mail.smtp.host = %s", "badHostName"));
        mailProperties.add(String.format("mail.smtp.port = %s", "2525"));
        FileUtils.writeLines(file, mailProperties);

        Document rootDoc = (Document) session.newRequest(FetchDocument.ID).set(
                "value", "/").execute();
        assertNotNull(rootDoc);

        OperationRequest operationRequest = session.newRequest(SendMail.ID).setInput(
                rootDoc).set("from", "sender@nuxeo.com").set("to",
                "recipient@nuxeo.com").set("subject", "My test mail").set(
                "message", "The message content.");

        // Call SendMail with rollbackOnError = true (default value)
        // => should throw a RemoteException
        try {
            operationRequest.execute();
            fail("Call to SendMail operation should have thrown a RemoteException since the SMTP server is not reachable");
        } catch (RemoteException re) {
            assertEquals("Failed to invoke operation Notification.SendMail",
                    re.getCause().getMessage());
        }

        // Call SendMail with rollbackOnError = false
        // => should only log a WARNING
        Object result = operationRequest.set("rollbackOnError", "false").execute();
        assertNotNull(result);
    }

    @Test
    public void testAuthenticationAndAuthorizationErrors() throws Exception {
        String testUserName = "automation-test-user";
        NuxeoPrincipal principal = userManager.getPrincipal(testUserName);
        if (principal != null) {
            userManager.deleteUser(testUserName);
        }
        try {
            DocumentModel user = userManager.getBareUserModel();
            user.setPropertyValue("user:username", testUserName);
            user.setPropertyValue("user:password", "secret");
            userManager.createUser(user);

            // check invalid credentials
            try {
                client.getSession(testUserName, "badpassword");
                fail("session should not have be created with bad password");
            } catch (RemoteException e) {
                // Bad credentials should be mapped to HTTP 401
                assertEquals(e.getStatus(), 401);
            }

            // test user does not have the permission to access the root, with
            // no property setting the permission exception HTTP status code
            Session userSession = client.getSession(testUserName, "secret");
            try {
                userSession.newRequest(FetchDocument.ID).set("value", "/").execute();
                fail("test user should not have read access to the root document");
            } catch (RemoteException e) {
                // Missing permissions should be mapped to HTTP 401, which is
                // the default behavior
                assertEquals(e.getStatus(), 401);
            }

            // test user does not have the permission to access the root, with
            // the org.nuxeo.ecm.automation.server.permission.httpcode property
            // setting the permission exception HTTP status code to 403
            Framework.getProperties().put(
                    "org.nuxeo.ecm.automation.server.permission.httpcode",
                    "403");
            try {
                userSession.newRequest(FetchDocument.ID).set("value", "/").execute();
                fail("test user should not have read access to the root document");
            } catch (RemoteException e) {
                // Missing permissions should be mapped to HTTP 403
                assertEquals(e.getStatus(), 403);
            }
            Framework.getProperties().remove(
                    "org.nuxeo.ecm.automation.server.permission.httpcode");
        } finally {
            userManager.deleteUser(testUserName);
        }
    }

    @Test
    public void sampleAutomationRemoteAccessWithComplexDocuments()
            throws Exception {

        // the repository init handler sould have created a sample doc in the
        // repo
        // Check that we see it
        Document testDoc = (Document) session.newRequest(
                DocumentService.GetDocumentChild).setInput(new PathRef("/")).set(
                "name", "testDoc").execute();

        assertNotNull(testDoc);

        // try to see what's in it

        // check dublincore.title
        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("testDoc", testDoc.getProperties().get("dc:title"));

        // schema only a subset of the properties are serialized with default
        // configuration (common and dublincore only)
        // see @Constants.HEADER_NX_SCHEMAS

        assertNull(testDoc.getProperties().get("ds:tableName"));
        assertNull(testDoc.getProperties().get("ds:fields"));

        // refetch the doc, but with the correct header
        testDoc = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").set("value", "/testDoc").execute();

        assertNotNull(testDoc);

        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("MyTable", testDoc.getProperties().get("ds:tableName"));
        assertNotNull(testDoc.getProperties().get("ds:fields"));

        PropertyList dbFields = testDoc.getProperties().getList("ds:fields");
        assertEquals(5, dbFields.size());

        PropertyMap dbField0 = dbFields.getMap(0);
        assertNotNull(dbField0);
        assertEquals("field0", dbField0.getString("name"));

        assertEquals("Decision", dbField0.getList("roles").getString(0));
        assertEquals("Score", dbField0.getList("roles").getString(1));

        // now update the doc
        Map<String, Object> updateProps = new HashMap<String, Object>();

        updateProps.put("ds:tableName", "newTableName");
        updateProps.put("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        updateProps.put("ds:fields", fieldsDataAsJSon);

        testDoc = (Document) session.newRequest(UpdateDocument.ID).setHeader(
                Constants.HEADER_NX_SCHEMAS, "*").setInput(
                new IdRef(testDoc.getId())).set("properties",
                new PropertyMap(updateProps).toString()).execute();

        // check the returned doc
        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("newTableName",
                testDoc.getProperties().get("ds:tableName"));

        PropertyList atts = testDoc.getProperties().getList("ds:attachments");
        assertNotNull(atts);
        assertEquals(4, atts.size());
        assertEquals("new1", atts.getString(0));
        assertEquals("new4", atts.getString(3));

        dbFields = testDoc.getProperties().getList("ds:fields");
        assertEquals(2, dbFields.size());

        PropertyMap dbFieldA = dbFields.getMap(0);
        assertNotNull(dbFieldA);
        assertEquals("fieldA", dbFieldA.getString("name"));
    }
}
