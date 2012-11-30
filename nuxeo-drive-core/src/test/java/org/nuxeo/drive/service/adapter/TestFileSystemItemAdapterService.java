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

import java.io.Serializable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryDescriptor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.drive.core")
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

    protected DocumentModel file;

    protected DocumentModel folder;

    protected DocumentModel custom;

    @Before
    public void createTestDocs() throws ClientException {

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
    }

    @Test
    public void testService() throws Exception {

        // ------------------------------------------------------
        // Check ordered factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> factoryDescs = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFactoryDescriptors();
        assertNotNull(factoryDescs);
        assertEquals(3, factoryDescs.size());

        FileSystemItemFactoryDescriptor desc = factoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(20, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFileItemFactory);

        desc = factoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(30, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFolderItemFactory);

        desc = factoryDescs.get("defaultFileSystemItemFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(50, desc.getOrder());
        assertEquals("defaultFileSystemItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        // ------------------------------------------------------
        // Check factories
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DummyFileItemFactory class
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFileItem);
        assertEquals("Dummy file with id " + file.getId(), fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Joe", fsItem.getCreator());

        // Folder => should use the dummyFacetFactory bound to the
        // DummyFolderItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFolderItem);
        assertEquals("Dummy folder with id " + folder.getId(), fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());

        // Custom => should use the defaultFileSystemItemFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(custom);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("Bonnie's file.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Bonnie", fsItem.getCreator());
        Blob fileFsItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.txt", fileFsItemBlob.getFilename());
        assertEquals("Content of the custom document's blob.",
                fileFsItemBlob.getString());
    }

    @Test
    public void testContribOverride() throws Exception {

        harness.deployContrib("org.nuxeo.drive.core.test",
                "OSGI-INF/test-nuxeodrive-adapter-service-contrib-override.xml");

        // ------------------------------------------------------
        // Check ordered factory descriptors
        // ------------------------------------------------------
        Map<String, FileSystemItemFactoryDescriptor> factoryDescs = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFactoryDescriptors();
        assertNotNull(factoryDescs);
        assertEquals(2, factoryDescs.size());

        FileSystemItemFactoryDescriptor desc = factoryDescs.get("dummyFacetFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(20, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        desc = factoryDescs.get("dummyDocTypeFactory");
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(30, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        // ------------------------------------------------------
        // Check factories
        // ------------------------------------------------------
        // File => should use the dummyDocTypeFactory bound to the
        // DefaultFileSystemItemFactory class, but return null because the
        // document has no file
        FileSystemItem fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(file);
        assertNull(fsItem);

        // Folder => should use the dummyFacetFactory bound to the
        // DefaultFileSystemItemFactory class
        fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());

        // Custom => should find no matching fileSystemItemFactory
        fsItem = fileSystemItemAdapterService.getFileSystemItemAdapter(custom);
        assertNull(fsItem);
    }
}
