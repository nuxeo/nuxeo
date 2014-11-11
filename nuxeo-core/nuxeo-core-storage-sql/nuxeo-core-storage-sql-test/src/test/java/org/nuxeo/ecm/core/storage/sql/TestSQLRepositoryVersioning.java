/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;

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

    public void testCreateVersionsManyTimes() throws Exception {
        for (int i = 0; i < 10; i++) {
            createVersions(i);
        }
    }

    protected void createVersions(int i) throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "fold" + i, "Folder");
        session.createDocument(folder);
        DocumentModel file = new DocumentModelImpl("/fold" + i, "file", "File");
        file = session.createDocument(file);
        createTrioVersions(file);
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
        assertTrue(doc.isCheckedOut());
        doc = session.createDocument(doc);
        assertTrue(session.isCheckedOut(doc.getRef()));
        assertTrue(doc.isCheckedOut());
        session.save();
        assertTrue(session.isCheckedOut(doc.getRef()));
        assertTrue(doc.isCheckedOut());

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

    public void testRestoreInvalidations() throws Exception {
        // open second session to receive invalidations
        CoreSession session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);

        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        doc.setPropertyValue("dc:title", "t1");
        doc = session.createDocument(doc);
        DocumentRef docRef = doc.getRef();
        DocumentRef v1 = session.checkIn(docRef, null, null);
        session.checkOut(docRef);
        doc.setPropertyValue("dc:title", "t2");
        session.saveDocument(doc);
        DocumentRef v2 = session.checkIn(docRef, null, null);
        session.save();
        session2.save(); // process invalidations

        assertEquals("t2", doc.getPropertyValue("dc:title"));
        DocumentModel doc2 = session2.getDocument(docRef);
        assertEquals("t2", doc2.getPropertyValue("dc:title"));

        // restore v1
        DocumentModel restored = session.restoreToVersion(docRef, v1);
        assertEquals("t1", restored.getPropertyValue("dc:title"));
        session.save();
        session2.save();
        DocumentModel restored2 = session2.getDocument(docRef);
        assertEquals("t1", restored2.getPropertyValue("dc:title"));

        // restore v2
        restored = session.restoreToVersion(docRef, v2);
        assertEquals("t2", restored.getPropertyValue("dc:title"));
        session.save();
        session2.save();
        restored2 = session2.getDocument(docRef);
        assertEquals("t2", restored2.getPropertyValue("dc:title"));

        closeSession(session2);
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

    public void testCopy() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();
        String versionSeriesId = doc.getVersionSeriesId();

        // copy
        DocumentModel copy = session.copy(doc.getRef(),
                session.getRootDocument().getRef(), "fileCopied");

        // check different version series id
        assertNotSame(versionSeriesId, copy.getVersionSeriesId());

        // create version and proxy
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel proxy = session.publishDocument(doc, folder);
        // check same version series id
        assertEquals(versionSeriesId, proxy.getVersionSeriesId());

        // copy proxy
        DocumentModel proxyCopy = session.copy(proxy.getRef(),
                session.getRootDocument().getRef(), "proxyCopied");
        // check same version series id
        assertEquals(versionSeriesId, proxyCopy.getVersionSeriesId());
    }

    public void testPublishing() throws ClientException {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        checkVersions(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        session.save();
        String versionSeriesId = doc.getVersionSeriesId();
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isProxy());
        assertTrue(proxy.hasFacet(FacetNames.IMMUTABLE));
        assertTrue(proxy.isImmutable());
        assertEquals(versionSeriesId, proxy.getVersionSeriesId());
        assertNotSame(versionSeriesId, proxy.getId());
        assertEquals("0.1", proxy.getVersionLabel());
        assertNull(proxy.getCheckinComment());
        assertFalse(proxy.isMajorVersion());
        assertTrue(proxy.isLatestVersion());
        assertFalse(proxy.isLatestMajorVersion());

        checkVersions(doc, "0.1");
        VersionModel lastVersion = session.getLastVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getLabel());
        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());
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
        checkVersions(copy, "0.2");
        lastVersion = session.getLastVersion(copy.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.2", lastVersion.getLabel());
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("fileCopied", lastVersionDocument.getName());
    }

    public void testCmisProperties() throws Exception {

        /*
         * checked out doc (live; private working copy)
         */

        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        doc = session.createDocument(doc);

        assertTrue(doc.isCheckedOut()); // nuxeo prop, false only on live
        assertFalse(doc.isVersion());
        assertFalse(doc.isProxy());
        assertFalse(doc.hasFacet(FacetNames.IMMUTABLE));
        assertFalse(doc.isImmutable());
        String versionSeriesId = doc.getVersionSeriesId();
        assertNotNull(versionSeriesId);
        // assertNotSame(versionSeriesId, doc.getId());
        assertEquals("0.0", doc.getVersionLabel());
        assertNull(doc.getCheckinComment());
        assertFalse(doc.isMajorVersion());
        assertFalse(doc.isLatestVersion());
        assertFalse(doc.isLatestMajorVersion());
        assertTrue(doc.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(), session.getWorkingCopy(doc.getRef()).getId());

        /*
         * proxy to checked out doc (live proxy)
         */

        DocumentModel proxy = session.createProxy(doc.getRef(),
                session.getRootDocument().getRef());

        assertTrue(proxy.isCheckedOut()); // nuxeo prop, false only on live
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isProxy());
        assertFalse(proxy.hasFacet(FacetNames.IMMUTABLE));
        assertFalse(proxy.isImmutable());
        assertEquals(versionSeriesId, proxy.getVersionSeriesId());
        assertEquals("0.0", proxy.getVersionLabel());
        assertNull(proxy.getCheckinComment());
        assertFalse(proxy.isMajorVersion());
        assertFalse(proxy.isLatestVersion());
        assertFalse(proxy.isLatestMajorVersion());
        assertTrue(proxy.isVersionSeriesCheckedOut());
        assertTrue(doc.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(),
                session.getWorkingCopy(proxy.getRef()).getId());

        /*
         * checked in doc
         */

        DocumentRef verRef = doc.checkIn(VersioningOption.MINOR, "comment");
        session.save();
        DocumentModel ver = session.getDocument(verRef);
        proxy.refresh();

        assertFalse(doc.isCheckedOut());
        assertFalse(doc.isVersion());
        assertFalse(doc.isProxy());
        // assertTrue(doc.hasFacet(FacetNames.IMMUTABLE)); // debatable
        // assertTrue(doc.isImmutable()); // debatable
        assertEquals(versionSeriesId, doc.getVersionSeriesId());
        assertEquals("0.1", doc.getVersionLabel());
        assertNull(doc.getCheckinComment());
        assertFalse(doc.isMajorVersion());
        assertFalse(doc.isLatestVersion());
        assertFalse(doc.isLatestMajorVersion());
        assertFalse(doc.isVersionSeriesCheckedOut());
        assertFalse(proxy.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(),
                session.getWorkingCopy(proxy.getRef()).getId());

        // TODO proxy to checked in doc

        /*
         * version
         */

        // assertFalse(ver.isCheckedOut()); // TODO
        assertTrue(ver.isVersion());
        assertFalse(ver.isProxy());
        assertTrue(ver.hasFacet(FacetNames.IMMUTABLE));
        assertTrue(ver.isImmutable());
        assertEquals(versionSeriesId, ver.getVersionSeriesId());
        assertEquals("0.1", ver.getVersionLabel());
        assertEquals("comment", ver.getCheckinComment());
        assertFalse(ver.isMajorVersion());
        assertTrue(ver.isLatestVersion());
        assertFalse(ver.isLatestMajorVersion());
        assertFalse(ver.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(), session.getWorkingCopy(ver.getRef()).getId());

        /*
         * proxy to version
         */

        proxy = session.createProxy(ver.getRef(),
                session.getRootDocument().getRef());

        assertFalse(proxy.isCheckedOut());
        assertFalse(proxy.isVersion());
        assertTrue(proxy.isProxy());
        assertTrue(proxy.hasFacet(FacetNames.IMMUTABLE));
        assertTrue(proxy.isImmutable());
        assertEquals(versionSeriesId, proxy.getVersionSeriesId());
        assertEquals("0.1", proxy.getVersionLabel());
        assertEquals("comment", proxy.getCheckinComment());
        assertFalse(proxy.isMajorVersion());
        assertTrue(proxy.isLatestVersion());
        assertFalse(proxy.isLatestMajorVersion());
        assertFalse(proxy.isVersionSeriesCheckedOut());
        assertFalse(doc.isVersionSeriesCheckedOut());
        assertFalse(ver.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(),
                session.getWorkingCopy(proxy.getRef()).getId());

        /*
         * re-checked out doc
         */

        doc.checkOut();
        ver.refresh();
        proxy.refresh();

        assertTrue(doc.isCheckedOut());
        assertFalse(doc.isVersion());
        assertFalse(doc.isProxy());
        assertFalse(doc.hasFacet(FacetNames.IMMUTABLE));
        assertFalse(doc.isImmutable());
        assertEquals(versionSeriesId, doc.getVersionSeriesId());
        assertEquals("0.1+", doc.getVersionLabel());
        assertNull(doc.getCheckinComment());
        assertFalse(doc.isMajorVersion());
        assertFalse(doc.isLatestVersion());
        assertFalse(doc.isLatestMajorVersion());
        assertTrue(doc.isVersionSeriesCheckedOut());
        assertTrue(ver.isVersionSeriesCheckedOut());
        assertTrue(proxy.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(), session.getWorkingCopy(doc.getRef()).getId());

        /*
         * major checkin
         */

        DocumentRef majRef = doc.checkIn(VersioningOption.MAJOR, "yo");
        DocumentModel maj = session.getDocument(majRef);
        ver.refresh();
        proxy.refresh();

        assertTrue(maj.isMajorVersion());
        assertTrue(maj.isLatestVersion());
        assertTrue(maj.isLatestMajorVersion());
        assertFalse(maj.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(), session.getWorkingCopy(maj.getRef()).getId());
        // previous ver
        assertFalse(ver.isMajorVersion());
        assertFalse(ver.isLatestVersion());
        assertFalse(ver.isLatestMajorVersion());
        assertFalse(ver.isVersionSeriesCheckedOut());
        assertFalse(doc.isVersionSeriesCheckedOut());
        assertFalse(proxy.isVersionSeriesCheckedOut());
        assertEquals(doc.getId(), session.getWorkingCopy(ver.getRef()).getId());
    }

    public void testSaveRestoredVersionWithVersionAutoIncrement() throws ClientException {
            // check-in version 1.0, 2.0 and restore version 1.0
             DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
             doc = session.createDocument(doc);
             doc = session.saveDocument(doc);
             DocumentRef co = doc.getRef();
             DocumentRef ci1 = session.checkIn(co, VersioningOption.MAJOR, "first check-in" ); 
             session.checkOut(co);
             DocumentRef ci2 = session.checkIn(co,VersioningOption.MAJOR, "second check-in");
             session.restoreToVersion(co, ci1);
            
            // save document with auto-increment should produce version 3.0
            doc = session.getDocument(co);
            assertEquals(doc.getVersionLabel(), "1.0");
            doc.getContextData().putScopedValue(ScopeType.DEFAULT, VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
            doc.setPropertyValue("dc:title", doc.getPropertyValue("dc:title")); // mark as dirty
            doc = session.saveDocument(doc);
            assertEquals(doc.getVersionLabel(), "3.0");
    }
}
