/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ftest.server.hotreload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.functionaltests.RestHelper;

/**
 * Tests the dev hot reload.
 *
 * @since 9.3
 */
public class ITDevHotReloadTest {

    public static final String NUXEO_RELOAD_PATH = "/sdk/reload";

    protected final static Function<URL, URI> URI_MAPPER = url -> {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new NuxeoException("Unable to map the url to uri", e);
        }
    };

    @Rule
    public final TestName testName = new TestName();

    @Before
    public void before() {
        RestHelper.logOnServer(String.format("Starting test 'ITDevHotReloadTest#%s'", testName.getMethodName()));
    }

    @After
    public void after() {
        RestHelper.logOnServer(String.format("Ending test 'ITDevHotReloadTest#%s'", testName.getMethodName()));
        RestHelper.cleanup();
        // reset dev.bundles file
        postToDevBundles("# AFTER TEST: " + testName.getMethodName());
    }

    @Test
    public void testEmptyHotReload() {
        postToDevBundles("# EMPTY HOT RELOAD");
        // test create a document
        String id = RestHelper.createDocument("/", "File", "file", "description");
        assertNotNull(id);
    }

    @Test
    public void testHotReloadVocabulary() {
        deployDevBundle();
        // test fetch created entry
        Map<String, Object> properties = RestHelper.fetchDirectoryEntryProperties("hierarchical", "child2");
        assertNotNull(properties);
        assertEquals("root1", properties.get("parent"));
        assertEquals("child2", properties.get("label"));
    }

    @Test
    public void testHotReloadDocumentType() {
        deployDevBundle();
        // test create a document
        String id = RestHelper.createDocument("/", "HotReload", "hot reload",
                Collections.singletonMap("hr:content", "some content"));
        assertNotNull(id);
    }

    @Test
    public void testHotReloadLifecycle() {
        deployDevBundle();
        // test follow a transition
        String id = RestHelper.createDocument("/", "File", "file");
        RestHelper.followLifecycleTransition(id, "to_in_process");
        RestHelper.followLifecycleTransition(id, "to_archived");
        RestHelper.followLifecycleTransition(id, "to_draft");
    }

    @Test
    public void testHotReloadStructureTemplate() {
        deployDevBundle();
        // test Folder creation trigger a child of type File
        RestHelper.createDocument("/", "Folder", "folder");
        assertTrue(RestHelper.documentExists("/folder/File"));

        // undeploy the bundle
        postToDevBundles("# Remove previous bundle for test");
        // test the opposite
        RestHelper.createDocument("/folder", "Folder", "child");
        assertFalse(RestHelper.documentExists("/folder/child/File"));
    }

    @Test
    public void testHotReloadWorkflow() {
        deployDevBundle();
        // test start a workflow
        String id = RestHelper.createDocument("/", "File", "file");
        // our workflow only has one automatic transition to the final node
        RestHelper.startWorkflowInstance(id, "newWorkflow");
        assertFalse(RestHelper.documentHasWorkflowStarted(id));
    }

    @Test
    public void testHotReloadAutomationChain() {
        deployDevBundle();
        // test call our automation chain
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parentPath", "/");
        parameters.put("docName", "file");
        RestHelper.operation("CreateDocumentAndStartWorkflow", parameters);
        // add the created file to RestHelper context
        RestHelper.addDocumentToDelete("/file");
        // our document should have a started workflow instance
        assertTrue(RestHelper.documentHasWorkflowStarted("/file"));
    }

    @Test
    public void testHotReloadAutomationScripting() {
        deployDevBundle();
        // test call our automation chain
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parentPath", "/");
        int nbChildren = 5;
        parameters.put("nbChildren", Integer.valueOf(nbChildren));
        RestHelper.operation("javascript.CreateSeveralChild", parameters);
        for (int i = 0; i < nbChildren; i++) {
            String childPath = "/file" + i;
            assertTrue(String.format("Document '%s' doesn't exist", childPath), RestHelper.documentExists(childPath));
            // add the created file to RestHelper context
            RestHelper.addDocumentToDelete(childPath);
        }
    }

    @Test
    public void testHotReloadAutomationEventHandler() {
        deployDevBundle();
        // test create a Folder to trigger automation event handler
        // this will create a File child
        RestHelper.createDocument("/", "Folder", "folder");
        assertTrue(RestHelper.documentExists("/folder/file"));
    }

    @Test
    public void testHotReloadUserAndGroup() {
        deployDevBundle();
        // test fetch userTest and groupTest
        assertTrue(RestHelper.userExists("userTest"));
        RestHelper.addUserToDelete("userTest");

        assertTrue(RestHelper.groupExists("groupTest"));
        RestHelper.addGroupToDelete("groupTest");
    }

    /**
     * Deploys the dev bundle located under src/test/resources/ITDevHotReloadTest/${testName}.
     */
    protected void deployDevBundle() {
        // first lookup the absolute paths
        String relativeTestPath = "/ITDevHotReloadTest/" + testName.getMethodName();
        URL url = getClass().getResource(relativeTestPath);
        URI uri = URI_MAPPER.apply(url);
        String absolutePath = Paths.get(uri).toAbsolutePath().toString();
        postToDevBundles("Bundle:" + absolutePath);
    }

    protected void postToDevBundles(String line) {
        // post new dev bundles to deploy
        if (!RestHelper.post(NUXEO_RELOAD_PATH, line)) {
            fail("Unable to reload dev bundles, for line=" + line);
        }
    }

}
