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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.functionaltests.RestHelper;

/**
 * Tests the dev hot reload.
 *
 * @since 9.3
 */
public class ITDevHotReloadTest {

    @Rule
    public final HotReloadTestRule hotReloadRule = new HotReloadTestRule();

    @Test
    public void testEmptyHotReload() {
        hotReloadRule.updateDevBundles("# EMPTY HOT RELOAD");
        // test create a document
        String id = RestHelper.createDocument("/", "File", "file", "description");
        assertNotNull(id);
    }

    @Test
    public void testHotReloadVocabulary() {
        // test fetch created entry
        Map<String, Object> properties = RestHelper.fetchDirectoryEntryProperties("hierarchical", "child2");
        assertNotNull(properties);
        assertEquals("root1", properties.get("parent"));
        assertEquals("child2", properties.get("label"));
    }

    @Test
    public void testHotReloadDocumentType() {
        // test create a document
        String id = RestHelper.createDocument("/", "HotReload", "hot reload",
                Collections.singletonMap("hr:content", "some content"));
        assertNotNull(id);
    }

    @Test
    public void testHotReloadLifecycle() {
        // test follow a transition
        String id = RestHelper.createDocument("/", "File", "file");
        RestHelper.followLifecycleTransition(id, "to_in_process");
        RestHelper.followLifecycleTransition(id, "to_archived");
        RestHelper.followLifecycleTransition(id, "to_draft");
    }

    @Test
    public void testHotReloadStructureTemplate() {
        // test Folder creation trigger a child of type File
        RestHelper.createDocument("/", "Folder", "folder");
        assertTrue(RestHelper.documentExists("/folder/File"));

        // undeploy the bundle
        hotReloadRule.updateDevBundles("# Remove previous bundle for test");
        // test the opposite
        RestHelper.createDocument("/folder", "Folder", "child");
        assertFalse(RestHelper.documentExists("/folder/child/File"));
    }

    @Test
    public void testHotReloadWorkflow() {
        // test start a workflow
        String id = RestHelper.createDocument("/", "File", "file");
        // our workflow only has one automatic transition to the final node
        RestHelper.startWorkflowInstance(id, "newWorkflow");
        assertFalse(RestHelper.documentHasWorkflowStarted(id));
    }

    @Test
    public void testHotReloadAutomationChain() {
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
        // test create a Folder to trigger automation event handler
        // this will create a File child
        RestHelper.createDocument("/", "Folder", "folder");
        assertTrue(RestHelper.documentExists("/folder/file"));
    }

    @Test
    public void testHotReloadUserAndGroup() {
        // test fetch userTest and groupTest
        assertTrue(RestHelper.userExists("userTest"));
        RestHelper.addUserToDelete("userTest");

        assertTrue(RestHelper.groupExists("groupTest"));
        RestHelper.addGroupToDelete("groupTest");
    }

    @Test
    public void testHotReloadPageProvider() {
        // test fetch result of page provider - SELECT * FROM File
        RestHelper.createDocument("/", "File", "file");
        int nbDocs = RestHelper.countQueryPageProvider("SIMPLE_NXQL_FOR_HOT_RELOAD_PAGE_PROVIDER");
        assertEquals(1, nbDocs);
    }

}
