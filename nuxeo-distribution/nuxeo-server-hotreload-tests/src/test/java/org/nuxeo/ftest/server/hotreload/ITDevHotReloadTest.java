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
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.functionaltests.AbstractTest.NUXEO_URL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.client.NuxeoClient.Builder;
import org.nuxeo.client.objects.Repository;
import org.nuxeo.client.spi.NuxeoClientRemoteException;
import org.nuxeo.ecm.core.test.StorageConfiguration;
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
    public void testHotReloadSequence() {
        String storageConf = StorageConfiguration.defaultSystemProperty(StorageConfiguration.CORE_PROPERTY,
                StorageConfiguration.DEFAULT_CORE);
        assumeTrue("This test only works with VCS", StorageConfiguration.CORE_VCS.equals(storageConf));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sequenceName", "hibernateSequencer");
        RestHelper.operation("javascript.getSequence", parameters);
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

    @Test
    public void testHotReloadPermission() {
        RestHelper.createUser("john", "doe");
        String docId = RestHelper.createDocument("/", "File", "file");
        // there's no existence check when adding a permission, so we will test to hot reload a new permission which
        // brings the Remove one, add it for john user on /file document and try to delete it
        // in order to be able to do that, john needs to have the RemoveChildren permission on the parent and as
        // CoreSession will resolve the parent, john also needs to have the Read permission on the parent
        RestHelper.addPermission("/", "john", "Read");
        RestHelper.addPermission("/", "john", "RemoveChildren");
        // it's better to keep all NuxeoClient usage in RestHelper, but adding a way to perform requests as another user
        // than Administrator may add complexity to RestHelper class
        // as it's the only usage currently, keep it as it for now
        Repository repository = new Builder().url(NUXEO_URL).authentication("john", "doe").connect().repository();
        try {
            repository.deleteDocument(docId);
            fail("User shouldn't be able to delete the document");
        } catch (NuxeoClientRemoteException nce) {
            assertEquals(403, nce.getStatus());
            assertEquals(String.format("Failed to delete document /file, Permission denied: cannot remove document %s, "
                    + "Missing permission 'Remove' on document %s", docId, docId), nce.getMessage());
        }
        // there's no check on adding a permission try to delete the file
        RestHelper.addPermission("/file", "john", "HotReloadRemove");
        repository.deleteDocument(docId);
        RestHelper.removeDocumentToDelete(docId);
    }

    @Test
    public void testHotReloadTwoBundles() {
        // this test just test we can hot reload two bundles
        // bundle-01 test
        testHotReloadPageProvider();
        // bundle-02 test
        testHotReloadAutomationScripting();
    }

    /**
     * Goal of this is test is to check that resources present in jar are correctly get after a hot reload.
     * <p/>
     * There are several caches around JarFile which leads to issues when hot reloading Nuxeo, for instance: when
     * replacing a jar and doing a hot reload, it's possible to get previous resource instead of new one present in jar.
     *
     * @since 10.10
     */
    @Test
    public void testHotReloadJarFileFactoryCleanup() {
        // deploy first bundle
        hotReloadRule.deployJarDevBundle(ITDevHotReloadTest.class,
                "_testHotReloadJarFileFactoryFlush/first/jar-to-hot-reload.jar");
        // assert workflow name
        assertEquals("New Workflow", RestHelper.getWorkflowInstanceTitle("newWorkflow"));
        // deploy second bundle with same jar name and resource change
        hotReloadRule.deployJarDevBundle(ITDevHotReloadTest.class,
                "_testHotReloadJarFileFactoryFlush/second/jar-to-hot-reload.jar");
        assertEquals("New Workflow (2)", RestHelper.getWorkflowInstanceTitle("newWorkflow"));
    }

    @Test
    public void testHotReloadFileImporters() throws IOException {
        // FileManagerService plugins extension point issue appearing when doing 2 hot reloads
        // see NXP-27147
        hotReloadRule.deployJarDevBundle(ITDevHotReloadTest.class, "testHotReloadFileImporters");

        Path tempPath = Files.createTempFile("", ".bin");
        File tempFile = tempPath.toFile();
        String docPath = "/default-domain/" + tempFile.getName();

        RestHelper.operation("FileManager.Import", tempFile, Map.of("currentDocument", "/default-domain"), null);
        RestHelper.addDocumentToDelete(docPath);

        String docType = RestHelper.fetchDocumentType(docPath);
        assertEquals("Foo", docType);
    }

}
