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
package org.nuxeo.drive.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.impl.FileSystemItemAdapterFactory;
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
 * Tests the {@link FileSystemItemAdapterFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.drive.core")
@LocalDeploy("org.nuxeo.drive.core:test-nuxeodrive-types-contrib.xml")
public class TestNuxeoDriveItemAdapterFactory {

    @Inject
    protected CoreSession session;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel notADriveItem;

    @Before
    public void createTestDocs() throws ClientException {

        // File
        file = session.createDocumentModel("/", "aFile", "File");
        file.setPropertyValue("dc:creator", "Joe");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        // Note
        note = session.createDocumentModel("/", "aNote", "Note");
        note.setPropertyValue("dc:creator", "Bob");
        note.setPropertyValue("note:note", "Content of Bob's note.");
        note = session.createDocument(note);

        // Custom doc type with the "file" schema
        custom = session.createDocumentModel("/", "aCustomDoc", "Custom");
        custom.setPropertyValue("dc:creator", "Bonnie");
        blob = new StringBlob("Content of Bonnie's file.");
        blob.setFilename("Bonnie's file.odt");
        custom.setPropertyValue("file:content", (Serializable) blob);
        custom = session.createDocument(custom);

        // Folder
        folder = session.createDocumentModel("/", "aFolder", "Folder");
        folder.setPropertyValue("dc:title", "Jack's folder");
        folder.setPropertyValue("dc:creator", "Jack");
        folder = session.createDocument(folder);

        // Non synchronizable doc type (not Folderish nor a BlobHolder)
        notADriveItem = session.createDocumentModel("/", "notADriveItem",
                "NotSynchronizable");
        notADriveItem = session.createDocument(notADriveItem);
    }

    @Test
    public void testAdapterFactory() throws Exception {

        // ------------------------------------------------------
        // Check downloadable NuxeoDriveItems
        // ------------------------------------------------------
        // Check File
        FileSystemItem driveItem = file.getAdapter(FileSystemItem.class);
        assertNotNull(driveItem);
        assertTrue(driveItem instanceof FileItem);
        assertEquals("test/" + file.getId(), driveItem.getId());
        assertEquals("Joe.odt", driveItem.getName());
        assertFalse(driveItem.isFolder());
        assertEquals("Joe", driveItem.getCreator());
        Blob fileItemBlob = ((FileItem) driveItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // Check Note
        driveItem = note.getAdapter(FileSystemItem.class);
        assertNotNull(driveItem);
        assertTrue(driveItem instanceof FileItem);
        assertEquals("test/" + note.getId(), driveItem.getId());
        assertEquals("aNote.txt", driveItem.getName());
        assertFalse(driveItem.isFolder());
        assertEquals("Bob", driveItem.getCreator());
        fileItemBlob = ((FileItem) driveItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());

        // Check custom doc type with the "file" schema
        driveItem = custom.getAdapter(FileSystemItem.class);
        assertNotNull(driveItem);
        assertTrue(driveItem instanceof FileItem);
        assertEquals("test/" + custom.getId(), driveItem.getId());
        assertEquals("Bonnie's file.odt", driveItem.getName());
        assertFalse(driveItem.isFolder());
        assertEquals("Bonnie", driveItem.getCreator());
        fileItemBlob = ((FileItem) driveItem).getBlob();
        assertEquals("Bonnie's file.odt", fileItemBlob.getFilename());
        assertEquals("Content of Bonnie's file.", fileItemBlob.getString());

        // Check File without a blob => not synchronizable
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        driveItem = file.getAdapter(FileSystemItem.class);
        assertNull(driveItem);

        // ------------------------------------------------------
        // Check folderish NuxeoDriveItem
        // ------------------------------------------------------
        driveItem = folder.getAdapter(FileSystemItem.class);
        assertNotNull(driveItem);
        assertTrue(driveItem instanceof FolderItem);
        assertEquals("test/" + folder.getId(), driveItem.getId());
        assertEquals("Jack's folder", driveItem.getName());
        assertTrue(driveItem.isFolder());
        assertEquals("Jack", driveItem.getCreator());
        FolderItem folderItem = (FolderItem) driveItem;
        List<FileSystemItem> children = folderItem.getChildren(session);
        assertNotNull(children);

        // ------------------------------------------------------
        // Check not adaptable to NuxeoDriveItem
        // ------------------------------------------------------
        driveItem = notADriveItem.getAdapter(FileSystemItem.class);
        assertNull(driveItem);
    }

}
