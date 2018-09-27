/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins <tmartins@nuxeo.com>
 */
package org.nuxeo.drive.service.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.security.Principal;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.drive.test.NuxeoDriveFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * The purpose of this class is to test the {@link DefaultFileSystemItemFactory} with various versioning options.
 * <ul>
 * <li>Default Configuration (DF)</li>
 * <li>Automatic Minor Versioning (AMV)</li>
 * <li>Automatic Minor Versioning + Drive Forced Versioning Disabled (AMV + DFVD)</li>
 * </ul>
 * The tested scenario is:
 * <ul>
 * <li>0. Initial state</li>
 * <li>1. Change without delay by same user</li>
 * <li>2. Change with delay by same user</li>
 * <li>3. Change with delay by same user</li>
 * <li>4. Change without delay by same user</li>
 * <li>5. Change without delay by another user</li>
 * <li>6. Change with delay by the same user</li>
 * </ul>
 * The expected version label is:
 * <table>
 * <caption>Matrix of the expected version label for each step of the scenario and each versioning policy</caption>
 * <tr>
 * <th>STEP</th>
 * <th>DF</th>
 * <th>AMV</th>
 * <th>AMV + DFVD</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>0.0</td>
 * <td>0.1</td>
 * <td>0.1</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>0.0</td>
 * <td>0.1+</td>
 * <td>0.2</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>1.0+</td>
 * <td>1.1</td>
 * <td>0.3</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>2.0+</td>
 * <td>1.2</td>
 * <td>0.4</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>2.0+</td>
 * <td>1.2+</td>
 * <td>0.5</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>3.0+</td>
 * <td>2.1</td>
 * <td>0.6</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>4.0+</td>
 * <td>2.2</td>
 * <td>0.7</td>
 * </tr>
 * </table>
 *
 * @author Thierry Martins
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveFeature.class)
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-versioning-filter-contrib.xml")
@Deploy("org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-versioning-policy-contrib.xml")
public class TestDriveVersioning {

    private static final int VERSIONING_DELAY = 1000; // ms

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected FileSystemItemAdapterService fileSystemItemAdapterService;

    @Inject
    protected NuxeoDriveManager nuxeoDriveManager;

    protected Principal principal;

    protected DocumentModel syncRootFolder;

    protected DocumentModel file;

    protected FileSystemItemFactory defaultFileSystemItemFactory;

    @Before
    public void createTestDocs() throws Exception {
        principal = session.getPrincipal();
        syncRootFolder = session.createDocumentModel("/", "syncRoot", "Folder");
        syncRootFolder = session.createDocument(syncRootFolder);
        nuxeoDriveManager.registerSynchronizationRoot(principal, syncRootFolder, session);

        // File
        file = session.createDocumentModel(syncRootFolder.getPathAsString(), "aFile", "File");
        Blob blob = new StringBlob("Content of Joe's file.");
        blob.setFilename("Joe.odt");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        session.save();

        // Get default file system item factory
        defaultFileSystemItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "defaultFileSystemItemFactory");
    }

    @Test
    public void testDefaultConfiguration() throws Exception {

        FileItem fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the
        // joe session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertFalse(fileItem.getCanUpdate());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertTrue(fileItem.getCanUpdate());

            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

            // ------------------------------------------------------
            // FileItem#getBlob
            // ------------------------------------------------------
            Blob fileItemBlob = fileItem.getBlob();
            assertEquals("Joe.odt", fileItemBlob.getFilename());
            assertEquals("Content of Joe's file.", fileItemBlob.getString());
            // Check initial version
            assertEquals("0.0", file.getVersionLabel());

            // ------------------------------------------------------
            // 1. Change without delay
            // ------------------------------------------------------
            Blob newBlob = new StringBlob("This is a new file.");
            newBlob.setFilename("New blob.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("New blob.txt", updatedBlob.getFilename());
            assertEquals("This is a new file.", updatedBlob.getString());
            // Check versioning => should not be versioned since same contributor
            // and last modification was done before the versioning delay
            // Should not be checked out since previously checked out
            assertEquals("0.0", file.getVersionLabel());

            // ------------------------------------------------------
            // 2. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "versioning-delay" policy
            // since last modification was done after the versioning delay
            // Should be checked out since the "versioning-delay" policy is applied before update
            assertEquals("1.0+", file.getVersionLabel());
            List<DocumentModel> fileVersions = session.getVersions(file.getRef());
            assertEquals(1, fileVersions.size());
            DocumentModel lastFileVersion = fileVersions.get(0);
            Blob versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("New blob.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 3. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified again.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "versioning-delay" policy
            // since last modification was done after the versioning delay
            // Should be checked out since the "versioning-delay" policy is applied before update
            assertEquals("2.0+", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(2, fileVersions.size());
            lastFileVersion = fileVersions.get(1);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 4. Change without delay
            // ------------------------------------------------------
            newBlob.setFilename("File name modified again as draft.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again as draft.txt", updatedBlob.getFilename());
            // Check versioning => should not be versioned since same contributor
            // and last modification was done before the versioning delay
            // Should not be checked out since previously checked out
            assertEquals("2.0+", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(2, fileVersions.size());
            lastFileVersion = fileVersions.get(1);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 5. Change without delay with another user
            // ------------------------------------------------------
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "collaborative-save" policy
            // since updated by a different contributor
            // Should be checked out since the "collaborative-save" policy is applied before update
            assertEquals("3.0+", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(3, fileVersions.size());
            lastFileVersion = fileVersions.get(2);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again as draft.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 6. Change with delay with the same user
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe again.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe again.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "versioning-delay" policy
            // since last modification was done after the versioning delay
            // Should be checked out since the "versioning-delay" policy is applied before update
            assertEquals("4.0+", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", versionedBlob.getFilename());
        }
        resetPermissions(rootDoc, "joe");
    }

    @Test
    @Deploy({ "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-versioning-file-policy-not-drive-contrib.xml" })
    public void testAutomaticVersioning() throws Exception {

        FileItem fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the
        // joe session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertFalse(fileItem.getCanUpdate());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertTrue(fileItem.getCanUpdate());

            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

            // ------------------------------------------------------
            // FileItem#getBlob
            // ------------------------------------------------------
            Blob fileItemBlob = fileItem.getBlob();
            assertEquals("Joe.odt", fileItemBlob.getFilename());
            assertEquals("Content of Joe's file.", fileItemBlob.getString());
            // Check initial version
            // A version with a minor increment is automatically created during creation for all File documents
            // by the "version-file" policy (automatic versioning),
            // see test-nuxeodrive-versioning-file-policy-not-drive-contrib.xml
            assertEquals("0.1", file.getVersionLabel());

            // ------------------------------------------------------
            // 1. Change without delay
            // ------------------------------------------------------
            Blob newBlob = new StringBlob("This is a new file.");
            newBlob.setFilename("New blob.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("New blob.txt", updatedBlob.getFilename());
            assertEquals("This is a new file.", updatedBlob.getString());
            // Check versioning => should not be versioned since same contributor,
            // last modification was done before the versioning delay
            // and the "drive-force-versioning" policy prevents automatic versioning
            // from performing a minor increment,
            // see test-nuxeodrive-versioning-file-policy-not-drive-contrib.xml
            // Should be checked out since not previously checked out
            assertEquals("0.1+", file.getVersionLabel());

            // ------------------------------------------------------
            // 2. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "versioning-delay" policy
            // since last modification was done after the versioning delay
            // and a minor increment by the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("1.1", file.getVersionLabel());
            List<DocumentModel> fileVersions = session.getVersions(file.getRef());
            assertEquals(3, fileVersions.size());
            DocumentModel firstMinorFileVersion = fileVersions.get(0);
            Blob versionedBlob = (Blob) firstMinorFileVersion.getPropertyValue("file:content");
            assertEquals("Joe.odt", versionedBlob.getFilename());
            DocumentModel lastMajorFileVersion = fileVersions.get(1);
            versionedBlob = (Blob) lastMajorFileVersion.getPropertyValue("file:content");
            assertEquals("New blob.txt", versionedBlob.getFilename());
            DocumentModel lastFileVersion = fileVersions.get(2);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 3. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified again.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "versioning-delay" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("1.2", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 4. Change without delay
            // ------------------------------------------------------
            newBlob.setFilename("File name modified again as new draft.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again as new draft.txt", updatedBlob.getFilename());
            // Check versioning => should not be versioned since same contributor,
            // last modification was done before the versioning delay
            // and the "drive-force-versioning" policy prevents automatic versioning
            // from performing a minor increment,
            // see test-nuxeodrive-versioning-file-policy-not-drive-contrib.xml
            // Should be checked out since not previously checked out
            assertEquals("1.2+", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 5. Change without delay with another user
            // ------------------------------------------------------
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", updatedBlob.getFilename());
            // Check versioning => should be versioned with a major increment by the "collaborative-save" policy
            // since updated by a different contributor
            // and a minor increment by the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("2.1", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(6, fileVersions.size());
            lastMajorFileVersion = fileVersions.get(4);
            versionedBlob = (Blob) lastMajorFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again as new draft.txt", versionedBlob.getFilename());
            lastFileVersion = fileVersions.get(5);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 6. Change with delay with the same user
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe again.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe again.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "versioning-delay" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("2.2", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(7, fileVersions.size());
            lastFileVersion = fileVersions.get(6);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified by Joe again.txt", versionedBlob.getFilename());
        }
        resetPermissions(rootDoc, "joe");
    }

    @Test
    @Deploy({ "org.nuxeo.drive.core:OSGI-INF/test-nuxeodrive-versioning-file-policy-contrib.xml" })
    public void testAutomaticVersioningAndDriveForceVersionDisabled() throws Exception {

        FileItem fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

        // As a user with READ permission
        DocumentModel rootDoc = session.getRootDocument();
        setPermission(rootDoc, "joe", SecurityConstants.READ, true);

        // Under Oracle, the READ ACL optims are not visible from the
        // joe session while the transaction has not been committed.
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            nuxeoDriveManager.registerSynchronizationRoot(joeSession.getPrincipal(), syncRootFolder, session);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertFalse(fileItem.getCanUpdate());

            // As a user with WRITE permission
            setPermission(rootDoc, "joe", SecurityConstants.WRITE, true);
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            assertTrue(fileItem.getCanUpdate());

            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);

            // ------------------------------------------------------
            // FileItem#getBlob
            // ------------------------------------------------------
            Blob fileItemBlob = fileItem.getBlob();
            assertEquals("Joe.odt", fileItemBlob.getFilename());
            assertEquals("Content of Joe's file.", fileItemBlob.getString());
            // Check initial version
            // A version with a minor increment is automatically created during creation for all File documents
            // by the "version-file" policy (automatic versioning),
            // see test-nuxeodrive-versioning-file-policy-contrib.xml
            assertEquals("0.1", file.getVersionLabel());

            // ------------------------------------------------------
            // 1. Change without delay
            // ------------------------------------------------------
            Blob newBlob = new StringBlob("This is a new file.");
            newBlob.setFilename("New blob.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            Blob updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("New blob.txt", updatedBlob.getFilename());
            assertEquals("This is a new file.", updatedBlob.getString());
            // Check versioning => should be versioned with a minor increment by the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.2", file.getVersionLabel());

            // ------------------------------------------------------
            // 2. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "versioning-delay" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.3", file.getVersionLabel());
            List<DocumentModel> fileVersions = session.getVersions(file.getRef());
            assertEquals(3, fileVersions.size());
            DocumentModel firstMinorFileVersion = fileVersions.get(0);
            Blob versionedBlob = (Blob) firstMinorFileVersion.getPropertyValue("file:content");
            assertEquals("Joe.odt", versionedBlob.getFilename());
            DocumentModel secondMinorFileVersion = fileVersions.get(1);
            versionedBlob = (Blob) secondMinorFileVersion.getPropertyValue("file:content");
            assertEquals("New blob.txt", versionedBlob.getFilename());
            DocumentModel lastFileVersion = fileVersions.get(2);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 3. Change with delay
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            newBlob.setFilename("File name modified again.txt");
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "versioning-delay" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.4", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(4, fileVersions.size());
            lastFileVersion = fileVersions.get(3);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 4. Change without delay
            // ------------------------------------------------------
            newBlob.setFilename("File name modified again as draft.txt");
            ensureJustModified(file, session);
            fileItem.setBlob(newBlob);
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified again as draft.txt", updatedBlob.getFilename());
            // Check versioning => should not be versioned since last modification was done before the versioning delay
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.5", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(5, fileVersions.size());
            lastFileVersion = fileVersions.get(4);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified again as draft.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 5. Change without delay with another user
            // ------------------------------------------------------
            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "collaborative-save" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.6", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(6, fileVersions.size());
            lastFileVersion = fileVersions.get(5);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified by Joe.txt", versionedBlob.getFilename());

            // ------------------------------------------------------
            // 6. Change with delay with the same user
            // ------------------------------------------------------
            // Wait for versioning delay
            Thread.sleep(VERSIONING_DELAY);

            file = joeSession.getDocument(file.getRef());
            fileItem = (FileItem) defaultFileSystemItemFactory.getFileSystemItem(file);
            newBlob.setFilename("File name modified by Joe again.txt");
            fileItem.setBlob(newBlob);
            // Re-fetch file with Administrator session
            file = session.getDocument(file.getRef());
            updatedBlob = (Blob) file.getPropertyValue("file:content");
            assertEquals("File name modified by Joe again.txt", updatedBlob.getFilename());
            // Check versioning => no major increment by the "versioning-delay" policy
            // because the document is already checked in,
            // but versioned with a minor increment because of the "version-file" policy
            // Should not be checked out since the "version-file" policy is not applied before update
            assertEquals("0.7", file.getVersionLabel());
            fileVersions = session.getVersions(file.getRef());
            assertEquals(7, fileVersions.size());
            lastFileVersion = fileVersions.get(6);
            versionedBlob = (Blob) lastFileVersion.getPropertyValue("file:content");
            assertEquals("File name modified by Joe again.txt", versionedBlob.getFilename());
        }
        resetPermissions(rootDoc, "joe");
    }

    @Test
    public void testSyncRootVersioning() throws Exception {
        // Expect no versions initially
        // Cannot use DocumentModel#getVersionLabel since it relies on the uid schema not held by the Folder type
        assertTrue(session.getVersions(syncRootFolder.getRef()).isEmpty());

        // Wait for the versioning delay and update the synchronization root
        Thread.sleep(VERSIONING_DELAY);
        FileSystemItemFactory defaultSyncRootFolderItemFactory = ((FileSystemItemAdapterServiceImpl) fileSystemItemAdapterService).getFileSystemItemFactory(
                "defaultSyncRootFolderItemFactory");
        FolderItem syncRootFolderItem = (FolderItem) defaultSyncRootFolderItemFactory.getFileSystemItem(syncRootFolder);
        syncRootFolderItem.rename("syncRootRenamed");
        syncRootFolder = session.getDocument(syncRootFolder.getRef());

        // Expect no versions since the "versioning-delay" policy doesn't apply to folderish documents
        assertTrue(session.getVersions(syncRootFolder.getRef()).isEmpty());
    }

    @Test
    public void testFolderVersioning() throws Exception {
        // Create a Folder in the synchronization root
        DocumentModel folder = session.createDocumentModel("/syncRoot", "aFolder", "Folder");
        folder = session.createDocument(folder);
        session.save();

        // Expect no versions initially
        // Cannot use DocumentModel#getVersionLabel since it relies on the uid schema not held by the Folder type
        assertTrue(session.getVersions(folder.getRef()).isEmpty());

        // Wait for the versioning delay and update the folder
        Thread.sleep(VERSIONING_DELAY);
        FolderItem folderItem = (FolderItem) defaultFileSystemItemFactory.getFileSystemItem(folder);
        folderItem.rename("aFolderRenamed");
        folder = session.getDocument(folder.getRef());

        // Expect no versions since the "versioning-delay" policy doesn't apply to folderish documents
        assertTrue(session.getVersions(folder.getRef()).isEmpty());
    }

    protected void setPermission(DocumentModel doc, String userName, String permission, boolean isGranted) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(userName, permission, isGranted));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected void resetPermissions(DocumentModel doc, String userName) {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        Iterator<ACE> localACLIt = localACL.iterator();
        while (localACLIt.hasNext()) {
            ACE ace = localACLIt.next();
            if (userName.equals(ace.getUsername())) {
                localACLIt.remove();
            }
        }
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

    protected long getVersion(DocumentModel doc, String prop) {
        Object propVal = doc.getPropertyValue(prop);
        if (propVal == null || !(propVal instanceof Long)) {
            return -1;
        } else {
            return ((Long) propVal).longValue();
        }
    }

    /**
     * Ensures that the given document has just been modified to avoid a false positive when checking if versioning is
     * needed in {@link DefaultFileSystemItemFactory#needsVersioning(DocumentModel)}.
     */
    protected void ensureJustModified(DocumentModel doc, CoreSession session) {
        doc.setPropertyValue("dc:modified", Calendar.getInstance());
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        session.saveDocument(doc);
    }

}
