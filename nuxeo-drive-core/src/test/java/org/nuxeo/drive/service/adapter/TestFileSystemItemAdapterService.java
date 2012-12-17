/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.DefaultSyncRootFolderItemFactory;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryDescriptor;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryWrapper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.runtime.reload" })
@LocalDeploy({
        "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml",
        "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-adapter-service-contrib.xml" })
public class TestFileSystemItemAdapterService {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected RuntimeHarness harness;

    protected String rootDocFileSystemItemId;

    protected DocumentModel file;

    protected DocumentModel folder;

    protected DocumentModel custom;

    @Before
    public void createTestDocs() throws ClientException {

        // TODO
        rootDocFileSystemItemId = "defaultSyncRootFolderItemFactory/test/"
                + session.getRootDocument().getId();

        file = session.createDocumentModel("/", "aFile", "File");
        file.setPropertyValue("dc:creator", "Joe");
        file = session.createDocument(file);

        folder = session.createDocumentModel("/", "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder.setPropertyValue("dc:creator", "Jack");
        folder = session.createDocument(folder);

        custom = session.createDocumentModel("/", "aCustom", "Custom");
        custom.setPropertyValue("dc:creator", "Bonnie");
        Blob blob = new StringBlob("Content of the custom document's blob.");
        blob.setFilename("Bonnie's file.txt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        session.save();
    }

    @Test
    public void testService() throws Exception {

        // ------------------------------------------------------
        // Check file system item factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> fileSystemItemFactoryDescs = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactoryDescriptors();
        assertNotNull(fileSystemItemFactoryDescs);
        assertEquals(4, fileSystemItemFactoryDescs.size());

        FileSystemItemFactoryDescriptor desc = fileSystemItemFactoryDescs.get("defaultSyncRootFolderItemFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(10, desc.getOrder());
        assertEquals("defaultSyncRootFolderItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("DriveSynchronized", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultSyncRootFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(20, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFileItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(30, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("defaultFileSystemItemFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(50, desc.getOrder());
        assertEquals("defaultFileSystemItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        // ------------------------------------------------------
        // Check ordered file system item factories
        // ------------------------------------------------------
        List<FileSystemItemFactoryWrapper> fileSystemItemFactories = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactories();
        assertNotNull(fileSystemItemFactories);
        assertEquals(4, fileSystemItemFactories.size());

        FileSystemItemFactoryWrapper factory = fileSystemItemFactories.get(0);
        assertNotNull(factory);
        assertNull(factory.getDocType());
        assertEquals("DriveSynchronized", factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DefaultSyncRootFolderItemFactory"));

        factory = fileSystemItemFactories.get(1);
        assertNotNull(factory);
        assertEquals("File", factory.getDocType());
        assertNull(factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DummyFileItemFactory"));

        factory = fileSystemItemFactories.get(2);
        assertNotNull(factory);
        assertNull(factory.getDocType());
        assertEquals("Folderish", factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DummyFolderItemFactory"));

        factory = fileSystemItemFactories.get(3);
        assertNotNull(factory);
        assertNull(factory.getDocType());
        assertNull(factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));

        // ------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc)
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DummyFileItemFactory class
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItem(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFileItem);
        assertEquals("dummyDocTypeFactory/test/" + file.getId(), fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Dummy file with id " + file.getId(), fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Joe", fsItem.getCreator());

        // Folder => should use the dummyFacetFactory bound to the
        // DummyFolderItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFolderItem);
        assertEquals("dummyFacetFactory/test/" + folder.getId(), fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Dummy folder with id " + folder.getId(), fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());

        // Custom => should use the defaultFileSystemItemFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(custom);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("defaultFileSystemItemFactory/test/" + custom.getId(),
                fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Bonnie's file.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Bonnie", fsItem.getCreator());
        Blob fileFsItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.txt", fileFsItemBlob.getFilename());
        assertEquals("Content of the custom document's blob.",
                fileFsItemBlob.getString());

        // ------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, String parentId)
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DummyFileItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(file,
                "fileParentId");
        assertNotNull(fsItem);
        assertEquals("fileParentId", fsItem.getParentId());

        // -------------------------------------------------------------
        // Check #getFileSystemItemFactoryForId(String id)
        // -------------------------------------------------------------
        // Default factory
        String fsItemId = "defaultFileSystemItemFactory/test/someId";
        FileSystemItemFactory fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("defaultFileSystemItemFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Factory with #canHandleFileSystemItemId returning false
        fsItemId = "dummyDocTypeFactory/test/someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found FileSystemItem id.");
        } catch (ClientException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id dummyDocTypeFactory/test/someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Non parsable id
        fsItemId = "nonParsableId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (ClientException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id nonParsableId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Non existent factory name
        fsItemId = "nonExistentFactoryName/test/someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (ClientException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id nonExistentFactoryName/test/someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // -------------------------------------------------------------
        // Check #getTopLevelFolderItemFactory()
        // -------------------------------------------------------------
        TopLevelFolderItemFactory topLevelFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertNotNull(topLevelFactory);
        assertTrue(topLevelFactory.getClass().getName().endsWith(
                "DefaultTopLevelFolderItemFactory"));
        assertTrue(topLevelFactory instanceof DefaultTopLevelFolderItemFactory);
    }

    @Test
    public void testContribOverride() throws Exception {

        harness.deployContrib("org.nuxeo.drive.core.test",
                "OSGI-INF/test-nuxeodrive-adapter-service-contrib-override.xml");
        Framework.getLocalService(ReloadService.class).reload();

        // ------------------------------------------------------
        // Check file system item factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> fileSystemItemFactoryDescs = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactoryDescriptors();
        assertNotNull(fileSystemItemFactoryDescs);
        assertEquals(3, fileSystemItemFactoryDescs.size());

        FileSystemItemFactoryDescriptor desc = fileSystemItemFactoryDescs.get("defaultSyncRootFolderItemFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(10, desc.getOrder());
        assertEquals("defaultSyncRootFolderItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("DriveSynchronized", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultSyncRootFolderItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(20, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        desc = fileSystemItemFactoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(30, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        // ------------------------------------------------------
        // Check ordered file system item factories
        // ------------------------------------------------------
        List<FileSystemItemFactoryWrapper> fileSystemItemFactories = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactories();
        assertNotNull(fileSystemItemFactories);
        assertEquals(3, fileSystemItemFactories.size());

        FileSystemItemFactoryWrapper factory = fileSystemItemFactories.get(0);
        assertNotNull(factory);
        assertNull(factory.getDocType());
        assertEquals("DriveSynchronized", factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DefaultSyncRootFolderItemFactory"));

        factory = fileSystemItemFactories.get(1);
        assertNotNull(factory);
        assertNull(factory.getDocType());
        assertEquals("Folderish", factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));

        factory = fileSystemItemFactories.get(2);
        assertNotNull(factory);
        assertEquals("File", factory.getDocType());
        assertNull(factory.getFacet());
        assertTrue(factory.getFactory().getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc)
        // -------------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DefaultFileSystemItemFactory class, but return null because the
        // document has no file
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItem(file);
        assertNull(fsItem);

        // Folder => should use the dummyFacetFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("dummyFacetFactory/test/" + folder.getId(), fsItem.getId());
        assertEquals(rootDocFileSystemItemId, fsItem.getParentId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());

        // Custom => should find no matching fileSystemItemFactory
        fsItem = fileSystemItemAdapterService.getFileSystemItem(custom);
        assertNull(fsItem);

        // -------------------------------------------------------------
        // Check #getFileSystemItem(DocumentModel doc, String parentId)
        // -------------------------------------------------------------
        // Folder => should use the dummyFacetFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItem(folder,
                "folderParentId");
        assertNotNull(fsItem);
        assertEquals("folderParentId", fsItem.getParentId());

        // -------------------------------------------------------------
        // Check #getFileSystemItemFactoryForId(String id)
        // -------------------------------------------------------------
        // Disabled default factory
        String fsItemId = "defaultFileSystemItemFactory/test/someId";
        try {
            fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
            fail("No fileSystemItemFactory should be found for FileSystemItem id.");
        } catch (ClientException e) {
            assertEquals(
                    "No fileSystemItemFactory found for FileSystemItem with id defaultFileSystemItemFactory/test/someId. Please check the contributions to the following extension point: <extension target=\"org.nuxeo.drive.service.FileSystemItemAdapterService\" point=\"fileSystemItemFactory\"> and make sure there is at least one defining a FileSystemItemFactory class for which the #canHandleFileSystemItemId(String id) method returns true.",
                    e.getMessage());
        }

        // Factory with #canHandleFileSystemItemId returning true
        fsItemId = "dummyDocTypeFactory/test/someId";
        FileSystemItemFactory fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("dummyDocTypeFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // Other test factory with #canHandleFileSystemItemId returning true
        fsItemId = "dummyFacetFactory/test/someId";
        fsItemFactory = fileSystemItemAdapterService.getFileSystemItemFactoryForId(fsItemId);
        assertNotNull(fsItemFactory);
        assertEquals("dummyFacetFactory", fsItemFactory.getName());
        assertTrue(fsItemFactory.getClass().getName().endsWith(
                "DefaultFileSystemItemFactory"));
        assertTrue(fsItemFactory.canHandleFileSystemItemId(fsItemId));

        // -------------------------------------------------------------
        // Check #getTopLevelFolderItemFactory()
        // -------------------------------------------------------------
        TopLevelFolderItemFactory topLevelFactory = fileSystemItemAdapterService.getTopLevelFolderItemFactory();
        assertNotNull(topLevelFactory);
        assertTrue(topLevelFactory.getClass().getName().endsWith(
                "DummyTopLevelFolderItemFactory"));
        assertTrue(topLevelFactory instanceof DummyTopLevelFolderItemFactory);
    }
}
