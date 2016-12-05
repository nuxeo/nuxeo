/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.number.IsCloseTo;
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
import org.nuxeo.ecm.automation.client.RemoteThrowable;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.BusinessService;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.DateUtils;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.IdRef;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.business.BusinessCreateOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessFetchOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessUpdateOperation;
import org.nuxeo.ecm.automation.core.operations.document.AddPermission;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.operations.services.AuditLog;
import org.nuxeo.ecm.automation.core.operations.services.AuditPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.traces.JsonStackToggleDisplayOperation;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.automation.server.test.business.client.BusinessBean;
import org.nuxeo.ecm.automation.server.test.business.client.TestBusinessArray;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;
import org.nuxeo.ecm.automation.server.test.json.NestedJSONOperation;
import org.nuxeo.ecm.automation.server.test.json.POJOObject;
import org.nuxeo.ecm.automation.server.test.json.SimplePojoObjectMarshaller;
import org.nuxeo.ecm.automation.server.test.operations.ContextInjectionOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.helpers.ExceptionTest;
import org.nuxeo.ecm.automation.test.helpers.HttpStatusOperationTest;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.notification.core:OSGI-INF/NotificationService.xml", "org.nuxeo.ecm.automation.test" })
@LocalDeploy({ "org.nuxeo.ecm.automation.test:test-bindings.xml", "org.nuxeo.ecm.automation.test:test-mvalues.xml",
        "org.nuxeo.ecm.automation.test:operation-contrib.xml" })
@Features({ EmbeddedAutomationServerFeature.class, AuditFeature.class })
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class EmbeddedAutomationClientTest extends AbstractAutomationClientTest {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    UserManager userManager;

    @BeforeClass
    public static void setupCodecs() throws Exception {
        Framework.getLocalService(ObjectCodecService.class).addCodec(new MyObjectCodec());
        Framework.getLocalService(AutomationService.class).putOperation(MyObjectOperation.class);
        // Fire application start on AutomationServer component forcing to load
        // correctly Document Adapter Codec in Test scope (to take into account
        // of document adapters contributed into test) -> see execution order
        // here: org.nuxeo.runtime.test.runner.RuntimeFeature.start()
        ComponentInstance componentInstance = Framework.getRuntime().getComponentInstance(
                "org.nuxeo.ecm.automation.server.AutomationServer");
        AutomationServerComponent automationServerComponent = (AutomationServerComponent) componentInstance.getInstance();
        automationServerComponent.applicationStarted(componentInstance);
    }

    /**
     * Use to setup complex documents for related tests
     */
    public void setupComplexDocuments() throws Exception {
        Document root = (Document) super.session.newRequest(FetchDocument.ID).set("value", "/").execute();
        createDocumentWithComplexProperties(root);
    }

    /**
     * Create document with complex properties (from json file)
     */
    public void createDocumentWithComplexProperties(Document root) throws Exception {
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
    }

    @BeforeClass
    public static void addDataCapsuleOperation() throws OperationException {
        Framework.getLocalService(AutomationService.class).putOperation(TestDataCapsule.class);
    }

    @Test
    public void testBlobSummaries() throws Exception {
        Blob blob = (Blob) session.newRequest(TestDataCapsule.ID).execute();
        assertEquals("TestDataCapsule - application/json - 25 B", blob.toString());
    }

    @Test
    public void testCodecs() throws Exception {
        JsonMarshalling.addMarshaller(new MyObjectMarshaller());
        MyObject msg = (MyObject) session.newRequest(MyObjectOperation.ID).execute();
        assertEquals("hello world", msg.getMessage());
    }

    @Test
    public void testMultiValued() throws Exception {
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();

        Document note = (Document) session.newRequest(CreateDocument.ID)
                                          .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                          .setInput(root)
                                          .set("type", "MV")
                                          .set("name", "pfff")
                                          .set("properties", "mv:sl=s1,s2\nmv:ss=s1,s2\nmv:bl=true,false\nmv:b=true\n")
                                          .execute();
        checkHasCorrectMultiValues(note);

        PaginableDocuments docs = (PaginableDocuments) session.newRequest(DocumentPageProviderOperation.ID)
                                                              .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                                              .set("query", "SELECT * from MV")
                                                              .set("queryParams", new String[] {})
                                                              .set("pageSize", 2)
                                                              .execute();

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
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        // create a folder
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setInput(root)
                                            .set("type", "Folder")
                                            .set("name", "chainTest")
                                            .execute();

        Document doc = (Document) session.newRequest("testchain").setInput(folder).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());

        // fetch again the note
        doc = (Document) session.newRequest(FetchDocument.ID).set("value", doc).execute();
        assertEquals("/chainTest/chain.doc", doc.getPath());
        assertEquals("Note", doc.getType());
    }

    /**
     * We allow to call chain operation since 5.7.2. Test on it.
     */
    @Test
    public void testRemoteChain() throws Exception {
        OperationDocumentation opd = session.getOperation("principals");
        assertNotNull(opd);
        Document doc = (Document) session.newRequest("principals").setInput(DocRef.newRef("/")).execute();
        assertNotNull(doc);
    }

    @Test(expected = RemoteException.class)
    public void testTxTimeout() throws Exception {
        session.newRequest(WaitForTxTimeoutOperation.ID).setHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY, "1").execute();
    }

    @Test
    public void testBaseInputAndReturnValues() throws Exception {
        Object r;
        r = session.newRequest(ReturnOperation.ID).setInput(Boolean.TRUE).execute();
        assertThat((Boolean) r, is(Boolean.TRUE));

        r = session.newRequest(ReturnOperation.ID).setInput("hello").execute();
        assertThat((String) r, is("hello"));

        r = session.newRequest(ReturnOperation.ID).setInput(1).execute();
        assertThat(((Number) r).intValue(), is(1));

        r = session.newRequest(ReturnOperation.ID).setInput(1000000000000000000L).execute();
        assertThat(((Number) r).longValue(), is(1000000000000000000L));

        r = session.newRequest(ReturnOperation.ID).setInput(1.1d).execute();
        assertThat(((Number) r).doubleValue(), IsCloseTo.closeTo(1.1d, 0.1));

        Date now = DateUtils.parseDate(DateUtils.formatDate(new Date(0)));
        r = session.newRequest(ReturnOperation.ID).setInput(now).execute();
        assertThat((Date) r, is(now));
    }

    @Test
    public void testNumberParamAdapters() throws Exception {
        Object r;
        // Long parameter
        Long longParam = 500L;
        r = session.newRequest(TestNumberParamAdaptersOperation.ID).set("longParam", longParam).execute();
        assertThat((Integer) r, is(500));
        // Integer parameter
        Integer integerParam = 500;
        r = session.newRequest(TestNumberParamAdaptersOperation.ID).set("longParam", integerParam).execute();
        assertThat((Integer) r, is(500));
    }

    /**
     * test a chain rollback
     */
    @Test
    public void testChainRollback() throws Exception {

        // get the root
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        // 1. create a note and exit gracefully
        Document doc = (Document) session.newRequest("exitNoRollback").setInput(root).execute();
        assertEquals("/test-exit1", doc.getPath());
        Document note = (Document) session.newRequest(FetchDocument.ID).set("value", "/test-exit1").execute();
        assertEquals(doc.getPath(), note.getPath());

        // 2. create a note and exit with rollback
        doc = (Document) session.newRequest("exitRollback").setInput(root).execute();
        assertEquals("/test-exit2", doc.getPath());
        try {
            note = (Document) session.newRequest(FetchDocument.ID).set("value", "/test-exit2").execute();
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
            note = (Document) session.newRequest(FetchDocument.ID).set("value", "/test-exit3").execute();
            fail("document should not exist");
        } catch (RemoteException e) {
            // do nothing
        }

    }

    @Test
    public void testSendMail() throws Exception {

        // Set bad SMTP configuration
        File file = new File(Environment.getDefault().getConfig(), "mail.properties");
        file.getParentFile().mkdirs();
        List<String> mailProperties = new ArrayList<>();
        mailProperties.add(String.format("mail.smtp.host = %s", "badHostName"));
        mailProperties.add(String.format("mail.smtp.port = %s", "2525"));
        mailProperties.add(String.format("mail.smtp.connectiontimeout = %s", "1000"));
        mailProperties.add(String.format("mail.smtp.timeout = %s", "1000"));
        FileUtils.writeLines(file, mailProperties);

        Document rootDoc = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        assertNotNull(rootDoc);

        OperationRequest operationRequest = session.newRequest(SendMail.ID)
                                                   .setInput(rootDoc)
                                                   .set("from", "sender@nuxeo.com")
                                                   .set("to", "recipient@nuxeo.com")
                                                   .set("subject", "My test mail")
                                                   .set("message", "The message content.");

        // Call SendMail with rollbackOnError = true (default value)
        // => should throw a RemoteException
        try {
            operationRequest.execute();
            fail("Call to SendMail operation should have thrown a RemoteException since the SMTP server is not reachable");
        } catch (RemoteException re) {
            assertEquals("Failed to invoke operation: Document.Mail", re.getMessage());
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

            // commit directory changes
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // check invalid credentials
            try {
                client.getSession(testUserName, "badpassword");
                fail("session should not have be created with bad password");
            } catch (RemoteException e) {
                // Bad credentials should be mapped to HTTP 401
                assertEquals(e.getStatus(), 401);
            }

            // test user does not have the permission to access the root
            Session userSession = client.getSession(testUserName, "secret");
            try {
                userSession.newRequest(FetchDocument.ID).set("value", "/").execute();
                fail("test user should not have read access to the root document");
            } catch (RemoteException e) {
                // Missing permissions should be mapped to HTTP 403
                assertEquals(e.getStatus(), 403);
            }
        } finally {
            userManager.deleteUser(testUserName);
        }
    }

    @Test
    public void sampleAutomationRemoteAccessWithComplexDocuments() throws Exception {

        // Initialize repository for this test
        setupComplexDocuments();

        // the repository init handler sould have created a sample doc in the
        // repo
        // Check that we see it
        Document testDoc = (Document) session.newRequest(DocumentService.GetDocumentChild)
                                             .setInput(new PathRef("/"))
                                             .set("name", "testDoc")
                                             .execute();

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
        testDoc = (Document) session.newRequest(DocumentService.FetchDocument)
                                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                    .set("value", "/testDoc")
                                    .execute();

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
        Map<String, Object> updateProps = new HashMap<>();

        updateProps.put("ds:tableName", "newTableName");
        updateProps.put("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        updateProps.put("ds:fields", fieldsDataAsJSon);

        testDoc = (Document) session.newRequest(UpdateDocument.ID)
                                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                    .setInput(new IdRef(testDoc.getId()))
                                    .set("properties", new PropertyMap(updateProps).toString())
                                    .execute();

        // check the returned doc
        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("newTableName", testDoc.getProperties().get("ds:tableName"));

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

    /* The following tests need automation server 5.7 or later */

    @Test
    public void testRawJSONDatastructuresAsParameters() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        POJOObject obj1 = new POJOObject("[obj1 text]", Arrays.asList("1", "2"));
        POJOObject obj2 = new POJOObject("[obj2 text]", Arrays.asList("2", "3"));

        String obj1JSON = mapper.writeValueAsString(obj1);
        String obj2JSON = mapper.writeValueAsString(obj2);

        Map<String, Object> map1 = mapper.readValue(obj1JSON, Map.class);
        Map<String, Object> map2 = mapper.readValue(obj2JSON, Map.class);

        // Expected result when passing obj1 and obj2 as input to the
        POJOObject expectedObj12 = new POJOObject("Merged texts: [obj1 text][obj2 text]", Arrays.asList("1", "2", "2",
                "3"));

        // The pojo and the map parameters can be passed as java objects
        // directly in the client call, the generic Jackson-based parser /
        // serialization will be used
        POJOObject returnedObj12 = (POJOObject) session.newRequest(NestedJSONOperation.ID)
                                                       .set("pojo", obj1)
                                                       .set("map", map2)
                                                       .execute();
        assertEquals(expectedObj12, returnedObj12);

        // It is also possible to pass alternative Java representation of the
        // input parameters as long as they share the same JSON representation
        // for the transport.
        returnedObj12 = (POJOObject) session.newRequest(NestedJSONOperation.ID)
                                            .set("pojo", map1)
                                            .set("map", obj2)
                                            .execute();
        assertEquals(expectedObj12, returnedObj12);

        // Check scalar parameters can be passed as argument
        POJOObject expectedObj1AndDouble = new POJOObject("Merged texts: [obj1 text]", Arrays.asList("1", "2", "3.0"));
        POJOObject returnedObj1AndDouble = (POJOObject) session.newRequest(NestedJSONOperation.ID)
                                                               .set("pojo", map1)
                                                               .set("doubleParam", 3.0)
                                                               .execute();
        assertEquals(expectedObj1AndDouble, returnedObj1AndDouble);
    }

    @Test
    public void testRawJSONDatastructuresAsInput() throws Exception {
        // It is possible to pass arbitrary Java objects as the input as
        // long as the JSON representation is a valid representation for the
        // expected input type of the operation
        POJOObject expectedListObj = new POJOObject("Merged texts: ", Arrays.asList("a", "b", "c"));
        POJOObject returnedListObj = (POJOObject) session.newRequest(NestedJSONOperation.ID)
                                                         .setInput(Arrays.asList("a", "b", "c"))
                                                         .execute();
        assertEquals(expectedListObj, returnedListObj);

        // Try with alternative input type datastructures to check input type
        // negotiation: note, as no special codec has been rejustered for
        // POJOObject, the operation must be able to consume Map instances with
        // the same inner structure as the POJOObject class.
        POJOObject pojoInput = new POJOObject("input pojo", Arrays.asList("a", "b", "c"));
        returnedListObj = (POJOObject) session.newRequest(NestedJSONOperation.ID).setInput(pojoInput).execute();
        assertEquals(expectedListObj, returnedListObj);

        // Pojo can be mapped to java Map datastructure and passed as input to
        // operations
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> mapInput = mapper.convertValue(pojoInput, Map.class);
        returnedListObj = (POJOObject) session.newRequest(NestedJSONOperation.ID).setInput(mapInput).execute();
        assertEquals(expectedListObj, returnedListObj);

        // It is also possible to serialize an explicitly typed represenation of
        // the pojo if both the client and the server are expected to have the
        // same class definition available in their classloading context.
        JsonMarshalling.addMarshaller(PojoMarshaller.forClass(POJOObject.class));
        returnedListObj = (POJOObject) session.newRequest(NestedJSONOperation.ID).setInput(pojoInput).execute();
        assertEquals(expectedListObj, returnedListObj);
    }

    @Test
    public void testArraysAsParametersAndResult() throws Exception {
        JsonMarshalling.addMarshaller(new SimplePojoObjectMarshaller());
        List<SimplePojo> list = new ArrayList<>();
        list.add(new SimplePojo("test1"));
        list.add(new SimplePojo("test2"));
        list.add(new SimplePojo("test3"));
        List<List<SimplePojo>> listList = new ArrayList<>();
        listList.add(list);
        SimplePojo[] simplePojos = list.toArray(new SimplePojo[list.size()]);
        SimplePojo result1 = (SimplePojo) session.newRequest(JSONOperationWithArrays.ID)
                                                 .set("pojoList", list)
                                                 .set("whichPojo", "pojoList")
                                                 .execute();
        assertEquals(result1.getName(), "test1");
        result1 = (SimplePojo) session.newRequest(JSONOperationWithArrays.ID)
                                      .set("pojo", new SimplePojo("nico"))
                                      .set("whichPojo", "pojo")
                                      .execute();
        assertEquals(result1.getName(), "nico");
        result1 = (SimplePojo) session.newRequest(JSONOperationWithArrays.ID)
                                      .set("pojoListList", listList)
                                      .set("whichPojo", "pojoListList")
                                      .execute();
        assertEquals(result1.getName(), "test1");
        result1 = (SimplePojo) session.newRequest(JSONOperationWithArrays.ID)
                                      .set("pojoArray", simplePojos)
                                      .set("whichPojo", "pojoArray")
                                      .execute();
        assertEquals(result1.getName(), "test1");
        result1 = (SimplePojo) session.newRequest(JSONOperationWithArrays.ID)
                                      .set("pojoArray", new SimplePojo[] {})
                                      .set("whichPojo", "empty")
                                      .execute();
        assertNull(result1);
    }

    @Test
    public void testNumericalValuesAsInputAndOuput() throws Exception {
        Object result = session.newRequest(NestedJSONOperation.ID).setInput(4.3).execute();
        assertEquals(4, result);
    }

    @Test
    public void testDirtyProperties() throws Exception {
        // Initialize repository for this test
        setupComplexDocuments();

        Document testDoc = (Document) session.newRequest(DocumentService.GetDocumentChild)
                                             .setInput(new PathRef("/"))
                                             .set("name", "testDoc")
                                             .execute();

        // No need to use PropertyMap object anymore
        testDoc.set("ds:tableName", "newTableName");
        testDoc.set("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        testDoc.set("ds:fields", fieldsDataAsJSon);

        // Fetch the dirty properties (updated values) from the document
        PropertyMap dirties = testDoc.getDirties();

        // Check that dirty properties doesn't contain title
        Assert.assertFalse("The property dc:title should not be part of dirty properties",
                dirties.getKeys().contains("dc:title"));

        testDoc = (Document) session.newRequest(UpdateDocument.ID)
                                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                    .setInput(new IdRef(testDoc.getId()))
                                    .set("properties", dirties.toString())
                                    .execute();

        // check the returned doc with properties not updated
        assertEquals("testDoc", testDoc.getTitle());
        // check properties set previously
        assertEquals("newTableName", testDoc.getProperties().get("ds:tableName"));
    }

    /**
     * This test use {@link OperationRequest#set(String, Object)} passing in object
     * {@link org.nuxeo.ecm.automation.client.model.Document} to map directly an automation client document to requested
     * parameters of Properties type
     */
    @Test
    public void testSetDocumentOperationMethod() throws Exception {
        // Test document creation
        Document document = new Document("myfolder2", "Folder");
        document.set("dc:title", "My Test Folder");
        document.set("dc:description", "test");
        document.set("dc:subjects", "a,b,c\\,d");
        Document folder = (Document) session.newRequest(CreateDocument.ID)
                                            .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                            .setInput(automationTestFolder)
                                            .set("type", document.getType())
                                            .set("name", document.getId())
                                            .set("properties", document)
                                            .execute();

        assertEquals("My Test Folder", folder.getString("dc:title"));
        assertEquals("test", folder.getString("dc:description"));
        // Initialize repository for this document update test
        setupComplexDocuments();

        Document testDoc = (Document) session.newRequest(DocumentService.GetDocumentChild)
                                             .setInput(new PathRef("/"))
                                             .set("name", "testDoc")
                                             .execute();

        // No need to use PropertyMap object anymore
        testDoc.set("ds:tableName", "newTableName");
        testDoc.set("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        testDoc.set("ds:fields", fieldsDataAsJSon);

        // No need to get properties from the document, just pass document
        // testDoc into "properties" entry
        testDoc = (Document) session.newRequest(UpdateDocument.ID)
                                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                                    .setInput(new IdRef(testDoc.getId()))
                                    .set("properties", testDoc)
                                    .execute();

        // check the returned doc with properties not updated
        assertEquals("testDoc", testDoc.getTitle());
        // check properties set previously
        assertEquals("newTableName", testDoc.getProperties().get("ds:tableName"));
    }

    @Test
    public void testAutomationDocumentService() throws Exception {

        // Test with simple metadata

        // Test document creation
        Document document = new Document("myfolder2", "Folder");
        document.set("dc:title", "My Test Folder");
        document.set("dc:description", "test");
        document.set("dc:subjects", "a,b,c\\,d");
        DocumentService documentService = session.getAdapter(DocumentService.class);

        // automationTestFolder is the parent, we can pass the Path or Id
        Document folder = documentService.createDocument(automationTestFolder.getId(), document);

        assertEquals("My Test Folder", folder.getTitle());

        // Fetch the document with its dublincore properties
        folder = documentService.getDocument(folder, "dublincore");
        assertEquals("My Test Folder", folder.getString("dc:title"));
        assertEquals("test", folder.getString("dc:description"));

        // Update the document title
        folder.set("dc:title", "New Title");
        documentService.update(folder);

        // Check if title has been updated
        folder = documentService.getDocument(folder, "*");
        assertEquals("New Title", folder.getString("dc:title"));

        // Test with complex metadata

        // Initialize repository for this test
        setupComplexDocuments();

        Document testDoc = documentService.getDocument(new PathRef("/testDoc"), "*");

        testDoc.set("ds:tableName", "newTableName");
        testDoc.set("ds:attachments", "new1,new2,new3,new4");
        PropertyList dbFields = testDoc.getProperties().getList("ds:fields");
        assertEquals(5, dbFields.size());
        PropertyMap dbField0 = dbFields.getMap(0);
        assertNotNull(dbField0);
        assertEquals("field0", dbField0.getString("name"));
        assertEquals("Decision", dbField0.getList("roles").getString(0));
        assertEquals("Score", dbField0.getList("roles").getString(1));

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = FileUtils.readFile(fieldAsJsonFile);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        testDoc.set("ds:fields", fieldsDataAsJSon);

        documentService.update(testDoc);

        testDoc = documentService.getDocument(testDoc, "*");

        // check the returned doc
        assertEquals("testDoc", testDoc.getTitle());
        assertEquals("newTableName", testDoc.getProperties().get("ds:tableName"));

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

    /**
     * 'pojo client side' <--- mapping ----> 'adapter server side' test
     */
    @Test
    public void testAutomationBusinessObjects() throws Exception {
        // Test for pojo <-> adapter automation creation
        BusinessBean note = new BusinessBean("Note", "File description", "Note Content", "Note", new String("object"));
        BusinessService<BusinessBean> businessService = session.getAdapter(BusinessService.class);
        assertNotNull(businessService);

        // Marshaller for bean 'note' registration
        client.registerPojoMarshaller(note.getClass());

        note = (BusinessBean) session.newRequest(BusinessCreateOperation.ID)
                                     .setInput(note)
                                     .set("name", note.getTitle())
                                     .set("parentPath", "/")
                                     .execute();
        assertNotNull(note);
        // Test for pojo <-> adapter automation update
        // Fetching the business adapter model
        note = (BusinessBean) session.newRequest(BusinessFetchOperation.ID).setInput(note).execute();
        assertNotNull(note.getId());
        note.setTitle("Update");
        note = (BusinessBean) session.newRequest(BusinessUpdateOperation.ID).setInput(note).execute();
        assertEquals("Update", note.getTitle());
    }

    /**
     * 'pojo client side' <--- mapping ----> 'adapter server side' test with BusinessService
     */
    @Test
    public void testAutomationBusinessObjectsWithService() throws Exception {
        // Test for pojo <-> adapter automation creation
        BusinessBean note = new BusinessBean("Note", "File description", "Note Content", "Note", new String("object"));
        BusinessService<BusinessBean> businessService = session.getAdapter(BusinessService.class);
        assertNotNull(businessService);

        // Marshaller for bean 'note' is registered on the fly
        note = businessService.create(note, note.getTitle(), "/");
        assertNotNull(note);
        // Fetching the business adapter model
        note = businessService.fetch(note);
        assertNotNull(note.getId());
        // Test for pojo <-> adapter automation update
        note.setTitle("Update");
        note = businessService.update(note);
        assertEquals("Update", note.getTitle());
    }

    @Test
    public void logAndThenQueryNoMapping() throws Exception {

        org.junit.Assert.assertNotNull(session);

        OperationRequest logRequest = session.newRequest(AuditLog.ID, new HashMap<String, Object>());

        logRequest.getParameters().put("event", "testing");
        logRequest.setInput(new PathRef("/"));
        logRequest.execute();

        OperationRequest queryRequest = session.newRequest(AuditPageProviderOperation.ID, new HashMap<String, Object>());

        queryRequest.getParameters().put("providerName", "AUDIT_BROWSER");
        Object result = queryRequest.execute();
        JsonNode node = (JsonNode) result;

        // System.out.println(result.toString());
        int count = node.get("currentPageSize").getValueAsInt();
        JsonNode entries = node.get("entries");
        for (int i = 0; i < count; i++) {
            org.junit.Assert.assertEquals("logEntry", entries.get(i).get("entity-type").getValueAsText());
        }

    }

    @Test
    public void testAutomationBusinessObjectsArray() throws Exception {
        BusinessBean[] businessBeans = new BusinessBean[2];
        BusinessService<BusinessBean> businessService = session.getAdapter(BusinessService.class);
        assertNotNull(businessService);

        // Marshaller for array 'businessBeans' registration
        client.registerPojoMarshaller(businessBeans.getClass());

        businessBeans = (BusinessBean[]) session.newRequest(TestBusinessArray.ID).execute();
        assertNotNull(businessBeans);
        assertEquals(2, businessBeans.length);
    }

    @Test
    public void testRemoteException() throws Exception {
        // Check if the json stack display activation operation exists
        OperationDocumentation opd = session.getOperation(JsonStackToggleDisplayOperation.ID);
        assertNotNull(opd);
        try {
            // get a wrong doc
            Document wrongDoc = (Document) session.newRequest(FetchDocument.ID).set("value", "/test").execute();
            fail("Unexpected " + wrongDoc);
        } catch (RemoteException e) {
            assertNotNull(e);
            assertNotNull(e.getRemoteStackTrace());
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void shouldReturnCustomHttpStatusWhenFailure() throws Exception {
        try {
            session.newRequest(HttpStatusOperationTest.ID).set("isFailing", true).execute();
            fail();
        } catch (RemoteException e) {
            assertNotNull(e);
            RemoteThrowable cause = (RemoteThrowable) e.getRemoteCause();
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = (RemoteThrowable) cause.getCause();
            }
            assertEquals("Exception Message", cause.getMessage());
            assertEquals(ExceptionTest.class.getCanonicalName(), cause.getOtherNodes().get("className").getTextValue());
            assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getStatus());
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * @since 7.3
     */
    @Test
    public void shouldWriteAutomationContextWithDocuments() throws IOException {
        Document root = (Document) super.session.newRequest(FetchDocument.ID).set("value", "/").execute();
        OperationRequest request = session.newRequest("RunOperationOnList");
        // Set document array list to inject into the context through automation client
        List<Document> list = new ArrayList<>();
        list.add(root);
        request.setContextProperty("users", list)
               .set("isolate", "true")
               .set("id", "TestContext")
               .set("list", "users")
               .set("item", "document");
        request.execute();
    }

    /**
     * @since 7.4
     */
    @Test
    public void shouldReadContentEnricher() throws IOException {
        Document root = (Document) super.session.newRequest(FetchDocument.ID)
                                                .setHeader("X-NXenrichers.document", "breadcrumb")
                                                .set("value", "/")
                                                .execute();
        assertNotNull(root.getContextParameters());
        assertEquals(1, root.getContextParameters().size());
        assertEquals("documents", ((PropertyMap) root.getContextParameters().get("breadcrumb")).get("entity-type"));
    }

    /**
     * @since 7.4
     */
    @Test
    public void canSendCalendarParameters() throws IOException {
        Document root = (Document) super.session.newRequest(FetchDocument.ID).set("value", "/").execute();
        OperationRequest request = session.newRequest(AddPermission.ID);
        GregorianCalendar begin = new GregorianCalendar(2015, Calendar.JUNE, 20, 12, 34, 56);
        GregorianCalendar end = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        request.setInput(root)
               .set("username", "members")
               .set("permission", "Write")
               .set("begin", begin)
               .set("end", end)
               .execute();
        // TODO NXP-17232 to use context parameters in json payload response with automation and automation client.
        // Once NXP-17232 resolved: assertions possible to get related doc ACLs.
    }

    /**
     * @since 8.3
     */
    @Test
    public void testContextInjection() throws IOException {
        Document root = (Document) session.newRequest(FetchDocument.ID).set("value", "/").execute();
        Document folder = (Document) session.newRequest(ContextInjectionOperation.ID)
                .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                .setInput(root)
                // Check for context null property marshalling
                .setContextProperty("description", null)
                .setContextProperty("title", "hello")
                .execute();
        assertEquals("hello", folder.getString("dc:title"));
        assertNull(folder.getString("dc:description"));
    }
}
