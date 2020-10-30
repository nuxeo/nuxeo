/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.server.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.automation.test.HttpAutomationRequest.ENTITY_TYPE;
import static org.nuxeo.ecm.automation.test.HttpAutomationRequest.ENTITY_TYPE_DOCUMENTS;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.business.BusinessCreateOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessFetchOperation;
import org.nuxeo.ecm.automation.core.operations.business.BusinessUpdateOperation;
import org.nuxeo.ecm.automation.core.operations.document.AddPermission;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChild;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.operations.services.AuditLog;
import org.nuxeo.ecm.automation.core.operations.services.AuditPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.query.DocumentPaginatedQuery;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.server.AutomationServerComponent;
import org.nuxeo.ecm.automation.server.test.business.client.BusinessBean;
import org.nuxeo.ecm.automation.server.test.business.client.TestBusinessArray;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;
import org.nuxeo.ecm.automation.server.test.json.NestedJSONOperation;
import org.nuxeo.ecm.automation.server.test.json.POJOObject;
import org.nuxeo.ecm.automation.server.test.json.SimplePojoObjectCodec;
import org.nuxeo.ecm.automation.server.test.operations.ContextInjectionOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationRequest;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.automation.test.adapter.BusinessBeanAdapter;
import org.nuxeo.ecm.automation.test.helpers.HttpStatusOperationTest;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.platform.url")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.notification:OSGI-INF/NotificationService.xml")
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.automation.test:test-bindings.xml")
@Deploy("org.nuxeo.ecm.automation.test:test-mvalues.xml")
@Deploy("org.nuxeo.ecm.automation.test:operation-contrib.xml")
@Features({ EmbeddedAutomationServerFeature.class, AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class EmbeddedAutomationClientTest extends AbstractAutomationClientTest {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    UserManager userManager;

    @Inject
    private TransactionalFeature txFeature;

    @BeforeClass
    public static void setupCodecs() throws Exception {
        Framework.getService(ObjectCodecService.class).addCodec(new MyObjectCodec());
        Framework.getService(AutomationService.class).putOperation(MyObjectOperation.class);
        // Fire application start on AutomationServer component forcing to load
        // correctly Document Adapter Codec in Test scope (to take into account
        // of document adapters contributed into test) -> see execution order
        // here: org.nuxeo.runtime.test.runner.RuntimeFeature.start()
        ComponentInstance componentInstance = Framework.getRuntime()
                                                       .getComponentInstance(
                                                               "org.nuxeo.ecm.automation.server.AutomationServer");
        AutomationServerComponent automationServerComponent = (AutomationServerComponent) componentInstance.getInstance();
        automationServerComponent.start(componentInstance);
    }

    /**
     * Use to setup complex documents for related tests
     */
    public void setupComplexDocuments() throws Exception {
        // Fill the document properties
        Map<String, Object> creationProps = new HashMap<>();
        creationProps.put("ds:tableName", "MyTable");
        creationProps.put("ds:attachments", attachments);

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = org.apache.commons.io.FileUtils.readFileToString(fieldAsJsonFile, UTF_8);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        creationProps.put("ds:fields", fieldsDataAsJSon);
        creationProps.put("dc:title", "testDoc");

        // Document creation
        session.newRequest(CreateDocument.ID)
               .setInput("doc:/")
               .set("type", "DataSet")
               .set("name", "testDoc")
               .set("properties", session.propertyMapToString(creationProps))
               .execute();
    }

    @BeforeClass
    public static void addDataCapsuleOperation() throws OperationException {
        Framework.getService(AutomationService.class).putOperation(TestDataCapsule.class);
    }

    @Test
    public void testBlobSummaries() throws Exception {
        Blob blob = session.newRequest(TestDataCapsule.ID)//
                           .executeReturningBlob();
        assertEquals("TestDataCapsule", blob.getFilename());
        assertEquals("application/json", blob.getMimeType());
        assertEquals(25, blob.getLength());
    }

    @Test
    public void testCodecs() throws Exception {
        JsonNode msg = session.newRequest(MyObjectOperation.ID) //
                              .execute();
        assertEquals("msg", msg.get(ENTITY_TYPE).asText());
        assertEquals("hello world", msg.get("value").get("message").asText());
    }

    @Test
    public void testMultiValued() throws Exception {
        JsonNode note = session.newRequest(CreateDocument.ID)
                               .setHeader(DOCUMENT_PROPERTIES, "*")
                               .setInput("doc:/")
                               .set("type", "MV")
                               .set("name", "pfff")
                               .set("properties", "mv:sl=s1,s2\nmv:ss=s1,s2\nmv:bl=true,false\nmv:b=true\n")
                               .executeReturningDocument();
        checkHasCorrectMultiValues(note);

        List<JsonNode> docs = session.newRequest(DocumentPaginatedQuery.ID)
                                     .setHeader(DOCUMENT_PROPERTIES, "*")
                                     .set("query", "SELECT * from MV")
                                     .set("queryParams", new String[] {})
                                     .set("pageSize", 2)
                                     .executeReturningDocuments();

        assertNotNull(docs);
        assertEquals(1, docs.size());
        checkHasCorrectMultiValues(docs.get(0));
    }

    private void checkHasCorrectMultiValues(JsonNode note) {
        assertNotNull(note);
        JsonNode properties = getProperties(note);
        assertNotNull(properties);

        JsonNode sl = properties.get("mv:sl");
        assertNotNull(sl);
        assertTrue(sl.isArray());
        assertEquals("[\"s1\",\"s2\"]", sl.toString());

        JsonNode ss = properties.get("mv:ss");
        assertNotNull(ss);
        assertTrue(ss.isArray());
        assertEquals("[\"s1\",\"s2\"]", ss.toString());

        JsonNode b = properties.get("mv:b");
        assertNotNull(b);
        assertTrue(b.isBoolean());
        assertEquals("true", b.toString());

        JsonNode bl = properties.get("mv:bl");
        assertNotNull(bl);
        assertTrue(ss.isArray());
        assertEquals("[true,false]", bl.toString());
    }

    /**
     * test a chain invocation
     */
    @Test
    public void testChain() throws Exception {
        // create a folder
        JsonNode folder = session.newRequest(CreateDocument.ID)
                                 .setInput("doc:/")
                                 .set("type", "Folder")
                                 .set("name", "chainTest")
                                 .executeReturningDocument();

        JsonNode doc = session.newRequest("testchain") //
                              .setInput(folder)
                              .executeReturningDocument();
        assertEquals("/chainTest/chain.doc", getPath(doc));
        assertEquals("Note", doc.get("type").asText());

        // fetch again the note
        doc = session.newRequest(FetchDocument.ID) //
                     .set("value", doc)
                     .executeReturningDocument();
        assertEquals("/chainTest/chain.doc", getPath(doc));
        assertEquals("Note", doc.get("type").asText());
    }

    /**
     * We allow to call chain operation since 5.7.2. Test on it.
     */
    @Test
    public void testRemoteChain() throws Exception {
        JsonNode doc = session.newRequest("testchain") //
                              .setInput("doc:/")
                              .executeReturningDocument();
        assertNotNull(doc);
    }

    @Test
    public void testTxTimeout() throws Exception {
        session.newRequest(WaitForTxTimeoutOperation.ID) //
               .setHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY, "1")
               .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testBaseInputAndReturnValues() throws Exception {
        boolean b = session.newRequest(ReturnOperation.ID) //
                           .setInput(Boolean.TRUE)
                           .executeReturningBooleanEntity();
        assertTrue(b);

        String s = session.newRequest(ReturnOperation.ID) //
                          .setInput("hello")
                          .executeReturningStringEntity();
        assertEquals("hello", s);

        Number n = session.newRequest(ReturnOperation.ID) //
                          .setInput(1)
                          .executeReturningNumberEntity();
        assertEquals(1, n.intValue());

        n = session.newRequest(ReturnOperation.ID) //
                   .setInput(1000000000000000000L)
                   .executeReturningNumberEntity();
        assertEquals(1000000000000000000L, n.longValue());

        n = session.newRequest(ReturnOperation.ID) //
                   .setInput(1.1d)
                   .executeReturningNumberEntity();
        assertEquals(1.1d, n.doubleValue(), 0.1);

        Date now = new Date(0);
        Instant i = session.newRequest(ReturnOperation.ID) //
                           .setInput(now)
                           .executeReturningDateEntity();
        assertEquals(now.toInstant(), i);
    }

    @Test
    public void testNumberParamAdapters() throws Exception {
        // Long parameter
        Long longParam = 500L;
        Number n = session.newRequest(TestNumberParamAdaptersOperation.ID) //
                          .set("longParam", longParam)
                          .executeReturningNumberEntity();
        assertEquals(500, n.intValue());
        // Integer parameter
        Integer integerParam = 500;
        n = session.newRequest(TestNumberParamAdaptersOperation.ID) //
                   .set("longParam", integerParam)
                   .executeReturningNumberEntity();
        assertEquals(500, n.intValue());
    }

    /**
     * test a chain rollback
     */
    @Test
    public void testChainRollback() throws Exception {
        // 1. create a note and exit gracefully
        JsonNode doc = session.newRequest("exitNoRollback") //
                              .setInput("doc:/")
                              .executeReturningDocument();
        assertEquals("/test-exit1", getPath(doc));
        JsonNode note = session.newRequest(FetchDocument.ID) //
                               .set("value", "/test-exit1")
                               .executeReturningDocument();
        assertEquals("/test-exit1", getPath(note));

        // 2. create a note and exit with rollback
        doc = session.newRequest("exitRollback") //
                     .setInput("doc:/")
                     .executeReturningDocument();
        assertEquals("/test-exit2", getPath(doc));
        String error = session.newRequest(FetchDocument.ID) //
                              .set("value", "/test-exit2")
                              .executeReturningExceptionEntity(SC_NOT_FOUND);
        assertEquals("Failed to invoke operation: Repository.GetDocument, /test-exit2", error);

        // 3. create a note and exit with error (+rollback)
        error = session.newRequest("exitError") //
                       .setInput("doc:/")
                       .executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: exitError", error);
        // test the note was not created
        error = session.newRequest(FetchDocument.ID) //
                       .set("value", "/test-exit3")
                       .executeReturningExceptionEntity(SC_NOT_FOUND);
        assertEquals("Failed to invoke operation: Repository.GetDocument, /test-exit3", error);
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
        org.apache.commons.io.FileUtils.writeLines(file, mailProperties);

        HttpAutomationRequest operationRequest = session.newRequest(SendMail.ID)
                                                   .setInput("doc:/")
                                                   .set("from", "sender@nuxeo.com")
                                                   .set("to", "recipient@nuxeo.com")
                                                   .set("subject", "My test mail")
                                                   .set("message", "The message content.");

        // Call SendMail with rollbackOnError = true (default value)
        String error = operationRequest.executeReturningExceptionEntity(SC_INTERNAL_SERVER_ERROR);
        assertEquals("Failed to invoke operation: Document.Mail", error);

        // Call SendMail with rollbackOnError = false
        // => should only log a WARNING
        JsonNode doc = operationRequest.set("rollbackOnError", "false") //
                                       .executeReturningDocument();
        assertEquals("/", getPath(doc));
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
            } catch (AssertionError e) {
                // Bad credentials should be mapped to HTTP 401
                assertEquals("expected:<200> but was:<401>", e.getMessage());
            }

            // test user does not have the permission to access the root
            HttpAutomationSession userSession = client.getSession(testUserName, "secret");
            String error = userSession.newRequest(FetchDocument.ID) //
                                      .set("value", "/")
                                      .executeReturningExceptionEntity(SC_FORBIDDEN);
            assertEquals("Failed to invoke operation: Repository.GetDocument, "
                    + "Privilege 'Read' is not granted to 'automation-test-user'", error);
        } finally {
            userManager.deleteUser(testUserName);
        }
    }

    @Test
    public void sampleAutomationRemoteAccessWithComplexDocuments() throws Exception {

        // Initialize repository for this test
        setupComplexDocuments();

        // the repository init handler sould have created a sample doc in the repo
        // Check that we see it
        JsonNode testDoc = session.newRequest(GetDocumentChild.ID)
                                  .setInput("doc:/")
                                  .set("name", "testDoc")
                                  .executeReturningDocument();
        assertNotNull(testDoc);

        // try to see what's in it

        // check dublincore.title
        assertEquals("testDoc", getTitle(testDoc));

        // schema only a subset of the properties are serialized with default
        // configuration (common and dublincore only)
        // see @Constants.HEADER_NX_SCHEMAS

        JsonNode properties = getProperties(testDoc);
        assertNull(properties);

        // refetch the doc, but with the correct header
        testDoc = session.newRequest(FetchDocument.ID)
                         .setHeader(DOCUMENT_PROPERTIES, "*")
                         .set("value", "/testDoc")
                         .executeReturningDocument();
        assertNotNull(testDoc);

        assertEquals("testDoc", getTitle(testDoc));
        properties = getProperties(testDoc);
        assertEquals("MyTable", properties.get("ds:tableName").asText());
        JsonNode dbFields = properties.get("ds:fields");
        assertNotNull(dbFields);
        assertEquals(5, dbFields.size());

        JsonNode dbField0 = dbFields.get(0);
        assertNotNull(dbField0);
        assertEquals("field0", dbField0.get("name").asText());

        assertEquals("Decision", dbField0.get("roles").get(0).asText());
        assertEquals("Score", dbField0.get("roles").get(1).asText());

        // now update the doc
        Map<String, Object> updateProps = new HashMap<>();

        updateProps.put("ds:tableName", "newTableName");
        updateProps.put("ds:attachments", "new1,new2,new3,new4");

        // send the fields representation as json
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("updateFields.json");
        assertNotNull(fieldAsJsonFile);
        String fieldsDataAsJSon = org.apache.commons.io.FileUtils.readFileToString(fieldAsJsonFile, UTF_8);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        updateProps.put("ds:fields", fieldsDataAsJSon);

        testDoc = session.newRequest(UpdateDocument.ID)
                         .setHeader(DOCUMENT_PROPERTIES, "*")
                         .setInput(testDoc)
                         .set("properties", updateProps)
                         .executeReturningDocument();

        // check the returned doc
        assertEquals("testDoc", getTitle(testDoc));
        properties = getProperties(testDoc);
        assertEquals("newTableName", properties.get("ds:tableName").asText());

        JsonNode atts = properties.get("ds:attachments");
        assertNotNull(atts);
        assertEquals(4, atts.size());
        assertEquals("new1", atts.get(0).asText());
        assertEquals("new4", atts.get(3).asText());

        dbFields = properties.get("ds:fields");
        assertEquals(2, dbFields.size());

        JsonNode dbFieldA = dbFields.get(0);
        assertNotNull(dbFieldA);
        assertEquals("fieldA", dbFieldA.get("name").asText());
    }

    /* The following tests need automation server 5.7 or later */

    @Test
    public void testRawJSONDatastructuresAsParameters() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        POJOObject obj1 = new POJOObject("[obj1 text]", Arrays.asList("1", "2"));
        POJOObject obj2 = new POJOObject("[obj2 text]", Arrays.asList("2", "3"));

        String obj1JSON = mapper.writeValueAsString(obj1);
        String obj2JSON = mapper.writeValueAsString(obj2);

        @SuppressWarnings("unchecked")
        Map<String, Object> map1 = mapper.readValue(obj1JSON, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map2 = mapper.readValue(obj2JSON, Map.class);

        // Expected result when passing obj1 and obj2 as input to the
        POJOObject expectedObj12 = new POJOObject("Merged texts: [obj1 text][obj2 text]",
                Arrays.asList("1", "2", "2", "3"));

        // The pojo and the map parameters can be passed as java objects
        // directly in the client call, the generic Jackson-based parser /
        // serialization will be used
        POJOObject returnedObj12 = session.newRequest(NestedJSONOperation.ID) //
                                          .set("pojo", obj1)
                                          .set("map", map2)
                                          .executeReturningEntity(POJOObject.class);
        assertEquals(expectedObj12, returnedObj12);

        // It is also possible to pass alternative Java representation of the
        // input parameters as long as they share the same JSON representation
        // for the transport.
        returnedObj12 = session.newRequest(NestedJSONOperation.ID) //
                               .set("pojo", map1)
                               .set("map", obj2)
                               .executeReturningEntity(POJOObject.class);
        assertEquals(expectedObj12, returnedObj12);

        // Check scalar parameters can be passed as argument
        POJOObject expectedObj1AndDouble = new POJOObject("Merged texts: [obj1 text]", Arrays.asList("1", "2", "3.0"));
        POJOObject returnedObj1AndDouble = session.newRequest(NestedJSONOperation.ID) //
                                                  .set("pojo", map1)
                                                  .set("doubleParam", 3.0)
                                                  .executeReturningEntity(POJOObject.class);
        assertEquals(expectedObj1AndDouble, returnedObj1AndDouble);
    }

    @Test
    public void testRawJSONDatastructuresAsInput() throws Exception {
        // It is possible to pass arbitrary Java objects as the input as
        // long as the JSON representation is a valid representation for the
        // expected input type of the operation
        POJOObject expectedListObj = new POJOObject("Merged texts: ", Arrays.asList("a", "b", "c"));
        POJOObject returnedListObj = session.newRequest(NestedJSONOperation.ID) //
                                            .setInput(Arrays.asList("a", "b", "c"))
                                            .executeReturningEntity(POJOObject.class);
        assertEquals(expectedListObj, returnedListObj);

        // Try with alternative input type datastructures to check input type
        // negotiation: note, as no special codec has been rejustered for
        // POJOObject, the operation must be able to consume Map instances with
        // the same inner structure as the POJOObject class.
        POJOObject pojoInput = new POJOObject("input pojo", Arrays.asList("a", "b", "c"));
        returnedListObj = session.newRequest(NestedJSONOperation.ID) //
                                 .setInput(pojoInput)
                                 .executeReturningEntity(POJOObject.class);
        assertEquals(expectedListObj, returnedListObj);

        // Pojo can be mapped to java Map datastructure and passed as input to
        // operations
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> mapInput = mapper.convertValue(pojoInput, Map.class);
        returnedListObj = session.newRequest(NestedJSONOperation.ID) //
                                 .setInput(mapInput)
                                 .executeReturningEntity(POJOObject.class);
        assertEquals(expectedListObj, returnedListObj);
    }

    @Test
    public void testArraysAsParametersAndResult() throws Exception {
        List<SimplePojo> list = new ArrayList<>();
        list.add(new SimplePojo("test1"));
        list.add(new SimplePojo("test2"));
        list.add(new SimplePojo("test3"));
        List<List<SimplePojo>> listList = new ArrayList<>();
        listList.add(list);
        SimplePojo[] simplePojos = list.toArray(new SimplePojo[list.size()]);
        String type = SimplePojoObjectCodec.TYPE;
        SimplePojo result1;
        result1 = session.newRequest(JSONOperationWithArrays.ID)
                         .set("pojoList", list, SimplePojo.class, type)
                         .set("whichPojo", "pojoList")
                         .executeReturningEntity(SimplePojo.class, type);
        assertEquals(result1.getName(), "test1");
        result1 = session.newRequest(JSONOperationWithArrays.ID)
                         .set("pojo", new SimplePojo("nico"), SimplePojo.class, type)
                         .set("whichPojo", "pojo")
                         .executeReturningEntity(SimplePojo.class, type);
        assertEquals(result1.getName(), "nico");
        result1 = session.newRequest(JSONOperationWithArrays.ID)
                         .set("pojoListList", listList, SimplePojo.class, type)
                         .set("whichPojo", "pojoListList")
                         .executeReturningEntity(SimplePojo.class, type);
        assertEquals(result1.getName(), "test1");
        result1 = session.newRequest(JSONOperationWithArrays.ID)
                         .set("pojoArray", simplePojos, SimplePojo.class, type)
                         .set("whichPojo", "pojoArray")
                         .executeReturningEntity(SimplePojo.class, type);
        assertEquals(result1.getName(), "test1");
        result1 = session.newRequest(JSONOperationWithArrays.ID)
                         .set("pojoArray", new SimplePojo[] {}, SimplePojo.class, type)
                         .set("whichPojo", "empty")
                         .executeReturningEntity(SimplePojo.class, type);
        assertNull(result1);
    }

    @Test
    public void testNumericalValuesAsInputAndOuput() throws Exception {
        Number n = session.newRequest(NestedJSONOperation.ID) //
                          .setInput(4.3)
                          .executeReturningNumberEntity();
        assertEquals(4, n.intValue());
    }

    /**
     * 'pojo client side' <--- mapping ----> 'adapter server side' test
     */
    @Test
    public void testAutomationBusinessObjects() throws Exception {
        // Test for pojo <-> adapter automation creation
        BusinessBean note = new BusinessBean("Note", "File description", "Note Content", "Note", "object");
        // adapter is registered through XML
        String type = BusinessBeanAdapter.class.getSimpleName();
        note = session.newRequest(BusinessCreateOperation.ID)
                      .setInput(note, BusinessBean.class, type)
                      .set("name", note.getTitle())
                      .set("parentPath", "/")
                      .executeReturningEntity(BusinessBean.class, type);
        assertNotNull(note);
        // Test for pojo <-> adapter automation update
        // Fetching the business adapter model
        note = session.newRequest(BusinessFetchOperation.ID) //
                      .setInput(note, BusinessBean.class, type)
                      .executeReturningEntity(BusinessBean.class, type);
        assertNotNull(note.getId());
        note.setTitle("Update");
        note = session.newRequest(BusinessUpdateOperation.ID) //
                      .setInput(note, BusinessBean.class, type)
                      .executeReturningEntity(BusinessBean.class, type);
        assertEquals("Update", note.getTitle());
    }

    @Test
    public void logAndThenQueryNoMapping() throws Exception {
        session.newRequest(AuditLog.ID) //
               .setInput("doc:/")
               .set("event", "testing")
               .execute();
        JsonNode node = session.newRequest(AuditPageProviderOperation.ID) //
                               .set("providerName", "AUDIT_BROWSER")
                               .execute();
        int count = node.get("currentPageSize").asInt();
        JsonNode entries = node.get("entries");
        for (int i = 0; i < count; i++) {
            assertEquals("logEntry", entries.get(i).get(ENTITY_TYPE).asText());
        }
    }

    @Test
    public void testAutomationBusinessObjectsArray() throws Exception {
        BusinessBean[] businessBeans = session.newRequest(TestBusinessArray.ID) //
                                              .executeReturningEntity(BusinessBean[].class);
        assertNotNull(businessBeans);
        assertEquals(2, businessBeans.length);
    }

    @Test
    public void testRemoteException() throws Exception {
        String error = session.newRequest(FetchDocument.ID) //
                              .set("value", "/test")
                              .executeReturningExceptionEntity(SC_NOT_FOUND);
        assertEquals("Failed to invoke operation: Repository.GetDocument, /test", error);
    }

    /**
     * @since 7.1
     */
    @Test
    public void shouldReturnCustomHttpStatusWhenFailure() throws Exception {
        String error = session.newRequest(HttpStatusOperationTest.ID)//
                              .set("isFailing", true)
                              .executeReturningExceptionEntity(SC_METHOD_NOT_ALLOWED);
        assertEquals("Failed to invoke operation: Test.HttpStatus", error);
    }

    /**
     * @since 7.3
     */
    @Test
    public void shouldWriteAutomationContextWithDocuments() throws IOException {
        JsonNode root = session.newRequest(FetchDocument.ID) //
                               .set("value", "/")
                               .executeReturningDocument();
        // Set document array list to inject into the context
        session.newRequest(RunOperationOnList.ID) //
               .setContextParameter("users", List.of(getDocRef(root)))
               .set("isolate", "true")
               .set("id", TestContextReaderOperation.ID)
               .set("list", "users")
               .set("item", "document")
               .execute();
    }

    /**
     * @since 7.4
     */
    @Test
    public void shouldReadContentEnricher() throws IOException {
        JsonNode node = session.newRequest(FetchDocument.ID) //
                               .setHeader("X-NXenrichers.document", "breadcrumb")
                               .set("value", "/")
                               .executeReturningDocument();
        JsonNode context = node.get("contextParameters");
        assertNotNull(context);
        JsonNode breadcrumb = context.get("breadcrumb");
        assertNotNull(breadcrumb);
        assertEquals(ENTITY_TYPE_DOCUMENTS, breadcrumb.get(ENTITY_TYPE).asText());
    }

    @Test
    public void canSendCalendarParameters() throws IOException {
        canSendCalendarParameters("existingMembers");
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-allow-virtual-user.xml")
    public void canSendCalendarParametersIfUserNotFound() throws IOException {
        ConfigurationService configService = Framework.getService(ConfigurationService.class);
        assertTrue(configService.getBoolean(AddPermission.ALLOW_VIRTUAL_USER).orElseThrow(AssertionError::new));

        canSendCalendarParameters("nonExistentMembers");
    }

    @Test
    public void cannotSendCalendarParametersIfUserNotFound() throws IOException {
        cannotSendCalendarParameters("nonExistentMembers");
    }

    private void cannotSendCalendarParameters(String username) throws IOException {
        GregorianCalendar begin = new GregorianCalendar(2015, Calendar.JUNE, 20, 12, 34, 56);
        GregorianCalendar end = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        String error = session.newRequest(AddPermission.ID)
                              .setInput("doc:/")
                              .set("username", username)
                              .set("permission", "Write")
                              .set("begin", begin)
                              .set("end", end)
                              .executeReturningExceptionEntity(SC_BAD_REQUEST);
        String expectedMsg = String.format(
                "Failed to invoke operation: Document.AddPermission, "
                        + "Failed to invoke operation Document.AddPermission with aliases [Document.AddACL], "
                        + "The following set of User or Group names do not exist: [%s]. Please provide valid ones.",
                username);
        assertEquals(expectedMsg, error);
    }

    private void canSendCalendarParameters(String username) throws IOException {
        // Setup
        DocumentModel testUser = userManager.getBareUserModel();
        testUser.setProperty("user", "username", username);
        testUser = userManager.createUser(testUser);
        assertNotNull(testUser.getId());
        txFeature.nextTransaction();

        try {
            GregorianCalendar begin = new GregorianCalendar(2015, Calendar.JUNE, 20, 12, 34, 56);
            GregorianCalendar end = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
            session.newRequest(AddPermission.ID)
                   .setInput("doc:/")
                   .set("username", username)
                   .set("permission", "Write")
                   .set("begin", begin)
                   .set("end", end)
                   .execute();
            // TODO NXP-17232 to use context parameters in json payload response with automation and automation client.
            // Once NXP-17232 resolved: assertions possible to get related doc ACLs.
        } finally {
            // Tear down
            userManager.deleteUser(testUser.getId());
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void testContextInjection() throws IOException {
        JsonNode folder = session.newRequest(ContextInjectionOperation.ID)
                                 .setHeader(DOCUMENT_PROPERTIES, "*")
                                 .setInput("doc:/")
                                 // Check for context null property marshalling
                                 .setContextParameter("description", null)
                                 .setContextParameter("title", "hello")
                                 .executeReturningDocument();
        JsonNode properties = getProperties(folder);
        assertEquals("hello", properties.get("dc:title").asText());
        assertTrue(properties.get("dc:description").isNull());
    }

}
