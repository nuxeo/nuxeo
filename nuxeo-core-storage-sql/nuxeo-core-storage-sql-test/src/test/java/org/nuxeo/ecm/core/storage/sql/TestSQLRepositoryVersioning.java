/*
 * (C) Copyright 2008-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
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

public class TestSQLRepositoryVersioning extends SQLRepositoryTestCase {

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

    /**
     * Sleep 1s, useful for stupid databases (like MySQL) that don't have
     * subsecond resolution in TIMESTAMP fields.
     */
    public void maybeSleepToNextSecond() {
        DatabaseHelper.DATABASE.maybeSleepToNextSecond();
    }

    public void testRemoveSingleDocVersion() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
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
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
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
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
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
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
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
        maybeSleepToNextSecond();
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
        maybeSleepToNextSecond();
        file = session.saveDocument(file);
        checkVersions(file, "1", "2", "3");

        // simple save of a prop, makes the doc diry again
        file.setProperty("file", "filename", "D");
        file = session.saveDocument(file);
        checkVersions(file, "1", "2", "3");
        // then snapshot
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        maybeSleepToNextSecond();
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
            throws ClientException {
        List<String> actual = new LinkedList<String>();
        for (DocumentModel ver : session.getVersions(doc.getRef())) {
            assertTrue(ver.isVersion());
            actual.add(ver.getVersionLabel());
        }
        assertEquals(Arrays.asList(labels), actual);
        List<DocumentRef> versionsRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

    public void testGetVersionsForDocument() throws Exception {
        String name2 = "file#123";
        DocumentModel childFile = new DocumentModelImpl("/", name2, "File");
        childFile = session.createDocument(childFile);

        session.save();

        DocumentModel ver = session.checkIn(childFile.getRef(), "d1");

        // test direct lookup (as administrator)
        assertNotNull(ver);
        // TODO assertEquals("d1", version.getDescription());
        // TODO assertNotNull(version.getCreated());

        List<VersionModel> versions = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions);
        assertEquals(1, versions.size());
        assertNotNull(versions.get(0));
        assertNotNull(versions.get(0));
        assertEquals("1", versions.get(0).getLabel()); // internally generated
        assertEquals("d1", versions.get(0).getDescription());
        // only label and descriptions are currently supported
        // assertEquals(cal.getTime().getTime(),
        // versions.get(0).getCreated().getTime().getTime());

        // creating a second version without description
        session.checkOut(childFile.getRef());
        session.save();
        maybeSleepToNextSecond();
        session.checkIn(childFile.getRef(), (String) null);

        List<VersionModel> versions2 = session.getVersionsForDocument(childFile.getRef());

        assertNotNull(versions2);
        assertEquals(2, versions2.size());
        assertNotNull(versions2.get(0));
        assertNotNull(versions2.get(0));
        assertEquals("1", versions2.get(0).getLabel());
        assertEquals("d1", versions2.get(0).getDescription());
        assertNotNull(versions2.get(1));
        assertNotNull(versions2.get(1));
        assertEquals("2", versions2.get(1).getLabel());
        assertNull(versions2.get(1).getDescription());
    }

    public void testCheckInCheckOut() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "file#789", "File");
        doc = session.createDocument(doc);
        session.save();

        DocumentModel ver = session.checkIn(doc.getRef(), (String) null);
        assertTrue(ver.isVersion());
        doc.refresh();
        assertFalse(session.isCheckedOut(doc.getRef()));
        assertFalse(doc.isCheckedOut());

        session.checkOut(doc.getRef());
        assertTrue(session.isCheckedOut(doc.getRef()));

        // using DocumentModel API
        DocumentModel ver2 = doc.checkIn(null);
        assertTrue(ver2.isVersion());
        assertFalse(doc.isCheckedOut());
        doc.checkOut();
        assertTrue(doc.isCheckedOut());
    }

    public void testRestoreToVersion() throws Exception {
        String name2 = "file#456";
        DocumentModel doc = new DocumentModelImpl("/", name2, "File");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();

        session.save();
        DocumentModel v1 = session.checkIn(docRef, "d1");
        assertFalse(session.isCheckedOut(docRef));
        session.checkOut(docRef);
        assertTrue(session.isCheckedOut(docRef));

        doc.setProperty("file", "filename", "second name");
        doc.setProperty("dc", "title", "f1");
        doc.setProperty("dc", "description", "desc 1");
        session.saveDocument(doc);
        session.save();

        DocumentModel v2 = session.checkIn(docRef, "d2");
        session.checkOut(docRef);

        DocumentModel newDoc = session.getDocument(docRef);
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        DocumentModel restoredDoc = session.restoreToVersion(docRef, v1.getRef());

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        restoredDoc = session.restoreToVersion(docRef, v2.getRef());

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        String pr = (String) restoredDoc.getProperty("file", "filename");
        assertEquals("second name", pr);
    }

    public void testGetDocumentWithVersion() throws Exception {
        String name2 = "file#248";
        DocumentModel childFile = new DocumentModelImpl("/", name2, "File");
        childFile = session.createDocument(childFile);
        session.save();
        DocumentModel v1 = session.checkIn(childFile.getRef(), (String) null);
        session.checkOut(childFile.getRef());

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dc", "title", "f1");
        childFile.setProperty("dc", "description", "desc 1");
        session.saveDocument(childFile);
        session.save();
        DocumentModel v2 = session.checkIn(childFile.getRef(), (String) null);
        session.checkOut(childFile.getRef());

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        // restore, no snapshot as already pristine
        DocumentModel restoredDoc = session.restoreToVersion(
                childFile.getRef(), v1.getRef());

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        DocumentModel last = session.getLastDocumentVersion(childFile.getRef());
        assertNotNull(last);
        assertNotNull(last.getRef());
        assertEquals(v2.getId(), last.getId());
        assertEquals("second name", last.getProperty("file", "filename"));
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

        DocumentModel proxy = session.publishDocument(file, folder);
        DocumentModel version = session.getLastDocumentVersion(file.getRef());
        session.save();

        // check security on version
        acp = session.getACP(version.getRef());
        ACL[] acls = acp.getACLs();
        assertEquals(2, acls.length);
        acl = acls[0];
        assertEquals(1, acl.size());
        assertEquals("princ2", acl.get(0).getUsername());
        acl = acls[1];
        assertEquals(1 + 3, acl.size()); // 1 + 3 root defaults
        assertEquals("princ1", acl.get(0).getUsername());

        // remove live document (there's a proxy so the version stays)
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

    public void testVersionLifecycle() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "t1");
        doc = session.saveDocument(doc);

        session.publishDocument(doc, root);
        session.save();

        // get version
        DocumentModel ver = session.getLastDocumentVersion(doc.getRef());
        assertTrue(ver.isVersion());

        assertEquals("project", ver.getCurrentLifeCycleState());
        ver.followTransition("approve");
        session.save();

        closeSession();
        openSession();
        doc = session.getDocument(new PathRef("/doc"));
        ver = session.getLastDocumentVersion(doc.getRef());
        assertEquals("approved", ver.getCurrentLifeCycleState());
    }

    public void testTransitionProxy() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");

        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "t1");
        doc = session.saveDocument(doc);

        DocumentModel proxy = session.publishDocument(doc, root);
        session.save();

        Collection<String> transitions = proxy.getAllowedStateTransitions();
        assertEquals(3, transitions.size());

        if (proxy.getAllowedStateTransitions().contains("delete")) {
            proxy.followTransition("delete");
        }
        assertEquals("deleted", proxy.getCurrentLifeCycleState());
    }

    public void testPublishingAfterVersionDelete() throws ClientException {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        checkVersions(doc);

        VersionModel lastVersion = session.getLastVersion(doc.getRef());
        assertNull(lastVersion);
        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNull(lastVersionDocument);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        checkVersions(proxy);
        checkVersions(doc, "1");
        lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("1", lastVersion.getLabel());
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());

        // unpublish
        session.removeDocument(proxy.getRef());
        // delete the version
        List<VersionModel> versions = session.getVersionsForDocument(doc.getRef());
        assertEquals(1, versions.size());
        DocumentModel docVersion = session.getDocumentWithVersion(doc.getRef(),
                versions.get(0));
        session.removeDocument(docVersion.getRef());

        checkVersions(doc);
        lastVersion = session.getLastVersion(doc.getRef());
        assertNull(lastVersion);
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNull(lastVersionDocument);

        // republish
        DocumentModel newProxy = session.publishDocument(doc, folder);
        checkVersions(newProxy);
        checkVersions(doc, "1");
        lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("1", lastVersion.getLabel());
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());
    }

    public void testPublishingAfterCopy() throws ClientException {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        checkVersions(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        checkVersions(proxy);
        checkVersions(doc, "1");
        VersionModel lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("1", lastVersion.getLabel());
        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());

        // copy published file
        DocumentModel copy = session.copy(doc.getRef(), folder.getRef(),
                "fileCopied");
        checkVersions(copy);
        lastVersion = session.getLastVersion(copy.getRef());
        assertNull(lastVersion);
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNull(lastVersionDocument);

        // republish
        DocumentModel newProxy = session.publishDocument(copy, folder);
        checkVersions(newProxy);
        checkVersions(copy, "1");
        lastVersion = session.getLastVersion(copy.getRef());
        assertNotNull(lastVersion);
        assertEquals("1", lastVersion.getLabel());
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("fileCopied", lastVersionDocument.getName());
    }

}
