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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
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
 * Tests the {@link DefaultFileSystemItemFactory}.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.drive.core")
@LocalDeploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-types-contrib.xml")
public class TestDefaultFileSystemItemFactory {

    @Inject
    protected CoreSession session;

    protected DocumentModel file;

    protected DocumentModel note;

    protected DocumentModel custom;

    protected DocumentModel folder;

    protected DocumentModel folderishFile;

    protected DocumentModel notAFileSystemItem;

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

        // FolderishFile: doc type with the "file" schema and the "Folderish"
        // facet
        folderishFile = session.createDocumentModel("/", "aFolderishFile",
                "FolderishFile");
        folderishFile.setPropertyValue("dc:title", "Sarah's folderish file");
        folderishFile.setPropertyValue("dc:creator", "Sarah");
        folderishFile = session.createDocument(folderishFile);

        // Doc not adaptable as a FileSystemItem (not Folderish nor a
        // BlobHolder)
        notAFileSystemItem = session.createDocumentModel("/",
                "notAFileSystemItem", "NotSynchronizable");
        notAFileSystemItem = session.createDocument(notAFileSystemItem);
    }

    @Test
    public void testFactory() throws Exception {

        // ------------------------------------------------------
        // Check downloadable FileSystemItems
        // ------------------------------------------------------
        // Check File
        FileSystemItem fsItem = file.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + file.getId(), fsItem.getId());
        assertEquals("Joe.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Joe", fsItem.getCreator());
        Blob fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        // Check Note
        fsItem = note.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + note.getId(), fsItem.getId());
        assertEquals("aNote.txt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Bob", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("aNote.txt", fileItemBlob.getFilename());
        assertEquals("Content of Bob's note.", fileItemBlob.getString());

        // Check custom doc type with the "file" schema
        fsItem = custom.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FileItem);
        assertEquals("test/" + custom.getId(), fsItem.getId());
        assertEquals("Bonnie's file.odt", fsItem.getName());
        assertFalse(fsItem.isFolder());
        assertEquals("Bonnie", fsItem.getCreator());
        fileItemBlob = ((FileItem) fsItem).getBlob();
        assertEquals("Bonnie's file.odt", fileItemBlob.getFilename());
        assertEquals("Content of Bonnie's file.", fileItemBlob.getString());

        // Check File without a blob => not adaptable as a FileSystemItem
        file.setPropertyValue("file:content", null);
        file = session.saveDocument(file);
        fsItem = file.getAdapter(FileSystemItem.class);
        assertNull(fsItem);

        // ------------------------------------------------------
        // Check folderish FileSystemItems
        // ------------------------------------------------------
        // Check Folder
        fsItem = folder.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("test/" + folder.getId(), fsItem.getId());
        assertEquals("Jack's folder", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Jack", fsItem.getCreator());
        List<FileSystemItem> children = ((FolderItem) fsItem).getChildren();
        assertNotNull(children);
        assertEquals(0, children.size());

        // Check FolderishFile => adaptable as a FolderItem since the default
        // FileSystemItem factory gives precedence to the Folderish facet
        fsItem = folderishFile.getAdapter(FileSystemItem.class);
        assertNotNull(fsItem);
        assertTrue(fsItem instanceof FolderItem);
        assertEquals("test/" + folderishFile.getId(), fsItem.getId());
        assertEquals("Sarah's folderish file", fsItem.getName());
        assertTrue(fsItem.isFolder());
        assertEquals("Sarah", fsItem.getCreator());

        // ------------------------------------------------------
        // Check not adaptable as a FileSystemItem
        // ------------------------------------------------------
        fsItem = notAFileSystemItem.getAdapter(FileSystemItem.class);
        assertNull(fsItem);
    }

    @Test
    public void testFileItem() throws Exception {

        // ------------------------------------------------------
        // FileItem#setBlob
        // ------------------------------------------------------
        FileItem fileItem = (FileItem) file.getAdapter(FileSystemItem.class);
        Blob fileItemBlob = fileItem.getBlob();
        assertEquals("Joe.odt", fileItemBlob.getFilename());
        assertEquals("Content of Joe's file.", fileItemBlob.getString());

        Blob newBlob = new StringBlob("This is a new file.");
        newBlob.setFilename("New blob.txt");
        fileItem.setBlob(newBlob);

        file = session.getDocument(file.getRef());
        Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
        assertEquals("New blob.txt", updatedBlob.getFilename());
        assertEquals("This is a new file.", updatedBlob.getString());
    }

}
