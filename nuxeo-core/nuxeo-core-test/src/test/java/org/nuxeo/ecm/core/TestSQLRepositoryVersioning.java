/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyUpdateBeforeModificationListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml",
        "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-listener-beforemod-updatedoc-contrib.xml" })
public class TestSQLRepositoryVersioning {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    protected CoreSession openSessionAs(String username) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), username);
    }

    /**
     * Sleep 1s, useful for stupid databases (like MySQL) that don't have subsecond resolution in TIMESTAMP fields.
     */
    protected void maybeSleepToNextSecond() {
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();
    }

    protected void waitForFulltextIndexing() {
        nextTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Test
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

    @Test
    public void testRemoveSingleDocVersion() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(), "file#1", "File");
        file = session.createDocument(file);

        checkVersions(file);

        file.setPropertyValue("file:content", (Serializable) Blobs.createBlob("B", "text/plain", "UTF-8", "A"));
        file = session.saveDocument(file);
        file.checkIn(VersioningOption.MINOR, null);
        file.checkOut(); // to allow deleting last version

        checkVersions(file, "0.1");

        DocumentModel lastversion = session.getLastDocumentVersion(file.getRef());
        assertNotNull(lastversion);

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file);
    }

    // Creates 3 versions and removes the first.
    @Test
    public void testRemoveFirstDocVersion() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(), "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 0;
        DocumentModel firstversion = session.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(firstversion);

        assertTrue(firstversion.isVersion());
        session.removeDocument(firstversion.getRef());

        checkVersions(file, "0.2", "0.3");
    }

    // Creates 3 versions and removes the second.
    @Test
    public void testRemoveMiddleDocVersion() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(), "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 1;
        DocumentModel version = session.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(version);

        assertTrue(version.isVersion());
        session.removeDocument(version.getRef());

        checkVersions(file, "0.1", "0.3");
    }

    // Creates 3 versions and removes the last.
    @Test
    public void testRemoveLastDocVersion() throws Exception {
        DocumentModel folder = new DocumentModelImpl("/", "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(), "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 2;
        DocumentModel lastversion = session.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(lastversion);

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, "0.1", "0.2");
    }

    private void createTrioVersions(DocumentModel file) throws Exception {
        // create a first version
        file.setProperty("file", "content", new StringBlob("A"));
        file = session.saveDocument(file);
        file.checkIn(VersioningOption.MINOR, null);

        checkVersions(file, "0.1");

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "content", new StringBlob("B"));
        maybeSleepToNextSecond();
        file = session.saveDocument(file);
        file.checkIn(VersioningOption.MINOR, null);

        checkVersions(file, "0.1", "0.2");

        // create a third version
        file.setProperty("file", "content", new StringBlob("C"));
        maybeSleepToNextSecond();
        file = session.saveDocument(file);
        file.checkIn(VersioningOption.MINOR, null);
        file.checkOut(); // to allow deleting last version

        checkVersions(file, "0.1", "0.2", "0.3");
    }

    private void checkVersions(DocumentModel doc, String... labels) {
        List<String> actual = new LinkedList<String>();
        for (DocumentModel ver : session.getVersions(doc.getRef())) {
            assertTrue(ver.isVersion());
            actual.add(ver.getVersionLabel());
        }
        // build a debug list of versions and creation times
        // in case of failure
        StringBuilder buf = new StringBuilder("version time: ");
        for (VersionModel vm : session.getVersionsForDocument(doc.getRef())) {
            buf.append(vm.getLabel());
            buf.append("=");
            buf.append(vm.getCreated().getTimeInMillis());
            buf.append(", ");
        }
        buf.setLength(buf.length() - 2);
        assertEquals(buf.toString(), Arrays.asList(labels), actual);
        List<DocumentRef> versionsRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

    @Test
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

    @Test
    public void testAutoCheckOut() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "file", "File");
        doc.setPropertyValue("dc:title", "t0");
        doc = session.createDocument(doc);
        assertTrue(doc.isCheckedOut());
        session.checkIn(doc.getRef(), null, null);
        doc.refresh();
        assertFalse(doc.isCheckedOut());

        // auto-checkout
        doc.setPropertyValue("dc:title", "t1");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());

        session.checkIn(doc.getRef(), null, null);
        doc.refresh();
        assertFalse(doc.isCheckedOut());

        // disable auto-checkout
        doc.setPropertyValue("dc:title", "t2");
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("t2", doc.getPropertyValue("dc:title"));

        // can still be checked out normally afterwards
        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("t2", doc.getPropertyValue("dc:title"));
    }

    @Test
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

        doc.setProperty("dublincore", "title", "f1");
        doc.setProperty("dublincore", "description", "desc 1");
        session.saveDocument(doc);
        session.save();

        maybeSleepToNextSecond();
        DocumentRef v2Ref = session.checkIn(docRef, null, null);
        session.checkOut(docRef);

        DocumentModel newDoc = session.getDocument(docRef);
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("desc 1", newDoc.getProperty("dublincore", "description"));

        waitForFulltextIndexing();
        maybeSleepToNextSecond();
        DocumentModel restoredDoc = session.restoreToVersion(docRef, v1Ref);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("dublincore", "description"));

        waitForFulltextIndexing();
        maybeSleepToNextSecond();
        restoredDoc = session.restoreToVersion(docRef, v2Ref);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertEquals("desc 1", restoredDoc.getProperty("dublincore", "description"));
    }

    @Test
    public void testRestoreInvalidations() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        doc.setPropertyValue("dc:title", "t1");
        doc = session.createDocument(doc);
        final DocumentRef docRef = doc.getRef();
        DocumentRef v1 = session.checkIn(docRef, null, null);
        session.checkOut(docRef);
        doc.setPropertyValue("dc:title", "t2");
        session.saveDocument(doc);
        session.save();

        waitForFulltextIndexing();

        // we need 2 threads to get 2 different sessions that send each other invalidations
        final CyclicBarrier barrier = new CyclicBarrier(2);
        Throwable[] throwables = new Throwable[2];
        Thread t1 = new Thread() {
            @Override
            public void run() {
                TransactionHelper.startTransaction();
                try (CoreSession session = openSessionAs(SecurityConstants.ADMINISTRATOR)) {
                    DocumentModel doc = session.getDocument(docRef);
                    assertEquals("t2", doc.getPropertyValue("dc:title"));
                    // 1. sync
                    barrier.await(30, TimeUnit.SECONDS); // (throws on timeout)
                    // 2. restore and next tx to send invalidations
                    DocumentModel restored = session.restoreToVersion(docRef, v1);
                    assertEquals("t1", restored.getPropertyValue("dc:title"));
                    session.save();
                    nextTransaction();
                    // 3. sync
                    barrier.await(30, TimeUnit.SECONDS); // (throws on timeout)
                    // 4. wait
                } catch (InterruptedException | BrokenBarrierException | TimeoutException | RuntimeException
                        | AssertionError t) {
                    throwables[0] = t;
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                TransactionHelper.startTransaction();
                try (CoreSession session = openSessionAs(SecurityConstants.ADMINISTRATOR)) {
                    DocumentModel doc = session.getDocument(docRef);
                    assertEquals("t2", doc.getPropertyValue("dc:title"));
                    // 1. sync
                    barrier.await(30, TimeUnit.SECONDS); // (throws on timeout)
                    // 2. nop
                    // 3. sync
                    barrier.await(30, TimeUnit.SECONDS); // (throws on timeout)
                    // 4. next tx to get invalidations and check
                    nextTransaction();
                    DocumentModel restored = session.getDocument(docRef);
                    assertEquals("t1", restored.getPropertyValue("dc:title"));
                } catch (InterruptedException | BrokenBarrierException | TimeoutException | RuntimeException
                        | AssertionError t) {
                    throwables[1] = t;
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        AssertionError assertionError = null;
        for (Throwable t : throwables) {
            if (t != null) {
                if (assertionError == null) {
                    assertionError = new AssertionError("Exceptions in threads");
                }
                assertionError.addSuppressed(t);
            }
        }
        if (assertionError != null) {
            throw assertionError;
        }
    }

    @Test
    public void testGetDocumentWithVersion() throws Exception {
        String name2 = "file#248";
        DocumentModel childFile = new DocumentModelImpl("/", name2, "File");
        childFile = session.createDocument(childFile);
        session.save();
        DocumentRef v1Ref = session.checkIn(childFile.getRef(), null, null);
        session.checkOut(childFile.getRef());

        childFile.setProperty("dublincore", "title", "f1");
        childFile.setProperty("dublincore", "description", "desc 1");
        session.saveDocument(childFile);
        session.save();
        maybeSleepToNextSecond();
        DocumentRef v2Ref = session.checkIn(childFile.getRef(), null, null);

        DocumentModel newDoc = session.getDocument(childFile.getRef());
        assertNotNull(newDoc);
        assertNotNull(newDoc.getRef());
        assertEquals("desc 1", newDoc.getProperty("dublincore", "description"));

        // restore, no snapshot as already pristine
        waitForFulltextIndexing();
        maybeSleepToNextSecond();
        DocumentModel restoredDoc = session.restoreToVersion(childFile.getRef(), v1Ref);

        assertNotNull(restoredDoc);
        assertNotNull(restoredDoc.getRef());
        assertNull(restoredDoc.getProperty("dublincore", "description"));

        DocumentModel last = session.getLastDocumentVersion(childFile.getRef());
        assertNotNull(last);
        assertNotNull(last.getRef());
        assertEquals(v2Ref.reference(), last.getId());
        assertEquals("desc 1", last.getProperty("dublincore", "description"));
    }

    // security on versions, see TestLocalAPIWithCustomVersioning
    @Test
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
        try (CoreSession session2 = openSessionAs(SecurityConstants.ADMINISTRATOR)) {
            session2.getDocument(proxy.getRef());
        }
    }

    @Test
    public void testVersionRemoval() throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel file = session.createDocumentModel("/folder", "file", "File");
        file = session.createDocument(file);
        DocumentModel proxy = session.publishDocument(file, folder);
        DocumentModel version = session.getLastDocumentVersion(file.getRef());
        session.save();

        // even Administrator/system cannot remove a version with a proxy
        try {
            session.removeDocument(version.getRef());
            fail("Admin should not be able to remove version");
        } catch (DocumentSecurityException e) {
            // ok
        }

        // remove the proxy first
        session.removeDocument(proxy.getRef());
        session.save();
        // check out the working copy
        file.checkOut();
        // we can now remove the version
        session.removeDocument(version.getRef());
        session.save();
    }

    @Test
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

        doc = session.getDocument(new PathRef("/doc"));
        ver = session.getLastDocumentVersion(doc.getRef());
        assertEquals("approved", ver.getCurrentLifeCycleState());
    }

    @Test
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

    @Test
    public void testCopy() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();
        String versionSeriesId = doc.getVersionSeriesId();

        // copy
        DocumentModel copy = session.copy(doc.getRef(), session.getRootDocument().getRef(), "fileCopied");

        // check different version series id
        assertNotSame(versionSeriesId, copy.getVersionSeriesId());

        // create version and proxy
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel proxy = session.publishDocument(doc, folder);
        // check same version series id
        assertEquals(versionSeriesId, proxy.getVersionSeriesId());

        // copy proxy
        DocumentModel proxyCopy = session.copy(proxy.getRef(), session.getRootDocument().getRef(), "proxyCopied");
        // check same version series id
        assertEquals(versionSeriesId, proxyCopy.getVersionSeriesId());
    }

    @Test
    public void testCopyCheckedIn() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        doc.checkIn(VersioningOption.MAJOR, "comment");
        session.save();
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // copy
        DocumentModel copy = session.copy(doc.getRef(), session.getRootDocument().getRef(), "fileCopied");

        assertTrue(copy.isCheckedOut());
        assertEquals("0.0", copy.getVersionLabel());
    }

    @Test
    public void testVersionLabelOfCopy() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();
        DocumentModel copy = session.copy(doc.getRef(), new PathRef("/"), "copy");
        DocumentRef versionRef = session.checkIn(copy.getRef(), VersioningOption.MINOR, null);
        DocumentModel version = session.getDocument(versionRef);

        // check version label
        assertEquals("0.1", version.getVersionLabel());
    }

    @Test
    public void testPublishing() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
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
        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());
        assertEquals("0.1", lastVersionDocument.getVersionLabel());
    }

    @Test
    public void testPublishingAfterVersionDelete() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        checkVersions(doc);

        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNull(lastVersionDocument);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        checkVersions(doc, "0.1");
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());
        assertEquals("0.1", lastVersionDocument.getVersionLabel());

        // unpublish
        session.removeDocument(proxy.getRef());
        // delete the version
        List<VersionModel> versions = session.getVersionsForDocument(doc.getRef());
        assertEquals(1, versions.size());
        DocumentModel docVersion = session.getDocumentWithVersion(doc.getRef(), versions.get(0));
        // check out the working copy to remove the base version
        doc.checkOut();
        session.removeDocument(docVersion.getRef());

        checkVersions(doc);
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNull(lastVersionDocument);

        // republish
        DocumentModel newProxy = session.publishDocument(doc, folder);
        checkVersions(doc, "0.2");
        lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());
        assertEquals("0.2", lastVersionDocument.getVersionLabel());
    }

    @Test
    public void testPublishingAfterCopy() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        checkVersions(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        checkVersions(doc, "0.1");
        DocumentModel lastVersion = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getVersionLabel());
        DocumentModel lastVersionDocument = session.getLastDocumentVersion(doc.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("file", lastVersionDocument.getName());

        // copy published file, version is reset
        DocumentModel copy = session.copy(doc.getRef(), folder.getRef(), "fileCopied");
        checkVersions(copy);
        lastVersion = session.getLastDocumentVersion(copy.getRef());
        assertNull(lastVersion);
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNull(lastVersionDocument);

        // republish
        DocumentModel newProxy = session.publishDocument(copy, folder);
        checkVersions(copy, "0.1");
        lastVersion = session.getLastDocumentVersion(copy.getRef());
        assertNotNull(lastVersion);
        assertEquals("0.1", lastVersion.getVersionLabel());
        lastVersionDocument = session.getLastDocumentVersion(copy.getRef());
        assertNotNull(lastVersionDocument);
        assertEquals("fileCopied", lastVersionDocument.getName());
    }

    @Test
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

        DocumentModel proxy = session.createProxy(doc.getRef(), session.getRootDocument().getRef());

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
        assertEquals(doc.getId(), session.getWorkingCopy(proxy.getRef()).getId());

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
        assertEquals(doc.getId(), session.getWorkingCopy(proxy.getRef()).getId());

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

        proxy = session.createProxy(ver.getRef(), session.getRootDocument().getRef());

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
        assertEquals(doc.getId(), session.getWorkingCopy(proxy.getRef()).getId());

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

    @Test
    public void testSaveRestoredVersionWithVersionAutoIncrement() {
        // check-in version 1.0, 2.0 and restore version 1.0
        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        doc = session.createDocument(doc);
        doc = session.saveDocument(doc);
        DocumentRef co = doc.getRef();
        DocumentRef ci1 = session.checkIn(co, VersioningOption.MAJOR, "first check-in");
        session.checkOut(co);
        maybeSleepToNextSecond();
        DocumentRef ci2 = session.checkIn(co, VersioningOption.MAJOR, "second check-in");
        waitForFulltextIndexing();
        maybeSleepToNextSecond();
        session.restoreToVersion(co, ci1);

        // save document with auto-increment should produce version 3.0
        doc = session.getDocument(co);
        assertEquals(doc.getVersionLabel(), "1.0");
        doc.getContextData().putScopedValue(ScopeType.DEFAULT, VersioningService.VERSIONING_OPTION,
                VersioningOption.MAJOR);
        // mark as dirty - must change the value
        doc.setPropertyValue("dc:title", doc.getPropertyValue("dc:title") + " dirty");
        doc = session.saveDocument(doc);
        assertEquals(doc.getVersionLabel(), "3.0");
    }

    @Test
    public void testAllowVersionWrite() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("icon", "icon1");
        doc = session.createDocument(doc);
        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);

        // regular version cannot be written
        DocumentModel ver = session.getDocument(verRef);
        ver.setPropertyValue("icon", "icon2");
        try {
            session.saveDocument(ver);
            fail("Should not allow version write");
        } catch (PropertyException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Cannot set property on a version"));
        }

        // with proper option, it's allowed
        ver.setPropertyValue("icon", "icon3");
        ver.putContextData(CoreSession.ALLOW_VERSION_WRITE, Boolean.TRUE);
        session.saveDocument(ver);
        // refetch to check
        ver = session.getDocument(verRef);
        assertEquals("icon3", ver.getPropertyValue("icon"));
    }

    @Test
    public void testAllowVersionWriteACL() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);
        DocumentModel ver = session.getDocument(verRef);
        ACL acl = new ACLImpl("acl1", false);
        ACE ace = new ACE("princ1", "perm1", true);
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);
        // check that ACP can be set
        ver.setACP(acp, true);
    }

    @Test
    public void testGetLastVersion() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();
        DocumentRef v1ref = session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        session.checkOut(doc.getRef());
        maybeSleepToNextSecond();
        DocumentRef v2ref = session.checkIn(doc.getRef(), VersioningOption.MINOR, null);

        // last version on the doc
        DocumentModel last = session.getLastDocumentVersion(doc.getRef());
        assertEquals(v2ref.reference(), last.getId());
        DocumentRef lastRef = session.getLastDocumentVersionRef(doc.getRef());
        assertEquals(v2ref.reference(), lastRef.reference());

        // last version on any version
        last = session.getLastDocumentVersion(v2ref);
        assertEquals(v2ref.reference(), last.getId());
        lastRef = session.getLastDocumentVersionRef(v2ref);
        assertEquals(v2ref.reference(), lastRef.reference());
    }

    @Test
    public void testGetVersions() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();
        DocumentRef v1ref = session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        session.checkOut(doc.getRef());
        session.checkIn(doc.getRef(), VersioningOption.MINOR, null);

        // versions on the doc
        List<DocumentModel> vers = session.getVersions(doc.getRef());
        assertEquals(2, vers.size());
        List<DocumentRef> verRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(2, verRefs.size());

        // versions on any version
        vers = session.getVersions(v1ref);
        assertEquals(2, vers.size());
        verRefs = session.getVersionsRefs(v1ref);
        assertEquals(2, verRefs.size());
    }

    @Test
    public void testSearchVersionWithNoLiveDocument() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();
        // create a version
        DocumentRef vref = session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        // create a proxy
        session.createProxy(vref, new PathRef("/"));
        // remove the live doc, the version is not removed because of the proxy
        session.removeDocument(doc.getRef());
        session.save();
        // now search as non-admin
        try (CoreSession bobSession = openSessionAs("bob")) {
            // if this returns then all is well, otherwise it means there's an infinite loop somewhere
            bobSession.query("SELECT * FROM Document");
        }
    }

    @Test
    public void testRemoveLiveProxyTarget() {
        DocumentModel fold = session.createDocumentModel("/", "fold", "Folder");
        fold = session.createDocument(fold);
        DocumentModel doc = session.createDocumentModel("/fold", "doc", "File");
        doc = session.createDocument(doc);
        // create a live proxy to the doc
        // put proxy in same folder so that we can remove both at once
        session.createProxy(doc.getRef(), fold.getRef());
        session.save();
        // remove the folder, containing the doc which is a proxy target
        session.removeDocument(fold.getRef());
        session.save();
    }

    @Test
    public void testDirtyStateBehaviours() throws Exception {
        // given a created doc with a given version
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.checkIn(doc.getRef(), VersioningOption.MAJOR, "Increment major version");
        doc = session.getDocument(doc.getRef());
        String v1 = doc.getVersionLabel();

        // when I update the doc with no change
        doc = session.saveDocument(doc);
        // then the version doesnt change
        doc = session.getDocument(doc.getRef());
        assertEquals(v1, doc.getVersionLabel());

        // when I update the doc with changes
        doc.setPropertyValue("dc:title", "coucou");
        doc = session.saveDocument(doc);
        // then the version change
        doc = session.getDocument(doc.getRef());
        assertNotEquals(v1, doc.getVersionLabel());

        // reset the version
        session.checkIn(doc.getRef(), VersioningOption.MAJOR, "Increment major version");
        doc = session.getDocument(doc.getRef());
        String v3 = doc.getVersionLabel();

        // when I update the doc though a event listener
        doc.getContextData().putScopedValue(ScopeType.DEFAULT,
                DummyUpdateBeforeModificationListener.PERDORM_UPDATE_FLAG, true);
        doc = session.saveDocument(doc);
        // then the version change
        doc = session.getDocument(doc.getRef());
        assertNotEquals(v3, doc.getVersionLabel());
    }

}
