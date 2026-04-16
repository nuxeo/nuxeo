/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedBefore;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedNotContains;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertIndexedSince;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.assertNotIndexed;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Timestamp;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.security.RetentionExpiredFinderListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
public class TestSearchIndexing {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected TrashService trashService;

    @Test
    public void shouldNotIndexRootDocument() {
        // update acp on root document as it is reinit during tear down
        DocumentModel root = session.getRootDocument();
        assertNotIndexed(root.getId());

        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acp.addACL(acl);
        root.setACP(acp, true);

        txFeature.nextTransaction();
        assertNotIndexed(root.getId());
    }

    @Test
    public void shouldIndexDocument() {
        List<String> docs = new ArrayList<>();
        // create docs
        for (int i = 0; i < 4; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
            docs.add(doc.getId());
        }
        final long t1 = System.currentTimeMillis();
        txFeature.nextTransaction();
        docs.forEach(doc -> assertIndexedSince(doc, t1));

        List<String> updatedDocs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
            updatedDocs.add(doc.getId());
        }
        final long t2 = System.currentTimeMillis();
        txFeature.nextTransaction();
        updatedDocs.forEach(doc -> assertIndexedSince(doc, t2));
    }

    @Test
    public void shouldIndexImportedDocument() {
        // import one doc
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        ((DocumentModelImpl) doc).setId(UUID.randomUUID().toString());
        doc.setPropertyValue("dc:title", "TestMe");
        session.importDocuments(Collections.singletonList(doc));
        final long t1 = System.currentTimeMillis();
        assertNotIndexed(doc.getId());
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t1);
    }

    @Test
    public void shouldNotIndexDocumentBecauseOfRollback() {
        List<String> docs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
            docs.add(doc.getId());
        }
        // Save session to prevent NXP-14494
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        txFeature.nextTransaction();
        docs.forEach(BaseCoreSearchFeature::assertNotIndexed);
    }

    @Test
    public void shouldUnIndexDocument() {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);
        final long t1 = System.currentTimeMillis();
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t1);

        // now delete the document
        session.removeDocument(doc.getRef());
        txFeature.nextTransaction();
        assertNotIndexed(doc.getId());
    }

    @Test
    public void shouldReIndexDocument() {
        List<String> docs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc.setPropertyValue("dc:nature", "A");
            doc = session.createDocument(doc);
            docs.add(doc.getId());
        }
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        docs.forEach(doc -> assertIndexedSince(doc, t1));

        List<String> updatedDocs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DocumentModel doc = session.getDocument(new IdRef(docs.get(i)));
            doc.setPropertyValue("dc:nature", "B");
            session.saveDocument(doc);
            updatedDocs.add(doc.getId());
            docs.remove(doc.getId());
        }
        final long t2 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        updatedDocs.forEach(doc -> assertIndexedSince(doc, t2));
        docs.forEach(doc -> assertIndexedBefore(doc, t2));
    }

    @Test
    public void shouldIndexBinaryFulltext() {
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        doc = session.createDocument(doc);
        session.save();
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t1);
        assertIndexedContains(doc.getId(), "You know for search");
    }

    @Test
    public void shouldIndexLargeBinaryFulltext() {
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        // lucene don't allow term > 32k so use 20k (all db backend don't support either > 32k field)
        holder.setBlob(new StringBlob("search " + createBigString(20000, 'a') + " foo"));
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "aaaa foo");
    }

    @Test
    public void shouldIndexLargeToken() {
        assumeTrue("DB backend needs to support fields bigger than 32k",
                coreFeature.getStorageConfiguration().isVCSH2());
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "search " + createBigString(40000, 'b') + " bar");
        // term > 32k cannot be indexed by lucene
        // but es discard them with the ignore_above and the with the custom tokenizer
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "search bbbb");
        assertIndexedContains(doc.getId(), "bbbb bar");
    }

    @Test
    public void shouldIndexOnPublishing() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        String versionId = proxy.getSourceId();
        assertIndexedSince(proxy.getId(), t1);
        assertIndexedSince(versionId, t1);
        assertIndexedSince(doc.getId(), t1);

        // unpublish
        session.removeDocument(proxy.getRef());
        final long t2 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();

        // no more proxy
        assertNotIndexed(proxy.getId());
        // no change on doc and version
        assertIndexedBefore(versionId, t2);
        assertIndexedBefore(doc.getId(), t2);
    }

    @Test
    public void shouldIndexOnRePublishing() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:description", "foo");
        doc = session.createDocument(doc);
        DocumentModel proxy = session.publishDocument(doc, folder);

        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "foo");
        assertIndexedContains(proxy.getId(), "foo");

        doc.setPropertyValue("dc:description", "bar");
        session.saveDocument(doc);
        session.publishDocument(doc, folder);
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "bar");
        assertIndexedContains(proxy.getId(), "bar");
    }

    @Test
    public void shouldUnIndexUsingTrashService() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        shouldUnIndexUsingTrashService(folder, doc);
    }

    @Test
    public void shouldUnIndexUsingTrashServiceWithoutRenaming() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        doc.putContextData(TrashService.DISABLE_TRASH_RENAMING, Boolean.TRUE);
        shouldUnIndexUsingTrashService(folder, doc);
    }

    protected void shouldUnIndexUsingTrashService(DocumentModel folder, DocumentModel doc) {
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(folder.getId(), t1);
        assertIndexedSince(doc.getId(), t1);

        trashService.trashDocument(doc);

        final long t2 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t2);
        assertIndexedContains(folder.getId(), "\"ecm:isTrashed\":false");
        assertIndexedContains(doc.getId(), "\"ecm:isTrashed\":true");

        doc = session.getDocument(doc.getRef());
        trashService.untrashDocument(doc);

        final long t3 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(doc.getId(), t3);

        assertIndexedContains(folder.getId(), "\"ecm:isTrashed\":false");
        assertIndexedContains(doc.getId(), "\"ecm:isTrashed\":false");

        trashService.purgeDocuments(session, Collections.singletonList(doc.getRef()));

        txFeature.nextTransaction();
        assertNotIndexed(doc.getId());
    }

    @Test
    public void shouldIndexOnCopy() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        DocumentRef src = doc.getRef();
        DocumentRef dst = new PathRef("/");
        DocumentModel dstDoc = session.copy(src, dst, "file2");
        session.save();
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();

        assertIndexedSince(dstDoc.getId(), t1);
        // no update on doc
        assertIndexedBefore(doc.getId(), t1);
    }

    @Test
    public void shouldHandleCreateDelete() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel(folder.getPathAsString(), "note", "Note");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        // we don't wait for async
        TransactionHelper.startTransaction();
        session.removeDocument(folder.getRef());
        txFeature.nextTransaction();
        assertNotIndexed(folder.getId());
        assertNotIndexed(doc.getId());
    }

    @Test
    public void shouldHandleUpdateOnTransientDoc() {
        DocumentModel tmpDoc = session.createDocumentModel("/", "file", "File");
        tmpDoc.setPropertyValue("dc:title", "TestMe");
        DocumentModel doc = session.createDocument(tmpDoc); // Send an ES_INSERT cmd
        session.saveDocument(doc); // Send an ES_UPDATE merged with ES_INSERT
        txFeature.nextTransaction();

        // here we manipulate the transient doc with a null docId
        assertNull(tmpDoc.getId());
        tmpDoc.setPropertyValue("dc:title", "NewTitle");
        tmpDoc = session.saveDocument(tmpDoc);
        txFeature.nextTransaction();

        assertIndexedContains(tmpDoc.getId(), "NewTitle");
    }

    @Test
    public void shouldHandleUpdateOnTransientDocBis() {
        DocumentModel tmpDoc = session.createDocumentModel("/", "file", "File");
        tmpDoc.setPropertyValue("dc:title", "TestMe");
        DocumentModel doc = session.createDocument(tmpDoc); // Send an ES_INSERT cmd
        session.saveDocument(doc); // Send an ES_UPDATE merged with ES_INSERT

        tmpDoc.setPropertyValue("dc:title", "NewTitle"); // ES_UPDATE with transient, merged
        tmpDoc = session.saveDocument(tmpDoc);

        txFeature.nextTransaction();
        assertIndexedContains(tmpDoc.getId(), "NewTitle");
    }

    @Test
    public void shouldHandleUpdateBeforeInsertOnTransientDoc() {
        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        session.createDocument(folder);
        folder = session.saveDocument(folder); // generate a WARN and an UPDATE command
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(folder.getId(), t1);
    }

    @Test
    public void shouldIndexOrderedFolder() {
        DocumentModel ofolder = session.createDocumentModel("/", "ofolder", "OrderedFolder");
        ofolder = session.createDocument(ofolder);
        DocumentModel file1 = session.createDocumentModel("/ofolder", "testfile1", "File");
        file1 = session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/ofolder", "testfile2", "File");
        file2 = session.createDocument(file2);
        DocumentModel file3 = session.createDocumentModel("/ofolder", "testfile3", "File");
        file3 = session.createDocument(file3);
        DocumentModel folder4 = session.createDocumentModel("/ofolder", "folder4", "Folder");
        folder4 = session.createDocument(folder4);
        DocumentModel fileInSubfolder = session.createDocumentModel("/ofolder/folder4", "testfile", "File");
        fileInSubfolder = session.createDocument(fileInSubfolder);

        txFeature.nextTransaction();
        assertIndexedContains(file1.getId(), "\"ecm:pos\":0");
        assertIndexedContains(file2.getId(), "\"ecm:pos\":1");
        assertIndexedContains(file3.getId(), "\"ecm:pos\":2");
        assertIndexedContains(folder4.getId(), "\"ecm:pos\":3");
        final long t1 = Timestamp.currentTimeMicros();
        session.orderBefore(ofolder.getRef(), "testfile3", "testfile2");
        txFeature.nextTransaction();
        assertIndexedContains(file1.getId(), "\"ecm:pos\":0");
        assertIndexedContains(file2.getId(), "\"ecm:pos\":2");
        assertIndexedContains(file3.getId(), "\"ecm:pos\":1");
        // this one was not reindexed
        assertIndexedBefore(fileInSubfolder.getId(), t1);
    }

    @Test
    public void shouldNotIndexRecursivelyVersionFolder() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel file1 = session.createDocumentModel("/folder", "testfile1", "File");
        file1 = session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/folder", "testfile2", "File");
        file2 = session.createDocument(file2);

        folder.setPropertyValue("dc:title", "v1");
        folder = session.saveDocument(folder);
        DocumentRef v1 = folder.checkIn(VersioningOption.MAJOR, "init");

        folder.setPropertyValue("dc:title", "v2");
        folder = session.saveDocument(folder);
        DocumentRef v2 = folder.checkIn(VersioningOption.MAJOR, "update");
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        // 3 docs (2 files + 1 folder checkout) + 2 versions of folder
        assertIndexedSince(folder.getId(), t1);
        assertIndexedSince(file1.getId(), t1);
        assertIndexedSince(file2.getId(), t1);
        assertIndexedSince((String) v1.reference(), t1);
        assertIndexedSince((String) v2.reference(), t1);

        // delete the first version
        session.removeDocument(v1);
        txFeature.nextTransaction();
        assertNotIndexed((String) v1.reference());
    }

    @Test
    public void shouldIndexLatestVersions() {
        final long t1 = Timestamp.currentTimeMicros();
        List<String> ids = createADocumentWith3Versions();
        ids.forEach(id -> assertIndexedSince(id, t1));

        String v3 = ids.getLast();
        assertIndexedContains(v3, "\"ecm:isLatestVersion\":true");
        assertIndexedContains(v3, "\"ecm:isLatestMajorVersion\":true");
        String v2 = ids.get(ids.size() - 2);
        assertIndexedContains(v2, "\"ecm:isLatestVersion\":false");
        assertIndexedContains(v2, "\"ecm:isLatestMajorVersion\":false");
        String v1 = ids.get(ids.size() - 3);
        assertIndexedContains(v1, "\"ecm:isLatestVersion\":false");
        assertIndexedContains(v1, "\"ecm:isLatestMajorVersion\":false");
    }

    /**
     * This test should be disabled now that we have an efficient way to reindex previous latest versions
     */
    @Test
    public void shouldNotIndexLatestVersions() {
        System.setProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY, "true");
        List<String> ids;
        try {
            ids = createADocumentWith3Versions();
        } finally {
            System.clearProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY);
        }
        // isLatestVersion and isLatestMajorVersion are not updated
        String v3 = ids.getLast();
        assertIndexedContains(v3, "\"ecm:isLatestVersion\":true");
        assertIndexedContains(v3, "\"ecm:isLatestMajorVersion\":true");
        String v2 = ids.get(ids.size() - 2);
        assertIndexedContains(v2, "\"ecm:isLatestVersion\":true");
        assertIndexedContains(v2, "\"ecm:isLatestMajorVersion\":true");
        String v1 = ids.get(ids.size() - 3);
        assertIndexedContains(v1, "\"ecm:isLatestVersion\":true");
        assertIndexedContains(v1, "\"ecm:isLatestMajorVersion\":true");
    }

    /*
     * NXP-23033
     */
    @Test
    public void shouldIndexAfterVersionRestored() {
        List<String> ids = createADocumentWith3Versions();
        String doc = ids.getFirst();
        String v3 = ids.getLast();
        assertIndexedContains(v3, "\"ecm:isLatestVersion\":true");
        assertIndexedContains(v3, "\"ecm:isLatestMajorVersion\":true");
        String v2 = ids.get(ids.size() - 2);
        assertIndexedContains(v2, "\"ecm:isLatestVersion\":false");
        assertIndexedContains(v2, "\"ecm:isLatestMajorVersion\":false");
        // restore the document to v2 and check version in ES
        VersionModel v2VM = new VersionModelImpl();
        v2VM.setLabel("2.0");
        DocumentModel v2Doc = session.getDocumentWithVersion(new IdRef(doc), v2VM);
        session.restoreToVersion(new IdRef(doc), v2Doc.getRef());
        final long t1 = Timestamp.currentTimeMicros();
        txFeature.nextTransaction();
        assertIndexedSince(doc, t1);
    }

    /*
     * NXP-23033
     */
    @Test
    public void shouldIndexAfterPublishThenRestore() {
        // create a document
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "v0.1");
        doc = session.saveDocument(doc);
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "v0.1");

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        assertEquals("0.1", proxy.getVersionLabel());
        txFeature.nextTransaction();

        // update document and version it
        doc.setPropertyValue("dc:title", "v0.2");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "v0.2");
        assertIndexedNotContains(doc.getId(), "v0.1");

        // restore document to 0.1
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setLabel("0.1");
        DocumentModel v1 = session.getDocumentWithVersion(doc.getRef(), versionModel);
        session.restoreToVersion(doc.getRef(), v1.getRef());
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "v0.1");
        assertIndexedNotContains(doc.getId(), "v0.2");
    }

    @Test
    public void shouldIndexUpdatedProxy() {
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);
        folder1 = session.saveDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        file1.setPropertyValue("dc:title", "Title before proxy update");
        file1 = session.saveDocument(file1);
        txFeature.nextTransaction();

        // Create proxy
        DocumentModel proxy = session.createProxy(file1.getRef(), folder1.getRef());
        proxy = session.saveDocument(proxy);
        txFeature.nextTransaction();

        // Now update it
        proxy.setPropertyValue("dc:title", "Title after proxy update");
        proxy = session.saveDocument(proxy);
        txFeature.nextTransaction();

        // Check that proxy was indexed
        assertIndexedContains(proxy.getId(), "Title after proxy update");
        // Check that live document was updated
        assertIndexedContains(file1.getId(), "Title after proxy update");
    }

    // NXP-30219
    @Test
    public void shouldIndexUpdatedProxyAfterDocumentTrashed() {
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        txFeature.nextTransaction();

        // Create proxy
        DocumentModel proxy = session.createProxy(file1.getRef(), folder1.getRef());
        txFeature.nextTransaction();

        // Now trash live document
        trashService.trashDocument(file1);
        txFeature.nextTransaction();

        // Check that live document was updated
        assertIndexedContains(file1.getId(), "\"ecm:isTrashed\":true");

        // Check that proxy was updated
        assertIndexedContains(proxy.getId(), "\"ecm:isTrashed\":true");
    }

    // NXP-31007
    @Test
    public void shouldIndexProxyAfterVersionUpdate() {
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1.setPropertyValue("dc:description", "An old description");
        file1 = session.createDocument(file1);
        txFeature.nextTransaction();
        // Publish the document
        DocumentModel proxy = session.publishDocument(file1, folder1);
        txFeature.nextTransaction();
        // Update the version
        DocumentModel version = session.getLastDocumentVersion(file1.getRef());
        version.setPropertyValue("dc:description", "A new description");
        version.putContextData(CoreSession.ALLOW_VERSION_WRITE, Boolean.TRUE);
        session.saveDocument(version);
        txFeature.nextTransaction();

        // Check the proxy is indexed
        assertIndexedContains(proxy.getId(), "A new description");
    }

    @Test
    public void shouldIndexComplexCase() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        ACP acp = new ACPImpl();
        ACL acl = ACPImpl.newACL(ACL.LOCAL_ACL);
        acl.add(new ACE("bob", SecurityConstants.READ, true));
        acp.addACL(acl);
        folder.setACP(acp, true);

        DocumentModel doc = session.createDocumentModel("/folder", "file", "File");
        doc.setPropertyValue("dc:title", "File");
        // upload file blob
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("OSGI-INF/search/blob.json")) {
            Blob fb = Blobs.createBlob(is, "image/jpeg");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        doc = session.createDocument(doc);
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "image/jpeg");
    }

    @Test
    public void pathLevelFieldMustBeSeenAsKeyword() {
        // Creates folders with names that can be taken as timestamp
        DocumentModel folder = session.createDocumentModel("/", "1530083790734003", "Folder");
        session.createDocument(folder);
        folder = session.createDocumentModel("/1530083790734003", "1530083790734004", "Folder");
        session.createDocument(folder);
        txFeature.nextTransaction();
        // Now creates folders with normal names to check that ecm:path@level# fields are typed as keyword and not as
        // date
        folder = session.createDocumentModel("/", "a-folder-name", "Folder");
        session.createDocument(folder);
        folder = session.createDocumentModel("/a-folder-name", "foo", "Folder");
        session.createDocument(folder);
        txFeature.nextTransaction();
    }

    @Test
    public void shouldIndexUpdatedRecord() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();
        // not a record
        assertIndexedNotContains(doc.getId(), "\"ecm:isRecord\":true");
        // make the doc a record
        session.makeRecord(doc.getRef());
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "\"ecm:isRecord\":true");
    }

    @Test
    public void shouldIndexUpdatedRetention() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        // set retention to few seconds in the future
        Calendar fewSeconds = Calendar.getInstance();
        fewSeconds.add(Calendar.SECOND, 3);
        session.makeRecord(doc.getRef());
        session.setRetainUntil(doc.getRef(), fewSeconds, null);
        session.save();
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "\"ecm:isRecord\":true");
        assertIndexedContains(doc.getId(), "\"ecm:retainUntil\"");
        // wait more to pass retention expiration date
        Thread.sleep(4_000);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        // wait for all bulk commands to be executed
        TransactionHelper.commitOrRollbackTransaction();
        BulkService bulkService = Framework.getService(BulkService.class);
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        txFeature.nextTransaction();
        // no more retainUntil in index
        assertIndexedNotContains(doc.getId(), "\"ecm:retainUntil\"");
    }

    @Test
    public void shouldIndexUpdatedLegalHold() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();
        assertIndexedNotContains(doc.getId(), "\"ecm:hasLegalHold\":true");

        // set legal hold
        session.makeRecord(doc.getRef());
        session.setLegalHold(doc.getRef(), true, null);
        txFeature.nextTransaction();
        assertIndexedContains(doc.getId(), "\"ecm:hasLegalHold\":true");

        // remove legal hold
        session.setLegalHold(doc.getRef(), false, null);
        txFeature.nextTransaction();
        assertIndexedNotContains(doc.getId(), "\"ecm:hasLegalHold\":true");
    }

    // ----------------------------------------------------------------
    // Helpers

    /**
     * Returns document ids: doc, v1, v2, v3
     */
    protected List<String> createADocumentWith3Versions() {
        List<String> ids = new ArrayList<>();
        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        ids.add(file1.getId());
        file1.setPropertyValue("dc:title", "v1");
        file1 = session.saveDocument(file1);
        DocumentRef ref = file1.checkIn(VersioningOption.MAJOR, "init v1");
        ids.add((String) ref.reference());
        txFeature.nextTransaction();

        file1.setPropertyValue("dc:title", "v2");
        file1 = session.saveDocument(file1);
        ref = file1.checkIn(VersioningOption.MAJOR, "update v2");
        ids.add((String) ref.reference());
        txFeature.nextTransaction();

        file1.setPropertyValue("dc:title", "v3");
        file1 = session.saveDocument(file1);
        ref = file1.checkIn(VersioningOption.MAJOR, "update v3");
        ids.add((String) ref.reference());
        txFeature.nextTransaction();
        return ids;
    }

    protected String createBigString(int length, char character) {
        return new String(new char[length]).replace('\0', character);
    }

    /**
     * @since 2025.19
     */
    @Test
    public void shouldNotIndexRelationByDefault() {
        var doc = session.createDocumentModel(null, "myRelation", "Relation");
        doc.setPropertyValue("dc:title", "A relation");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();
        assertNotIndexed(docId);
    }

    /**
     * @since 2025.19
     */
    @Test
    @WithFrameworkProperty(name = "nuxeo.search.indexing.relation.enabled", value = "true")
    public void shouldIndexRelationWhenEnabled() {
        var doc = session.createDocumentModel(null, "myRelation", "Relation");
        doc.setPropertyValue("dc:title", "A relation");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();
        assertIndexedContains(docId, "A relation");
    }
}
