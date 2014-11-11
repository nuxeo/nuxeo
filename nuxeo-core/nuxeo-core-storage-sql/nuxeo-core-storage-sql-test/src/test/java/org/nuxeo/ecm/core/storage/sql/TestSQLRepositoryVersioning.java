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

package org.nuxeo.ecm.core.storage.sql;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositoryVersioning extends SQLRepositoryTestCase {

    public TestSQLRepositoryVersioning(String name) {
        super(name);
    }

    private static final Log log = LogFactory.getLog(TestSQLRepositoryVersioning.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
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

    /**
     * Sleep 1s, useful for stupid databases (like MySQL) that don't have
     * subsecond resolution in TIMESTAMP fields.
     */
    public void maybeSleepToNextSecond() {
        DatabaseHelper.DATABASE.maybeSleepToNextSecond();
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

        checkVersions(file);

        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, "1");

        DocumentModel lastversion = session.getLastDocumentVersion(file.getRef());
        assertNotNull(lastversion);

        log.info("removing version with label: "
                + lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file);
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

        log.info("removing version with label: "
                + firstversion.getVersionLabel());

        assertTrue(firstversion.isVersion());
        session.removeDocument(firstversion.getRef());

        checkVersions(file, "2", "3");
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

        checkVersions(file, "1", "3");
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

        log.info("removing version with label: "
                + lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, "1", "2");
    }

    public void testSnapshottingCreateDocument() throws Exception {
        DocumentModel file = new DocumentModelImpl("/", "file", "File");
        file.setProperty("file", "filename", "A");
        file = session.createDocument(file);
        // no version yet
        checkVersions(file);

        // do another save, doc should still be dirty
        file = session.saveDocument(file);
        checkVersions(file);

        // create snapshot of B state when saving
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1");
    }

    public void testSnapshotting() throws Exception {
        DocumentModel file = new DocumentModelImpl("/", "file", "File");
        file.setProperty("file", "filename", "A");
        file = session.createDocument(file);
        session.save();
        // no version yet
        checkVersions(file);

        // create snapshot of A state before saving
        file.setProperty("file", "filename", "B");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1");

        // create a snapshot of the last change (B)
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1", "2");

        // another snapshot on save won't do anything, doc is not dirty
        file.setProperty("file", "filename", "C");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1", "2");

        // but last change (C) now has to be saved on snapshot
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1", "2", "3");

        // simple save of a prop, makes the doc diry again
        file.setProperty("file", "filename", "D");
        file = session.saveDocument(file);
        checkVersions(file, "1", "2", "3");
        // then snapshot
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);
        checkVersions(file, "1", "2", "3", "4");
    }

    private void createTrioVersions(DocumentModel file) throws Exception {
        // create a first version
        file.setProperty("file", "filename", "A");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, "1");

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "filename", "B");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        maybeSleepToNextSecond();
        file = session.saveDocument(file);

        checkVersions(file, "1", "2");

        // create a third version
        file.setProperty("file", "filename", "C");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        maybeSleepToNextSecond();
        file = session.saveDocument(file);

        checkVersions(file, "1", "2", "3");
    }

    private void checkVersions(DocumentModel doc, String... labels)
            throws Exception {
        List<String> actual = new LinkedList<String>();
        for (DocumentModel ver : session.getVersions(doc.getRef())) {
            assertTrue(ver.isVersion());
            actual.add(ver.getVersionLabel());
        }
        assertEquals(Arrays.asList(labels), actual);
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

        // test direct lookup (as administrator)
        DocumentModel ver = session.getVersion(childFile.getId(), version);
        assertNotNull(ver);
        assertEquals("d1", version.getDescription());
        assertNotNull(version.getCreated());

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
        maybeSleepToNextSecond();
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
        doc.setProperty("dc", "title", "f1");
        doc.setProperty("dc", "description", "desc 1");
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
        childFile.setProperty("dc", "title", "f1");
        childFile.setProperty("dc", "description", "desc 1");
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

    // security on versions, see TestLocalAPIWithCustomVersioning

    public void testVersionSecurity() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        ACE ace = new ACE("princ1", "perm1", true);
        ACL acl = new ACLImpl("acl1", false);
        acl.add(ace);
        acp.addACL(acl);
        session.setACP(folder.getRef(), acp, true);
        DocumentModel file = new DocumentModelImpl("/folder", "file", "File");
        file = session.createDocument(file);
        // set security
        acp = new ACPImpl();
        ace = new ACE("princ2", "perm2", true);
        acl = new ACLImpl("acl2", false);
        acl.add(ace);
        acp.addACL(acl);
        session.setACP(file.getRef(), acp, true);
        session.save();
        VersionModel vm = new VersionModelImpl();
        vm.setLabel("v1");
        session.checkIn(file.getRef(), vm);
        session.checkOut(file.getRef());

        // check security on version
        DocumentModel version = session.getDocumentWithVersion(file.getRef(),
                vm);
        acp = session.getACP(version.getRef());
        ACL[] acls = acp.getACLs();
        assertEquals(2, acls.length);
        acl = acls[0];
        assertEquals(1, acl.size());
        assertEquals("princ2", acl.get(0).getUsername());
        acl = acls[1];
        assertEquals(1 + 4, acl.size()); // 1 + 4 root defaults
        assertEquals("princ1", acl.get(0).getUsername());

        // remove live document (create a proxy so the version stays)
        DocumentModel proxy = session.createProxy(folder.getRef(), file.getRef(), vm, true);
        session.save();
        session.removeDocument(file.getRef());
        session.save();
        // recheck security on version (works because we're administrator)
        acp = session.getACP(version.getRef());
        assertNull(acp);
        // check proxy still accessible (in another session)
        CoreSession session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
        try {
            session2.getDocument(proxy.getRef());
        } finally {
            closeSession(session2);
        }
    }

}
