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

        file.setPropertyValue("file:filename", "A");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, "0.1");

        DocumentModel lastversion = session.getLastDocumentVersion(file.getRef());
        assertNotNull(lastversion);

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

        assertTrue(firstversion.isVersion());
        session.removeDocument(firstversion.getRef());

        checkVersions(file, "0.2", "0.3");
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

        assertTrue(version.isVersion());
        session.removeDocument(version.getRef());

        checkVersions(file, "0.1", "0.3");
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

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, "0.1", "0.2");
    }

    private void createTrioVersions(DocumentModel file) throws Exception {
        // create a first version
        file.setProperty("file", "filename", "A");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        file = session.saveDocument(file);

        checkVersions(file, "0.1");

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "filename", "B");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        maybeSleepToNextSecond();
        file = session.saveDocument(file);

        checkVersions(file, "0.1", "0.2");

        // create a third version
        file.setProperty("file", "filename", "C");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        maybeSleepToNextSecond();
        file = session.saveDocument(file);

        checkVersions(file, "0.1", "0.2", "0.3");
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

    public void testCheckInCheckOut() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "file#789", "File");
        doc = session.createDocument(doc);
        session.save();

        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);
        DocumentModel ver = session.getDocument(verRef);
        assertTrue(ver.isVersion());
        doc.refresh();
        assertFalse(session.isCheckedOut(doc.getRef()));
        assertFalse(doc.isCheckedOut());

        session.checkOut(doc.getRef());
        assertTrue(session.isCheckedOut(doc.getRef()));

        // using DocumentModel API
        DocumentRef verRef2 = doc.checkIn(null, null);
        DocumentModel ver2 = session.getDocument(verRef2);
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
        DocumentRef v1Ref = session.checkIn(docRef, null, null);
        assertFalse(session.isCheckedOut(docRef));
        session.checkOut(docRef);
        assertTrue(session.isCheckedOut(docRef));

        doc.setProperty("file", "filename", "second name");
        doc.setProperty("dc", "title", "f1");
        doc.setProperty("dc", "description", "desc 1");
        session.saveDocument(doc);
        session.save();

        DocumentRef v2Ref = session.checkIn(docRef, null, null);
        session.checkOut(docRef);

        DocumentModel newDoc = session.getDocument(docRef);
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        DocumentModel restoredDoc = session.restoreToVersion(docRef, v1Ref);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        restoredDoc = session.restoreToVersion(docRef, v2Ref);

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
        DocumentRef v1Ref = session.checkIn(childFile.getRef(), null, null);
        session.checkOut(childFile.getRef());

        childFile.setProperty("file", "filename", "second name");
        childFile.setProperty("dc", "title", "f1");
        childFile.setProperty("dc", "description", "desc 1");
        session.saveDocument(childFile);
        session.save();
        DocumentRef v2Ref = session.checkIn(childFile.getRef(), null, null);

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("second name", newDoc.getProperty("file", "filename"));

        // restore, no snapshot as already pristine
        DocumentModel restoredDoc = session.restoreToVersion(
                childFile.getRef(), v1Ref);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("file", "filename"));

        DocumentModel last = session.getLastDocumentVersion(childFile.getRef());
        assertNotNull(last);
        assertNotNull(last.getRef());
        assertEquals(v2Ref.reference(), last.getId());
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
        checkVersions(doc, "0.1");
        lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getLabel());
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
        checkVersions(doc, "0.2");
        lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.2", lastVersion.getLabel());
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
        checkVersions(doc, "0.1");
        VersionModel lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getLabel());
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
        checkVersions(copy, "0.2");
        lastVersion = session.getLastVersion(copy.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.2", lastVersion.getLabel());
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("fileCopied", lastVersionDocument.getName());
    }

}
