/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositoryVersioning extends SQLRepositoryTestCase {

    public TestSQLRepositoryVersioning(String name) {
        super(name);
    }

    private static final Log log = LogFactory.getLog(TestSQLRepositoryVersioning.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    protected void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    //
    //
    // -----------------------------------------------------------
    // ----- copied from TestVersioning in nuxeo-core-facade -----
    // -----------------------------------------------------------
    //
    //

    public void testSaveAsNewVersion() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");

        session.save();
        session.checkIn(file.getRef(), version);
        session.checkOut(file.getRef());

        DocumentModel newFileVersion = session.saveDocumentAsNewVersion(file);
    }

    public void testSaveAsNewVersion2() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        DocumentModel newFileVersion = session.saveDocumentAsNewVersion(file);
    }

    // SUPNXP-60: Suppression d'une version d'un document.
    public void testRemoveSingleDocVersion() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        checkVersions(file, new String[0]);

        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1" });

        DocumentModel lastversion = session.getLastDocumentVersion(file.getRef());
        assertNotNull(lastversion);

        log.info("removing version with label: " +
                lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, new String[0]);
    }

    // Creates 3 versions and removes the first.
    public void testRemoveFirstDocVersion() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 0;
        DocumentModel firstversion = session.getVersions(file.getRef()).get(
                VERSION_INDEX);
        assertNotNull(firstversion);

        log.info("removing version with label: " +
                firstversion.getVersionLabel());

        assertTrue(firstversion.isVersion());
        session.removeDocument(firstversion.getRef());

        checkVersions(file, new String[] { "2", "3" });
    }

    // Creates 3 versions and removes the second.
    public void testRemoveMiddleDocVersion() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 1;
        DocumentModel version = session.getVersions(file.getRef()).get(
                VERSION_INDEX);
        assertNotNull(version);

        log.info("removing version with label: " + version.getVersionLabel());

        assertTrue(version.isVersion());
        session.removeDocument(version.getRef());

        checkVersions(file, new String[] { "1", "3" });
    }

    // Creates 3 versions and removes the last.
    public void testRemoveLastDocVersion() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 2;
        DocumentModel lastversion = session.getVersions(file.getRef()).get(
                VERSION_INDEX);
        assertNotNull(lastversion);

        log.info("removing version with label: " +
                lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, new String[] { "1", "2" });
    }

    private void createTrioVersions(DocumentModel file) throws Exception {
        // create a first version
        file.setProperty("file", "filename", "A");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1" });

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "filename", "B");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1", "2" });

        // create a third version
        file.setProperty("file", "filename", "C");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1", "2", "3" });
    }

    private void checkVersions(DocumentModel doc, String[] labels)
            throws Exception {
        List<DocumentModel> vers = session.getVersions(doc.getRef());
        assertEquals(labels.length, vers.size());
        int i = 0;
        for (DocumentModel ver : vers) {
            assertTrue(ver.isVersion());
            assertEquals(labels[i], ver.getVersionLabel());
            i++;
        }
        List<DocumentRef> versionsRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

    //
    //
    // ----------------------------------------------------
    // ----- copied from TestAPI in nuxeo-core-facade -----
    // ----------------------------------------------------
    //
    //

    public void testGetVersionsForDocument() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#123";
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = session.createDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        version.setDescription("d1");
        // only label and description are currently supported
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);

        session.save();
        session.checkIn(childFile.getRef(), version);

        List<VersionModel> versions = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions);
        assertEquals(1, versions.size());
        assertNotNull(versions.get(0));
        assertNotNull(versions.get(0));
        assertEquals("v1", versions.get(0).getLabel());
        assertEquals("d1", versions.get(0).getDescription());
        // only label and descriptions are currently supported
        // assertEquals(cal.getTime().getTime(),
        // versions.get(0).getCreated().getTime().getTime());

        // creating a second version without description
        session.checkOut(childFile.getRef());
        VersionModel version2 = new VersionModelImpl();
        version2.setLabel("v2");

        session.save();
        session.checkIn(childFile.getRef(), version2);

        List<VersionModel> versions2 = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions2);
        assertEquals(2, versions2.size());
        assertNotNull(versions2.get(0));
        assertNotNull(versions2.get(0));
        assertEquals("v1", versions2.get(0).getLabel());
        assertEquals("d1", versions2.get(0).getDescription());
        assertNotNull(versions2.get(1));
        assertNotNull(versions2.get(1));
        assertEquals("v2", versions2.get(1).getLabel());
        assertNull(versions2.get(1).getDescription());
    }

    public void testCheckIn() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#789";
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = session.createDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        session.save();

        session.checkIn(childFile.getRef(), version);
    }

    public void testGetCheckedOut() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#135";
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = session.createDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setCreated(Calendar.getInstance());

        session.save();

        session.checkIn(childFile.getRef(), version);
        assertFalse(session.isCheckedOut(childFile.getRef()));
        session.checkOut(childFile.getRef());
        assertTrue(session.isCheckedOut(childFile.getRef()));
    }

    public void testRestoreToVersion() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#456";
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setDescription("d1");
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);
        session.save();
        session.checkIn(docRef, version);
        assertFalse(session.isCheckedOut(docRef));
        session.checkOut(docRef);
        assertTrue(session.isCheckedOut(docRef));

        doc.setProperty("file", "filename", "second name");
        doc.setProperty("common", "title", "f1");
        doc.setProperty("common", "description", "desc 1");
        session.saveDocument(doc);
        session.save();

        version.setLabel("v2");
        session.checkIn(docRef, version);
        session.checkOut(docRef);

        DocumentModel newDoc = session.getDocument(docRef);
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");
        DocumentModel restoredDoc = session.restoreToVersion(docRef, version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        restoredDoc = session.restoreToVersion(docRef, version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        String pr = (String) restoredDoc.getProperty("file", "filename");
        assertEquals("second name", pr);
    }

    public void testGetDocumentWithVersion() throws Exception {
        DocumentModel root = session.getRootDocument();

        String name2 = "file#248";
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                name2, "File");
        childFile = session.createDocument(childFile);

        VersionModel version = new VersionModelImpl();
        version.setLabel("v1");
        // only label is used for the moment
        // version.setDescription("d1");
        // Calendar cal = Calendar.getInstance();
        // version.setCreated(cal);
        session.save();
        session.checkIn(childFile.getRef(), version);
        session.checkOut(childFile.getRef());

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("common", "title", "f1");
        childFile.setProperty("common", "description", "desc 1");
        session.saveDocument(childFile);
        session.save();
        version = new VersionModelImpl();
        version.setLabel("v2");
        session.checkIn(childFile.getRef(), version);
        session.checkOut(childFile.getRef());

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        version.setLabel("v1");

        DocumentModel restoredDoc = session.restoreToVersion(
                childFile.getRef(), version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        version.setLabel("v2");
        // TODO: this fails as there is a NPE because a document version does
        // not currently have a document type
        restoredDoc = session.getDocumentWithVersion(childFile.getRef(),
                version);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("second name", restoredDoc.getProperty("file", "filename"));
    }

}
