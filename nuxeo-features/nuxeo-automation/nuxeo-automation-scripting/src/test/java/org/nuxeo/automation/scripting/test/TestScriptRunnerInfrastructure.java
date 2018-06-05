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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.MvelExpression;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationScriptingFeature.class)
public class TestScriptRunnerInfrastructure {

    protected static String[] attachments = { "att1", "att2", "att3" };

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Inject
    AutomationScriptingFeature feature;

    @Inject
    protected AutomationScriptingService service;

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private PrintStream outStream;

    @Inject
    TracerFactory factory;

    @Before
    public void setupContext() {
        outStream = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() throws IOException {
        outContent.close();
        System.setOut(outStream);
    }

    @Inject
    AutomationScriptingFeature scripting;

    @Test
    public void shouldExecuteSimpleScript() throws Exception {
        DocumentModelList docs = scripting.run("simpleAutomationScript.js", session, DocumentModelList.class);
        assertEquals(10, docs.size());
    }

    @Test
    public void simpleScriptingOperationShouldBeAvailable() throws Exception {

        OperationType type = automationService.getOperation("Scripting" + ".HelloWorld");
        assertNotNull(type);

        Param[] paramDefs = type.getDocumentation().getParams();
        assertEquals(1, paramDefs.length);

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("lang", "en");
            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.HelloWorld", params);
            assertEquals("Hello John", result.toString());
        }

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("lang", "fr");
            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.HelloWorld", params);
            assertEquals("Bonjour John", result.toString());
        }
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

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            params.put("facet", "HiddenInNavigation");
            params.put("type", "File");
            ctx.setInput(root);
            Object result = automationService.run(ctx, "Scripting.AddFacetInSubTree", params);
            DocumentModelList docs = (DocumentModelList) result;
            assertEquals(5, docs.size());
        }
    }

    @Test
    public void simpleScriptingOperationsInChain() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();

            ctx.setInput("John");
            Object result = automationService.run(ctx, "Scripting.ChainedHello", params);
            assertEquals("Hello Bonjour John", result.toString());
        }

    }

    @Test
    public void simpleCallToScriptingOperationsChain() throws Exception {
        String message = scripting.run("simpleCallToChain.js", session, String.class);
        assertEquals("Hello Bonjour John", message);

    }

    @Test
    public void testOperationCtx() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            ctx.put("test", "odd");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationCtx", params);
            assertEquals("odd", result.getPropertyValue("dc:nature"));
            assertEquals("modifiedValue", result.getPropertyValue("dc:description"));
            assertEquals("newEntry", result.getPropertyValue("dc:title"));
            assertEquals("Administrator", result.getPropertyValue("dc:creator"));
        }
    }

    @Test
    public void testOperationWithBlob() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(fb);
            Map<String, Object> params = new HashMap<>();
            params.put("document", "/newDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestBlob", params);
            final Blob blob = (Blob) result.getPropertyValue("file:content");
            assertEquals("New Title", result.getTitle());
            assertEquals("creationFields.json", blob.getFilename());
        }
    }

    @Test
    public void testWithSetBlobOperation() throws IOException, OperationException {
        // upload file blob
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("creationFields.json");
        Blob fb = Blobs.createBlob(fieldAsJsonFile);
        fb.setMimeType("image/jpeg");

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(fb);
            Map<String, Object> params = new HashMap<>();
            params.put("document", "/newDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestSetBlob", params);
            assertEquals("creationFields.json", ((Blob) result.getPropertyValue("file:content")).getFilename());
        }
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
        String fieldsDataAsJSon = org.apache.commons.io.FileUtils.readFileToString(fieldAsJsonFile, UTF_8);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");
        creationProps.put("ds:fields", fieldsDataAsJSon);
        creationProps.put("dc:title", "testDoc");

        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> params = new HashMap<>();
            params.put("properties", toString(creationProps));
            params.put("type", "DataSet");
            params.put("name", "testDoc");
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestComplexProperties",
                    params);
            assertEquals("whatever",
                    ((Map<?, ?>) ((List<?>) result.getPropertyValue("ds:fields")).get(0)).get("sqlTypeHint"));
        }
    }

    @Test
    public void testClassFilter() throws Exception {
        try {
            scripting.run("classFilterScript.js", session, Void.class);
        } catch (RuntimeException cause) {
            assertEquals(ClassNotFoundException.class, cause.getCause().getClass());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting.tests:OSGI-INF/classfilter-contrib.xml")
    public void testClassFilterAllowed() throws Exception {
        // injected fields in features aren't recomputed correctly (bug), so pass service explicitly
        feature.run(service, "classFilterScript.js", session, Void.class);
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting.tests:OSGI-INF/classfilter2-contrib.xml")
    public void testClassFilterDenied() throws Exception {
        // injected fields in features aren't recomputed correctly (bug), so pass service explicitly
        try {
            feature.run(service, "classFilterScript.js", session, Void.class);
        } catch (RuntimeException cause) {
            assertEquals(ClassNotFoundException.class, cause.getCause().getClass());
        }
    }

    @Test
    public void testFn() throws Exception {
        // Test platform functions injection
        String message = scripting.run("platformFunctions.js", session, String.class);
        assertEquals("devnull@nuxeo.com", message);
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
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModelList result = (DocumentModelList) automationService.run(ctx,
                    "Scripting.TestInputDocumentList");
            assertNotNull(result);
        }
    }

    @Test
    public void handleWorkflowVariables() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Map<String, Object> wfVars = new HashMap<>();
            Map<String, Object> nodeVars = new HashMap<>();
            wfVars.put("var", "workflow");
            nodeVars.put("var", "node");
            ctx.put(Constants.VAR_WORKFLOW, wfVars);
            ctx.put(Constants.VAR_WORKFLOW_NODE, nodeVars);
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestOperationWF");
            assertEquals("workflow", result.getPropertyValue("dc:title"));
            assertEquals("node", result.getPropertyValue("dc:description"));
        }
    }

    @Test
    public void canUseChainWithDashes() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestChainWithDashes");
            assertNotNull(result);
        }
    }

    @Test
    public void canManageDocumentModelWrappers() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            root.setPropertyValue("dc:title", "New Title");
            session.saveDocument(root);
            ctx.setInput(root);
            ctx.put("var", root);
            Map<String, Object> params = new HashMap<>();
            params.put("param", "root");
            Object result = automationService.run(ctx, "Scripting.TestWrappers", params);
            assertTrue(result instanceof DocumentModel);
            Object doc = ctx.get("var");
            assertNotNull(doc);
            assertTrue(doc instanceof DocumentModel);
            assertTrue((Boolean) ctx.get("entry"));
        }
    }

    @Test
    public void canHandleJavaListMap() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "doc", "List");
        List<String> attachments = new ArrayList<>();
        attachments.add("att1");
        attachments.add("att2");
        attachments.add("att3");
        doc.setPropertyValue("list:items", (Serializable) attachments);
        Map<String, String> values = new HashMap<>();
        values.put("name", "vlad");
        values.put("description", "desc");
        doc.setPropertyValue("list:complexItem", (Serializable) values);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(session.createDocument(doc));
            DocumentModel result = (DocumentModel) automationService.run(ctx, "Scripting.TestList");
            assertEquals("newValue", ((String[]) result.getPropertyValue("list:items"))[0]);
            assertEquals("vlad", ((Map<?, ?>) result.getPropertyValue("list:complexItem")).get("name"));
        }
    }

    @Test
    public void canHandleLoginAsCtx() throws OperationException {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(this.session.getRepositoryName(), "jdoe")) {
            try (OperationContext ctx = new OperationContext(session)) {
                String username = (String) automationService.run(ctx, "my-chain-with-loginasctx");
                assertEquals("Administrator", username);
            }
        }
    }

    @Test
    public void canHandleLoginAsOp() throws OperationException {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(this.session.getRepositoryName(), "jdoe")) {
            try (OperationContext ctx = new OperationContext(session)) {
                String username = (String) automationService.run(ctx, "my-chain-with-loginasop");
                assertEquals("Administrator", username);
            }
        }
    }

    @Test
    public void canUnwrapContextDocListing() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            DocumentModelList docs = new DocumentModelListImpl();
            docs.add(root);
            docs.add(root);
            ctx.put("docs", docs);
            Object result = automationService.run(ctx, "Scripting.SimpleScript");
            assertNotNull(result);
        }
    }

    /*
     * NXP-19012
     */
    @Test
    public void canUnwrapContextWithTrace() throws OperationException {
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }

        try (OperationContext ctx = new OperationContext(session)) {
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
            assertTrue(
                    ctx.get("docs") instanceof DocumentModelList && ((DocumentModelList) ctx.get("docs")).size() == 2);
        }
    }

    @Test
    public void testMVELScriptResolver() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            Object mvelResult = automationService.run(ctx, "my-chain-with-mvelresolver");
            assertEquals("Foo Bar", mvelResult);
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSet() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSet");
            assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0L);
            assertEquals(cal, root.getProperty("dc:created").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValue() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValue");
            assertEquals("TitleFromTest", root.getProperty("dc:title").getValue());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0L);
            assertEquals(cal, root.getProperty("dc:created").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetArray() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetArray");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
    }

    /*
     * NXP-19444
     */
    @Test
    public void testSetPropertyValueArray() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestSetPropertyValueArray");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
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

        try (OperationContext ctx = new OperationContext(session)) {
            BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestInputBlobList");
            assertNotNull(result);
            assertEquals(2, result.size());
            // We added two blobs to context
            BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
            assertNotNull(blobs);
            assertEquals(2, blobs.size());
        }
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

        try (OperationContext ctx = new OperationContext(session)) {
            BlobList result = (BlobList) automationService.run(ctx, "Scripting.TestInputBlobArray");
            assertNotNull(result);
            assertEquals(2, result.size());
            // We added two blobs to context
            BlobList blobs = (BlobList) ctx.pop(Constants.O_BLOBS);
            assertNotNull(blobs);
            assertEquals(2, blobs.size());
        }
    }

    @Test
    public void testArrayObjectParametersOperation() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            DocumentModel root = session.getRootDocument();
            ctx.setInput(root);
            root = (DocumentModel) automationService.run(ctx, "Scripting.TestArrayObjectProperties");
            assertArrayEquals(new String[] { "sciences", "society" },
                    (Object[]) root.getProperty("dc:subjects").getValue());
        }
    }

    @Test
    public void testNotInlinedContext() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.put("today", new MvelExpression("CurrentDate.date"));
            ctx.put("tomorrow", new MvelExpression("CurrentDate.days(1).date"));
            DataModelProperties props = (DataModelProperties) automationService.run(ctx, "Scripting.TestParams");
            Assertions.assertThat(props.getMap()).containsOnlyKeys("today");
        }
    }

    @Test
    public void testLoopWithDifferentParameters() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            Object result = automationService.run(ctx, "Scripting.TestLoopWithDifferentParameters");
            assertEquals("P1 P2 P3 P4 P5 P6 P7 P8 P9 P10 ", String.valueOf(result));
        }
    }

    @Test
    public void testDocumentPathInputAdaptedAsDocument() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput("/default-domain");
            DocumentModel doc = (DocumentModel) automationService.run(ctx, "Scripting.TestInputAdaptedAsDocument");
            assertTrue(doc.getPathAsString().equals("/default-domain"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentPathsInputAdaptedAsDocuments() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput("/default-domain,/default-domain/workspaces");
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestInputAdaptedAsDocuments");
            assertEquals(2, docs.size());
            assertTrue(docs.get(0).getPathAsString().equals("/default-domain"));
            assertTrue(docs.get(1).getPathAsString().equals("/default-domain/workspaces"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentsInputAdaptedAsDocuments() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(Arrays.asList(domain, workspaces));
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestInputAdaptedAsDocuments");
            assertEquals(2, docs.size());
            assertTrue(docs.get(0).getPathAsString().equals("/default-domain"));
            assertTrue(docs.get(1).getPathAsString().equals("/default-domain/workspaces"));
        }
    }

    @Test
    public void testUpdateProperties() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:source", "source");
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            DocumentModel res = (DocumentModel) automationService.run(ctx, "Scripting.TestUpdateProperties");
            assertEquals("foo", res.getPropertyValue("dc:description"));
            assertEquals("bar", res.getPropertyValue("dc:source"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateComplexProperties() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "docComplex", "List");
        Map<String, Serializable> map = new HashMap<>();
        map.put("name", "name");
        map.put("description", "description");
        doc.setPropertyValue("list:complexItem", (Serializable) map);
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            DocumentModel res = (DocumentModel) automationService.run(ctx, "Scripting.TestUpdateComplexProperties");
            Map<String, Serializable> complexItem = (Map<String, Serializable>) res.getPropertyValue("list:complexItem");
            assertEquals("foo", complexItem.get("name"));
            assertEquals("bar", complexItem.get("description"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPropertiesAccessOnDocuments() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(Arrays.asList(domain, workspaces));
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestPropertiesAccessOnDocuments");
            assertEquals(2, docs.size());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDocumentProperty() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel doc = session.createDocumentModel("/", "file", "FileWithDocumentFields");
        doc.setPropertyValue("df:documentId", computeDocumentFieldRef(domain.getId()));
        doc.setPropertyValue("df:documentPath", computeDocumentFieldRef(domain.getPathAsString()));
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestDocumentProperty");
            assertEquals(2, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain", docs.get(1).getPathAsString());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void tesDocumentListProperty() throws OperationException {
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        DocumentModel workspaces = session.getDocument(new PathRef("/default-domain/workspaces"));
        DocumentModel doc = session.createDocumentModel("/", "file", "FileWithDocumentFields");
        doc.setPropertyValue("df:documentIds", (Serializable) Arrays.asList(computeDocumentFieldRef(domain.getId()),
                computeDocumentFieldRef(workspaces.getId())));
        doc.setPropertyValue("df:documentPaths",
                (Serializable) Arrays.asList(computeDocumentFieldRef(workspaces.getPathAsString()),
                        computeDocumentFieldRef(domain.getPathAsString())));
        doc = session.createDocument(doc);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc);
            List<DocumentModel> docs = (List<DocumentModel>) automationService.run(ctx,
                    "Scripting.TestDocumentListProperty");
            assertEquals(4, docs.size());
            assertEquals("/default-domain", docs.get(0).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(1).getPathAsString());
            assertEquals("/default-domain/workspaces", docs.get(2).getPathAsString());
            assertEquals("/default-domain", docs.get(3).getPathAsString());
        }
    }

    protected String computeDocumentFieldRef(String value) {
        return session.getRepositoryName() + ":" + value;
    }

}
