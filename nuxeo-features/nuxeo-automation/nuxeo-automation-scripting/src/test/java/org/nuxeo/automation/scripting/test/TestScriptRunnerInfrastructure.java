/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>, Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */
package org.nuxeo.automation.scripting.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.script.ScriptException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.operation.ScriptingOperationTypeImpl;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.automation.scripting", "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting.tests:automation-scripting-contrib.xml",
        "org.nuxeo.ecm.automation.scripting.tests:core-types-contrib.xml" })
public class TestScriptRunnerInfrastructure {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private PrintStream outStream;

    @Inject
    TracerFactory factory;

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

    @Test
    public void serviceShouldBeDeclared() {
        AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(scriptingService);
    }

    @Test
    public void shouldExecuteSimpleScript() throws Exception {
        AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/simpleAutomationScript.js");
        assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("Documents Updated" + System.lineSeparator(), outContent.toString());
    }

    @Test
    public void simpleScriptingOperationShouldBeAvailable() throws Exception {

        OperationType type = automationService.getOperation("Scripting" + ".HelloWorld");
        assertNotNull(type);
        assertTrue(type instanceof ScriptingOperationTypeImpl);

        Param[] paramDefs = type.getDocumentation().getParams();
        assertEquals(1, paramDefs.length);

        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();

        params.put("lang", "en");
        ctx.setInput("John");
        Object result = automationService.run(ctx, "Scripting.HelloWorld", params);
        assertEquals("Hello John", result.toString());

        params.put("lang", "fr");
        ctx.setInput("John");
        result = automationService.run(ctx, "Scripting.HelloWorld", params);
        assertEquals("Bonjour John", result.toString());
    }

    @Test
    public void runOperationOnSubTree() throws Exception {

        DocumentModel root = session.getRootDocument();

        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "new" + i, "File");
            session.createDocument(doc);
        }

        session.save();
        DocumentModelList res = session.query("select * from File where  " + "ecm:mixinType = 'HiddenInNavigation'");
        assertEquals(0, res.size());

        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();

        params.put("facet", "HiddenInNavigation");
        params.put("type", "File");
        ctx.setInput(root);
        Object result = automationService.run(ctx, "Scripting.AddFacetInSubTree", params);
        DocumentModelList docs = (DocumentModelList) result;
        assertEquals(5, docs.size());
    }

    @Test
    public void simpleScriptingOperationsInChain() throws Exception {

        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();

        ctx.setInput("John");
        Object result = automationService.run(ctx, "Scripting.ChainedHello", params);
        assertEquals("Hello Bonjour John", result.toString());

    }

    @Test
    public void simpleCallToScriptingOperationsChain() throws Exception {

        AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/simpleCallToChain.js");
        assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("Hello Bonjour John" + System.lineSeparator(), outContent.toString());

    }

    @Test
    public void testMultiThread() throws InterruptedException {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.setInput("John");
        Thread t = new Thread(() -> {
            try {
                ctx.setInput("Vlad");
                Object result = automationService.run(ctx, "Scripting.ChainedHello", params);
                assertEquals("Hello Bonjour Vlad", result.toString());
            } catch (Exception e) {
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                Object result = automationService.run(ctx, "Scripting.ChainedHello", params);
                assertEquals("Hello Bonjour John", result.toString());
            } catch (Exception e) {
            }
        });
        t.start();
        t2.start();
        t.join();
        t2.join();
    }

    @Test
    public void testOperationCtx() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        ctx.put("test", "odd");
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationCtx", params);
        assertEquals("odd", result.getPropertyValue("dc:nature"));
        assertEquals("modifiedValue", result.getPropertyValue("dc:description"));
        assertEquals("newEntry", result.getPropertyValue("dc:title"));
        assertEquals("Administrator", result.getPropertyValue("dc:creator"));
    }

    @Test
    public void testOperationWithBlob() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(fb);
        Map<String, Object> params = new HashMap<>();
        params.put("document", "/newDoc");
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestBlob", params);
        assertEquals("creationFields.json", ((Blob) result.getPropertyValue("file:content")).getFilename());
        assertEquals("doc title:New Title" + System.lineSeparator() + "doc title:New Title" + System.lineSeparator()
                + "title:creationFields.json" + System.lineSeparator(), outContent.toString());
    }

    @Test
    public void testWithSetBlobOperation() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(fb);
        Map<String, Object> params = new HashMap<>();
        params.put("document", "/newDoc");
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestSetBlob", params);
        assertEquals("creationFields.json", ((Blob) result.getPropertyValue("file:content")).getFilename());
    }

    @Test
    public void testComplexProperties() throws IOException, OperationException {
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

        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        params.put("properties", toString(creationProps));
        params.put("type", "DataSet");
        params.put("name", "testDoc");
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestComplexProperties", params);
        assertEquals("whatever",
                ((Map<?, ?>) ((List<?>) result.getPropertyValue("ds:fields")).get(0)).get("sqlTypeHint"));
    }

    @Test
    public void testClassFilter() throws ScriptException, OperationException {
        AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/classFilterScript.js");
        assertNotNull(stream);
        try {
            scriptingService.run(stream, session);
            fail();
        } catch (RuntimeException e) {
            assertEquals("java.lang.ClassNotFoundException: java.io.File", e.getMessage());
        }
    }

    @Test
    public void testFn() throws ScriptException, OperationException {
        // Test platform functions injection
        AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/platformFunctions.js");
        assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("devnull@nuxeo.com" + System.lineSeparator(), outContent.toString());
    }

    public String toString(Map<String, Object> creationProps) {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, Object> entry : creationProps.entrySet()) {
            Object v = entry.getValue();
            if (v != null) {
                if (v.getClass() == String.class) {
                    buf.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                }
            } else {
                buf.append(entry.getKey()).append("=").append("\n");
            }
        }
        return buf.toString();
    }

    @Test
    public void handleDocumentListAsInput() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModelList result = (DocumentModelList) automationService.run(ctx, "Scripting.TestInputDocumentList",
                null);
        assertNotNull(result);
    }

    @Test
    public void handleWorkflowVariables() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> wfVars = new HashMap<>();
        Map<String, Object> nodeVars = new HashMap<>();
        wfVars.put("var", "workflow");
        nodeVars.put("var", "node");
        ctx.put(Constants.VAR_WORKFLOW, wfVars);
        ctx.put(Constants.VAR_WORKFLOW_NODE, nodeVars);
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationWF", null);
        assertEquals("workflow", result.getPropertyValue("dc:title"));
        assertEquals("node", result.getPropertyValue("dc:description"));
    }

    @Test
    public void canUseChainWithDashes() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestChainWithDashes", null);
        assertNotNull(result);
    }

    @Test
    public void canManageDocumentModelWrappers() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        root.setPropertyValue("dc:title", "New Title");
        session.saveDocument(root);
        ctx.setInput(root);
        ctx.put("doc", root);
        Map<String, Object> params = new HashMap<>();
        params.put("doc", root);
        Object result = automationService.run(ctx, "Scripting.TestWrappers", params);
        assertEquals(
                "Root input title:New Title" + System.lineSeparator() + "Root input title:New Title"
                        + System.lineSeparator() + "Root ctx title:New Title" + System.lineSeparator()
                        + "Root ctx title:New Title" + System.lineSeparator() + "Root params title:New Title"
                        + System.lineSeparator() + "Root params title:New Title" + System.lineSeparator()
                        + "Root result title:New Title" + System.lineSeparator() + "Root result title:New Title"
                        + System.lineSeparator() + "Root ctx title:New Title" + System.lineSeparator()
                        + "Root ctx title:New Title" + System.lineSeparator(), outContent.toString());
        assertTrue(result instanceof DocumentModel);
        Object doc = ctx.get("doc");
        assertNotNull(doc);
        assertTrue(doc instanceof DocumentModel);
        assertTrue((Boolean) ctx.get("entry"));
    }

    @Test
    public void canHandleJavaListMap() throws OperationException {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "List");
        List<String> attachments = new ArrayList<>();
        attachments.add("att1");
        attachments.add("att2");
        attachments.add("att3");
        doc.setPropertyValue("list:items", (Serializable) attachments);
        Map<String, String> values = new HashMap<>();
        values.put("name", "vlad");
        values.put("description", "desc");
        doc.setPropertyValue("list:complexItem", (Serializable) values);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.createDocument(doc));
        DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestList", null);
        assertEquals(
                "att1" + System.lineSeparator() + "att2" + System.lineSeparator() + "att3" + System.lineSeparator()
                        + "newValue" + System.lineSeparator() + "att2" + System.lineSeparator() + "att3"
                        + System.lineSeparator() + "vlad" + System.lineSeparator() + "desc" + System.lineSeparator(),
                outContent.toString());
        assertEquals("newValue", ((String[]) result.getPropertyValue("list:items"))[0]);
        assertEquals("vlad", ((Map<?, ?>) result.getPropertyValue("list:complexItem")).get("name"));
    }

    @Test
    public void canHandleLoginAsCtx() throws OperationException {
        try (CoreSession session = CoreInstance.openCoreSession(this.session.getRepositoryName(), "jdoe")) {
            OperationContext ctx = new OperationContext(session);
            automationService.run(ctx, "my-chain-with-loginasctx", null);
            assertEquals("Administrator" + System.lineSeparator(), outContent.toString());
        }
    }

    @Test
    public void canHandleLoginAsOp() throws OperationException {
        try (CoreSession session = CoreInstance.openCoreSession(this.session.getRepositoryName(), "jdoe")) {
            OperationContext ctx = new OperationContext(session);
            String principal = (String) automationService.run(ctx, "my-chain-with-loginasop", null);
            assertEquals("Administrator" + System.lineSeparator(), outContent.toString());
            assertEquals("Administrator", principal);
        }
    }

    @Test
    public void canUnwrapContextDocListing() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        DocumentModelList docs = new DocumentModelListImpl();
        docs.add(root);
        docs.add(root);
        ctx.put("docs", docs);
        Object result = automationService.run(ctx, "Scripting.SimpleScript", null);
        assertNotNull(result);
    }

    /*
     * NXP-19012
     */
    @Test
    public void canUnwrapContextWithTrace() throws OperationException {
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }

        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        DocumentModelList docs = new DocumentModelListImpl();
        docs.add(root);
        docs.add(root);
        ctx.put("docs", docs);
        ctx.setInput(root);
        Map<String, Object> params = new HashMap<>();
        Object result = automationService.run(ctx, "Scripting.ChainWithScripting", params);
        assertNotNull(result);
        // check if the context has been unwrapped correctly
        assertTrue(ctx.get("docs") instanceof DocumentModelList && ((DocumentModelList) ctx.get("docs")).size() == 2);
    }

    @Test
    public void testMVELScriptResolver() throws Exception {
        OperationContext ctx = new OperationContext(session);
        String mvelResult = (String) automationService.run(ctx, "my-chain-with-mvelresolver", null);
        assertEquals("Foo Bar", mvelResult);
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSet() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        root = (DocumentModel) automationService.run(ctx, "Scripting.TestSet", null);
        assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0L);
        assertEquals(cal, root.getProperty("dc:created").getValue());
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValue() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValue", null);
        assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0L);
        assertEquals(cal, root.getProperty("dc:created").getValue());
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetArray() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetArray", null);
        assertArrayEquals(new String[] { "sciences", "society" }, (Object[]) root.getProperty("dc:subjects").getValue());
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValueArray() throws Exception {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValueArray", null);
        assertArrayEquals(new String[] { "sciences", "society" }, (Object[]) root.getProperty("dc:subjects").getValue());
    }

    /*
     * NXP-19176
     */
    @Test
    public void handleBlobListAsInput() throws IOException, OperationException {
        // Init parameters
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        DocumentModel doc = session.createDocumentModel("/", "docWithBlobs", "File");
        doc = session.createDocument(doc);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        session.saveDocument(doc);

        OperationContext ctx = new OperationContext(session);
        BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestInputBlobList", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        // We added two blobs to context
        BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
        assertNotNull(blobs);
        assertEquals(2, blobs.size());
    }

    /*
     * NXP-19176
     */
    @Test
    public void handleBlobArrayAsInput() throws IOException, OperationException {
        // Init parameters
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        DocumentModel doc = session.createDocumentModel("/", "docWithBlobs", "File");
        doc = session.createDocument(doc);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        DocumentHelper.addBlob(doc.getProperty("files:files"), fb);
        session.saveDocument(doc);

        OperationContext ctx = new OperationContext(session);
        BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestInputBlobArray", null);
        assertNotNull(result);
        assertEquals(2, result.size());
        // We added two blobs to context
        BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
        assertNotNull(blobs);
        assertEquals(2, blobs.size());
    }

    @Test
    public void testArrayObjectParametersOperation() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel root = session.getRootDocument();
        ctx.setInput(root);
        root = (DocumentModel) automationService.run(ctx, "Scripting.TestArrayObjectProperties", null);
        assertArrayEquals(new String[] { "sciences", "society" }, (Object[]) root.getProperty("dc:subjects").getValue());
    }

}
