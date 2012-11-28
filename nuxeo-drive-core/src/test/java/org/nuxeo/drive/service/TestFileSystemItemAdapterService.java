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
package org.nuxeo.drive.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.service.impl.FileSystemItemFactoryDescriptor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Tests the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.drive.core")
@LocalDeploy({ "org.nuxeo.drive.core:test-nuxeodrive-types-contrib.xml",
        "org.nuxeo.drive.core:test-nuxeodrive-adapter-service-contrib.xml" })
public class TestFileSystemItemAdapterService {

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService nuxeoDriveItemAdapterService;

    protected DocumentModel file;

    protected DocumentModel folder;

    protected DocumentModel custom;

    @Before
    public void createTestDocs() throws ClientException {

        file = session.createDocumentModel("/", "aFile", "File");
        file.setPropertyValue("dc:creator", "Joe");
        file = session.createDocument(file);

        folder = session.createDocumentModel("/", "aFolder", "Folder");
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
        // Check factory descriptors
        // ------------------------------------------------------
        Set<FileSystemItemFactoryDescriptor> factoryDescs = ((FileSystemItemAdapterServiceImpl) nuxeoDriveItemAdapterService).getFactoryDescriptors();
        assertNotNull(factoryDescs);

        Iterator<FileSystemItemFactoryDescriptor> factoryDescsIt = factoryDescs.iterator();

        FileSystemItemFactoryDescriptor desc = factoryDescsIt.next();
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(20, desc.getOrder());
        assertEquals("dummyDocTypeFactory", desc.getName());
        assertEquals("File", desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFileItemFactory);

        desc = factoryDescsIt.next();
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(30, desc.getOrder());
        assertEquals("dummyFacetFactory", desc.getName());
        assertNull(desc.getDocType());
        assertEquals("Folderish", desc.getFacet());
        assertTrue(desc.getFactory() instanceof DummyFolderItemFactory);

        desc = factoryDescsIt.next();
        assertNotNull(desc);
        assertTrue(desc.isEnabled());
        assertEquals(50, desc.getOrder());
        assertEquals("defaultFileSystemItemFactory", desc.getName());
        assertNull(desc.getDocType());
        assertNull(desc.getFacet());
        assertTrue(desc.getFactory() instanceof DefaultFileSystemItemFactory);

        assertFalse(factoryDescsIt.hasNext());

        // ------------------------------------------------------
        // Check factories
        // ------------------------------------------------------
        // File => should use the "dummyDocTypeFactory"
        FileSystemItem fsItem = nuxeoDriveItemAdapterService.getFileSystemItemAdapter(file);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFileItem);
        assertEquals("Dummy file with id " + file.getId(), fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Joe", fsItem.getCreator());

        // Folder => should use the "dummyFacetFactory"
        fsItem = nuxeoDriveItemAdapterService.getFileSystemItemAdapter(folder);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof DummyFolderItem);
        assertEquals("Dummy folder with id " + folder.getId(), fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());

        // Custom => should use the "defaultFileSystemItemFactory"
        fsItem = nuxeoDriveItemAdapterService.getFileSystemItemAdapter(custom);
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
}
