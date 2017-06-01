/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.NuxeoDriveContribException;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.VirtualFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultSyncRootFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryDescriptor;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryWrapper;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService.Forcing;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Tests the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-adapter-service-contrib.xml")
public class TestFileSystemItemAdapterService {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected RuntimeHarness harness;

    protected String syncRootItemId;

    protected FolderItem syncRootItem;

    protected DocumentModel file;

    protected DocumentModel folder;

    protected DocumentModel custom;

    protected DocumentModel syncRootFolder;

    @Before
    public void registerRootAndCreateSomeDocs() throws Exception {

        syncRootFolder = session.createDocumentModel("/", "syncRoot", "Folder");
        syncRootFolder = session.createDocument(syncRootFolder);

        // Register the root folder as sync root
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        driveManager.registerSynchronizationRoot(session.getPrincipal(), syncRootFolder, session);

        syncRootItem = (FolderItem) fileSystemItemAdapterService.getFileSystemItem(syncRootFolder);
        syncRootItemId = syncRootItem.getId();

        file = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFile", "File");
        file.setPropertyValue("dc:creator", "Joe");
        file.setPropertyValue("dc:lastContributor", "Joe");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe's file.txt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file.putContextData(DocumentValidationService.CTX_MAP_KEY, Forcing.TURN_OFF);
        file.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);

        file = session.createDocument(file);

        folder = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder.setPropertyValue("dc:creator", "Jack");
        folder.setPropertyValue("dc:lastContributor", "Jack");
        folder.putContextData(DocumentValidationService.CTX_MAP_KEY, Forcing.TURN_OFF);
        folder.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        folder = session.createDocument(folder);

        custom = session.createDocumentModel(syncRootFolder.getPathAsString(), "aCustom", "Custom");
        custom.setPropertyValue("dc:creator", "Bonnie");
        custom.setPropertyValue("dc:lastContributor", "Bonnie");
        blob = new StringBlob("Content of the custom document's blob.");
        blob.setFilename("Bonnie's file.txt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom.putContextData(DocumentValidationService.CTX_MAP_KEY, Forcing.TURN_OFF);
        custom.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        custom = session.createDocument(custom);

        session.save();
    }

    @Test
    public void testService() throws Exception {

        // ------------------------------------------------------
        // Check file system item factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> fileSystemItemFactoryDescs =
                ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactoryDescriptors();
        assertNotNull(fileSystemItemFactoryDescs);
        assertEquals(12, fileSystemItemFactoryDescs.size());

        FileSystemItemFactoryDescriptor desc = fileSystemItemFactoryDescs.get("defaultSyncRootFolderItemFactory");
        assertNotNull(desc);
        assertEquals(10, desc.getOrder());
        assertEquals("defaultSyncRootFolderItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("DriveSynchronized", desc.getFacet());
        FileSystemItemFactory factory = desc.getFactory();
        assertTrue(factory instanceof DefaultSyncRootFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertEquals(20, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DummyFileItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertEquals(30, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DummyFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("defaultFileSystemItemFactory");
        assertNotNull(desc);
        assertEquals(50, desc.getOrder());
        assertEquals("defaultFileSystemItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DefaultFileSystemItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyVirtualFolderItemFactory");
        assertNotNull(desc);
        assertEquals(100, desc.getOrder());
        assertEquals("dummyVirtualFolderItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof VirtualFolderItemFactory);
        assertEquals("Dummy Folder", ((VirtualFolderItemFactory) factory).getFolderName());

        desc = fileSystemItemFactoryDescs.get("nullMergeTestFactory");
        assertNotNull(desc);
        assertEquals(200, desc.getOrder());
        assertEquals("nullMergeTestFactory", desc.getName());
        assertEquals("Note", desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DummyFileItemFactory);

        // ------------------------------------------------------
        // Check ordered file system item factories
        // ------------------------------------------------------
        List<FileSystemItemFactoryWrapper> fileSystemItemFactories =
                ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactories();
        assertNotNull(fileSystemItemFactories);
        assertEquals(7, fileSystemItemFactories.size());

        FileSystemItemFactoryWrapper factoryWrapper = fileSystemItemFactories.get(0);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("Collection", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("CollectionSyncRootFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(1);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("DriveSynchronized", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DefaultSyncRootFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(2);
        assertNotNull(factoryWrapper);
        assertEquals("File", factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DummyFileItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(3);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("Folderish", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DummyFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(4);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DefaultFileSystemItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(5);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DummyVirtualFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(6);
        assertNotNull(factoryWrapper);
        assertEquals("Note", factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DummyFileItemFactory"));

        // ------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc)
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DummyFileItemFactory class
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItem(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFileItem);
        assertEquals("dummyDocTypeFactory#test#" + file.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Dummy file with id " + file.getId(), fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Joe", fsItem.getCreator());
        assertEquals("Joe", fsItem.getLastContributor());

        // Folder => should use the dummyFacetFactory bound to the
        // DummyFolderItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals("dummyFacetFactory#test#" + folder.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Dummy folder with id " + folder.getId(), fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());
        assertEquals("Jack", fsItem.getLastContributor());

        // Custom => should use the defaultFileSystemItemFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(custom);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("defaultFileSystemItemFactory#test#" + custom.getId(), fsItem.getId());
        assertEquals(
                "/org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#/" + syncRootItemId + "/"
                        + fsItem.getId(),
                fsItem.getPath());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Bonnie's file.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Bonnie", fsItem.getCreator());
        assertEquals("Bonnie", fsItem.getLastContributor());
        Blob fileFsItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.txt", fileFsItemBlob.getFilename());
        assertEquals("Content of the custom document's blob.", fileFsItemBlob.getString());

        // -------------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, boolean includeDeleted,
        // boolean relaxSyncRootConstraint)
        // -------------------------------------------------------------------
        fsItem = fileSystemItemAdapterService.getFileSystemItem(custom, false, true);
        assertNotNull(fsItem);
        assertEquals("test#" + custom.getId(), fsItem.getId());

        // ------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, FolderItem parentItem)
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DummyFileItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(file, syncRootItem);
        assertNotNull(fsItem);
        assertEquals(syncRootItemId, fsItem.getParentId());

        // -------------------------------------------------------------
        // Check #getFileSystemItemFactoryForId(String id)
        // -------------------------------------------------------------
        // Default factory
        String fsItemId = "defaultFileSystemItemFactory#test#someId";
        FileSystemItemFactory fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("defaultFileSystemItemFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith("DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Top level folder item factory
        fsItemId = "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#";
        fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertTrue(fsItemFactory.getName().endsWith("DefaultTopLevelFolderItemFactory"));
        assertTrue(fsItemFactory.getClass().getName().endsWith("DefaultTopLevelFolderItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Factory with #canHandleFileSystemItemId returning false
        fsItemId = "dummyDocTypeFactory#test#someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found FileSystemItem id.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id dummyDocTypeFactory#test#someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Non parsable id
        fsItemId = "nonParsableId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id nonParsableId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Non existent factory name
        fsItemId = "nonExistentFactoryName#test#someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id nonExistentFactoryName#test#someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // -------------------------------------------------------------
        // Check #getTopLevelFolderItemFactory()
        // -------------------------------------------------------------
        TopLevelFolderItemFactory topLevelFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertNotNull(topLevelFactory);
        assertTrue(topLevelFactory.getClass().getName().endsWith("DefaultTopLevelFolderItemFactory"));
        assertTrue(topLevelFactory instanceof DefaultTopLevelFolderItemFactory);

        // -------------------------------------------------------------
        // Check #getVirtualFolderItemFactory(String factoryName)
        // -------------------------------------------------------------
        try {
            fileSystemItemAdapterService.getVirtualFolderItemFactory("nonExistentFactory");
            fail("No VirtualFolderItemFactory should be found for factory name.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "No factory named nonExistentFactory. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\">.",
                    e.getMessage());
        }
        try {
            fileSystemItemAdapterService.getVirtualFolderItemFactory("defaultFileSystemItemFactory");
            fail("No VirtualFolderItemFactory should be found for factory name.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "Factory class org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory for factory defaultFileSystemItemFactory is not a VirtualFolderItemFactory.",
                    e.getMessage());
        }
        VirtualFolderItemFactory virtualFolderItemFactory = fileSystemItemAdapterService.getVirtualFolderItemFactory("dummyVirtualFolderItemFactory");
        assertNotNull(virtualFolderItemFactory);
        assertTrue(virtualFolderItemFactory.getClass().getName().endsWith("DummyVirtualFolderItemFactory"));

        // -------------------------------------------------------------
        // Check #getActiveFileSystemItemFactories()
        // -------------------------------------------------------------
        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(7, activeFactories.size());
        assertTrue(activeFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("defaultSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("defaultFileSystemItemFactory"));
        assertTrue(activeFactories.contains("dummyDocTypeFactory"));
        assertTrue(activeFactories.contains("dummyFacetFactory"));
        assertTrue(activeFactories.contains("dummyVirtualFolderItemFactory"));
        assertTrue(activeFactories.contains("nullMergeTestFactory"));
    }

    @Test
    public void testContribOverride() throws Exception {
        assumeFalse("Cannot test reload for in-memory repository", coreFeature.getStorageConfiguration().isDBSMem());

        Framework.getRuntime().standby(Instant.now());
        try {
            harness.deployContrib("org.nuxeo.drive.core.test",
                    "OSGI-INF/test-nuxeodrive-adapter-service-contrib-override.xml");
        } finally {
            Framework.getRuntime().resume();
        }

        registerRootAndCreateSomeDocs();

        // Re-adapt the sync root to take the override into account
        syncRootItem = (FolderItem) fileSystemItemAdapterService.getFileSystemItem(syncRootFolder);
        syncRootItemId = syncRootItem.getId();

        // ------------------------------------------------------
        // Check file system item factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> fileSystemItemFactoryDescs =
                ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactoryDescriptors();
        assertNotNull(fileSystemItemFactoryDescs);
        assertEquals(12, fileSystemItemFactoryDescs.size());

        FileSystemItemFactoryDescriptor desc = fileSystemItemFactoryDescs.get("defaultSyncRootFolderItemFactory");
        assertNotNull(desc);
        assertEquals(10, desc.getOrder());
        assertEquals("defaultSyncRootFolderItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("DriveSynchronized", desc.getFacet());
        FileSystemItemFactory factory = desc.getFactory();
        assertTrue(factory instanceof DefaultSyncRootFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("defaultFileSystemItemFactory");
        assertNotNull(desc);
        assertEquals(50, desc.getOrder());
        assertEquals("defaultFileSystemItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DefaultFileSystemItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertEquals(20, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DefaultFileSystemItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertEquals(30, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DefaultFileSystemItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyVirtualFolderItemFactory");
        assertNotNull(desc);

        desc = fileSystemItemFactoryDescs.get("nullMergeTestFactory");
        assertNotNull(desc);
        assertEquals(200, desc.getOrder());
        assertEquals("nullMergeTestFactory", desc.getName());
        assertEquals("Note", desc.getDocType());
        assertNull(desc.getFacet());
        factory = desc.getFactory();
        assertTrue(factory instanceof DummyFileItemFactory);

        // ------------------------------------------------------
        // Check ordered file system item factories
        // ------------------------------------------------------
        List<FileSystemItemFactoryWrapper> fileSystemItemFactories =
                ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactories();
        assertNotNull(fileSystemItemFactories);
        assertEquals(6, fileSystemItemFactories.size());

        FileSystemItemFactoryWrapper factoryWrapper = fileSystemItemFactories.get(0);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("Collection", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("CollectionSyncRootFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(1);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("DriveSynchronized", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DefaultSyncRootFolderItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(2);
        assertNotNull(factoryWrapper);
        assertNull(factoryWrapper.getDocType());
        assertEquals("Folderish", factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DefaultFileSystemItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(3);
        assertNotNull(factoryWrapper);
        assertEquals("File", factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DefaultFileSystemItemFactory"));

        factoryWrapper = fileSystemItemFactories.get(4);
        assertNotNull(factoryWrapper);

        factoryWrapper = fileSystemItemFactories.get(5);
        assertNotNull(factoryWrapper);
        assertEquals("Note", factoryWrapper.getDocType());
        assertNull(factoryWrapper.getFacet());
        assertTrue(factoryWrapper.getFactory().getClass().getName().endsWith("DummyFileItemFactory"));

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc)
        // -------------------------------------------------------------
        // File => should try the dummyDocTypeFactory bound to the
        // DefaultFileSystemItemFactory class, returning null because the
        // document has no file, then try the dummyVirtualFolderItemFactory
        // bound to the DummyVirtualFolderItemFactory, returning null because
        // virtual
        file.setPropertyValue("file:content", null);
        session.saveDocument(file);
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItem(file);
        assertNull(fsItem);

        // Folder => should use the dummyFacetFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertTrue(((FolderItem) fsItem).getCanCreateChild());
        assertEquals("dummyFacetFactory#test#" + folder.getId(), fsItem.getId());
        assertEquals(syncRootItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());
        assertEquals("Jack", fsItem.getLastContributor());

        // Custom => should try the dummyVirtualFolderItemFactory
        // bound to the DummyVirtualFolderItemFactory, returning null because
        // virtual
        fsItem = fileSystemItemAdapterService.getFileSystemItem(custom);
        assertNull(fsItem);

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, String parentId)
        // -------------------------------------------------------------
        // Folder => should use the dummyFacetFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder, syncRootItem);
        assertNotNull(fsItem);
        assertEquals(syncRootItemId, fsItem.getParentId());

        // -------------------------------------------------------------
        // Check #getFileSystemItemFactoryForId(String id)
        // -------------------------------------------------------------
        // Disabled default factory
        String fsItemId = "defaultFileSystemItemFactory#test#someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (NuxeoDriveContribException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id defaultFileSystemItemFactory#test#someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Factory with #canHandleFileSystemItemId returning true
        fsItemId = "dummyDocTypeFactory#test#someId";
        FileSystemItemFactory fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("dummyDocTypeFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith("DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Other test factory with #canHandleFileSystemItemId returning true
        fsItemId = "dummyFacetFactory#test#someId";
        fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("dummyFacetFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith("DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Top level folder item factory
        fsItemId = "org.nuxeo.drive.service.adapter.DummyTopLevelFolderItemFactory#";
        fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertTrue(fsItemFactory.getName().endsWith("DummyTopLevelFolderItemFactory"));
        assertTrue(fsItemFactory.getClass().getName().endsWith("DummyTopLevelFolderItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // -------------------------------------------------------------
        // Check #getTopLevelFolderItemFactory()
        // -------------------------------------------------------------
        TopLevelFolderItemFactory topLevelFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertNotNull(topLevelFactory);
        assertTrue(topLevelFactory.getClass().getName().endsWith("DummyTopLevelFolderItemFactory"));
        assertTrue(topLevelFactory instanceof DummyTopLevelFolderItemFactory);

        // -------------------------------------------------------------
        // Check #getActiveFileSystemItemFactories()
        // -------------------------------------------------------------
        Set<String> activeFactories = fileSystemItemAdapterService.getActiveFileSystemItemFactories();
        assertEquals(6, activeFactories.size());
        assertTrue(activeFactories.contains("collectionSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("defaultSyncRootFolderItemFactory"));
        assertTrue(activeFactories.contains("dummyDocTypeFactory"));
        assertTrue(activeFactories.contains("dummyFacetFactory"));
        assertTrue(activeFactories.contains("dummyVirtualFolderItemFactory"));
        assertTrue(activeFactories.contains("nullMergeTestFactory"));

        Framework.getRuntime().standby(Instant.now());
        try {
            harness.undeployContrib("org.nuxeo.drive.core.test",
                    "OSGI-INF/test-nuxeodrive-adapter-service-contrib-override.xml");
        } finally {
            Framework.getRuntime().resume();
        }
    }

    void reload() throws InterruptedException {
        Properties lastProps = Framework.getProperties();
        try {
            Framework.getLocalService(ReloadService.class).reload();
        } finally {
            Framework.getProperties().putAll(lastProps);
        }
    }
}
