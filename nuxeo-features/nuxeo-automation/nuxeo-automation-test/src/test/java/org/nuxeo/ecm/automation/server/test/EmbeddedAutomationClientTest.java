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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.number.IsCloseTo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PojoMarshaller;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.DateUtils;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.server.jaxrs.io.ObjectCodecService;
import org.nuxeo.ecm.automation.server.test.json.NestedJSONOperation;
import org.nuxeo.ecm.automation.server.test.json.POJOObject;
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
        "org.nuxeo.ecm.automation.server:test-mvalues.xml" })
@Features(EmbeddedAutomationServerFeature.class)
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class EmbeddedAutomationClientTest extends AbstractAutomationClientTest {

    @Inject
    UserManager userManager;

    @BeforeClass
    public static void setupCodecs() throws OperationException {
        Framework.getLocalService(ObjectCodecService.class).addCodec(
                new MyObjectCodec());
        Framework.getLocalService(AutomationService.class).putOperation(
                MyObjectOperation.class);
    }

    @BeforeClass
    public static void addDataCapsuleOperation() throws OperationException {
        Framework.getLocalService(AutomationService.class).putOperation(
                TestDataCapsule.class);
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
                "X-NXDocumentProperties", "*").setInput(root).set("type", "MV").set(
                "name", "pfff").set("properties",
                "mv:sl=s1,s2\nmv:ss=s1,s2\nmv:bl=true,false\nmv:b=true\n").execute();
        checkHasCorrectMultiValues(note);

        PaginableDocuments docs = (PaginableDocuments) session.newRequest(
                DocumentPageProviderOperation.ID).setHeader(
                "X-NXDocumentProperties", "*").set("query", "SELECT * from MV").set(
                "pageSize", 2).execute();

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

    @Test
    public void testBaseInputAndReturnValues() throws Exception {
        Object r;
        r = session.newRequest(ReturnOperation.ID).setInput(Boolean.TRUE).execute();
        assertThat((Boolean) r, is(Boolean.TRUE));

        r = session.newRequest(ReturnOperation.ID).setInput("hello").execute();
        assertThat((String) r, is("hello"));

        r = session.newRequest(ReturnOperation.ID).setInput(1).execute();
        assertThat(((Number) r).intValue(), is(1));

        r = session.newRequest(ReturnOperation.ID).setInput(
                1000000000000000000L).execute();
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
        r = session.newRequest(TestNumberParamAdaptersOperation.ID).set(
                "longParam", longParam).execute();
        assertThat((Integer) r, is(500));
        // Integer parameter
        Integer integerParam = 500;
        r = session.newRequest(TestNumberParamAdaptersOperation.ID).set(
                "longParam", integerParam).execute();
        assertThat((Integer) r, is(500));
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
        POJOObject expectedObj12 = new POJOObject(
                "Merged texts: [obj1 text][obj2 text]", Arrays.asList("1", "2",
                        "2", "3"));

        // The pojo and the map parameters can be passed as java objects
        // directly in the client call, the generic Jackson-based parser /
        // serialization will be used
        POJOObject returnedObj12 = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).set("pojo", obj1).set("map", map2).execute();
        assertEquals(expectedObj12, returnedObj12);

        // It is also possible to pass alternative Java representation of the
        // input parameters as long as they share the same JSON representation
        // for the transport.
        returnedObj12 = (POJOObject) session.newRequest(NestedJSONOperation.ID).set(
                "pojo", map1).set("map", obj2).execute();
        assertEquals(expectedObj12, returnedObj12);

        // Check scalar parameters can be passed as argument
        POJOObject expectedObj1AndDouble = new POJOObject(
                "Merged texts: [obj1 text]", Arrays.asList("1", "2", "3.0"));
        POJOObject returnedObj1AndDouble = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).set("pojo", map1).set("doubleParam",
                3.0).execute();
        assertEquals(expectedObj1AndDouble, returnedObj1AndDouble);
    }

    @Test
    public void testRawJSONDatastructuresAsInput() throws Exception {
        // It is possible to pass arbitrary Java objects as the input as
        // long as the JSON representation is a valid representation for the
        // expected input type of the operation
        POJOObject expectedListObj = new POJOObject("Merged texts: ",
                Arrays.asList("a", "b", "c"));
        POJOObject returnedListObj = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).setInput(Arrays.asList("a", "b", "c")).execute();
        assertEquals(expectedListObj, returnedListObj);

        // Try with alternative input type datastructures to check input type
        // negotiation: note, as no special codec has been rejustered for
        // POJOObject, the operation must be able to consume Map instances with
        // the same inner structure as the POJOObject class.
        POJOObject pojoInput = new POJOObject("input pojo", Arrays.asList("a",
                "b", "c"));
        returnedListObj = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).setInput(pojoInput).execute();
        assertEquals(expectedListObj, returnedListObj);

        // Pojo can be mapped to java Map datastructure and passed as input to
        // operations
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> mapInput = mapper.convertValue(pojoInput, Map.class);
        returnedListObj = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).setInput(mapInput).execute();
        assertEquals(expectedListObj, returnedListObj);

        // It is also possible to serialize an explicitly typed represenation of
        // the pojo if both the client and the server are expected to have the
        // same class definition available in their classloading context.
        JsonMarshalling.addMarshaller(PojoMarshaller.forClass(POJOObject.class));
        returnedListObj = (POJOObject) session.newRequest(
                NestedJSONOperation.ID).setInput(pojoInput).execute();
        assertEquals(expectedListObj, returnedListObj);
    }

    @Test
    public void testNumericalValuesAsInputAndOuput() throws Exception {
        Object result = session.newRequest(NestedJSONOperation.ID).setInput(4.3).execute();
        assertEquals(4, result);
    }

}
