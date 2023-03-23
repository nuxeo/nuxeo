/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.tag.FacetedTagService;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test "on the fly" indexing via the listener system
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
// @WithFrameworkProperty(name = RECURSIVE_INDEXING_USING_BULK_SERVICE_PROPERTY, value = "true")
public class TestAutomaticIndexing {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected BulkService bulk;

    @Inject
    protected TrashService trashService;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected TagService tagService;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected BulkService bulkService;

    @Inject
    ElasticSearchAdmin esa;

    private boolean syncMode = false;

    private int commandProcessed;

    // Number of processed command since the startTransaction
    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    /**
     * Wait for async worker completion then wait for indexing completion
     */
    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        bulk.await(Duration.ofSeconds(20));
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    public void activateSynchronousMode() throws Exception {
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        syncMode = true;
    }

    @After
    public void restoreAsyncAndConsoleLog() {
        ElasticSearchInlineListener.useSyncIndexing.set(false);
        syncMode = false;
        logFeature.restoreConsoleLog();
    }

    protected void startTransaction() {
        if (syncMode) {
            ElasticSearchInlineListener.useSyncIndexing.set(true);
        }
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void shouldNotIndexRootDocument() throws Exception {
        startTransaction();
        // update acp on root document as it is reinit during tear down
        DocumentModel root = session.getRootDocument();
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("Administrator", "Everything", true));
        acp.addACL(acl);
        root.setACP(acp, true);

        // check no indexing was performed
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(0);

        // check no root document from search response
        startTransaction();
        SearchResponse searchResponse = searchAll();
        assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    protected SearchResponse searchAll() {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        return esa.getClient().search(request);
    }

    protected SearchResponse search(QueryBuilder query) {
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        request.source(new SearchSourceBuilder().query(query));
        return esa.getClient().search(request);
    }

    @Test
    public void shouldNotIndexRootDocumentDuringReindexAll() throws Exception {
        startTransaction();
        // Re-index all repositories
        for (String repositoryName : esa.getRepositoryNames()) {
            esa.dropAndInitRepositoryIndex(repositoryName);
            esi.runReindexingWorker(repositoryName, "SELECT ecm:uuid FROM Document");
        }

        // check no indexing was performed
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(0);

        // check no root document from search response
        startTransaction();
        SearchResponse searchResponse = searchAll();
        assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexDocument() throws Exception {
        startTransaction();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        // merge 5
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        SearchResponse searchResponse = searchAll();
        assertEquals(10, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexImportedDocument() throws Exception {
        startTransaction();
        // import one doc
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        ((DocumentModelImpl) doc).setId(UUID.randomUUID().toString());
        doc.setPropertyValue("dc:title", "TestMe");
        session.importDocuments(Collections.singletonList(doc));

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        SearchResponse searchResponse = esa.getClient().search(request);
        assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldNotIndexDocumentBecauseOfRollback() throws Exception {
        startTransaction();
        // create 10 docs
        activateSynchronousMode();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        // Save session to prevent NXP-14494
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(0);

        startTransaction();
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        SearchResponse searchResponse = esa.getClient().search(request);

        assertEquals(0, searchResponse.getHits().getTotalHits());
        Assert.assertFalse(esa.isIndexingInProgress());
    }

    @Test
    public void shouldUnIndexDocument() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        SearchResponse searchResponse = esa.getClient().search(request);
        assertEquals(1, searchResponse.getHits().getTotalHits());

        // now delete the document
        session.removeDocument(doc.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().search(request);
        assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldReIndexDocument() throws Exception {
        startTransaction();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc.setPropertyValue("dc:nature", "A");
            doc = session.createDocument(doc);

        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(10);

        startTransaction();

        SearchResponse searchResponse = search(QueryBuilders.matchQuery("dc:nature", "A"));
        assertEquals(10, searchResponse.getHits().getTotalHits());

        int i = 0;
        for (SearchHit hit : searchResponse.getHits()) {
            i++;
            if (i > 8) {
                break;
            }
            DocumentModel doc = session.getDocument(new IdRef(hit.getId()));
            doc.setPropertyValue("dc:nature", "B");
            session.saveDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(8);

        startTransaction();
        searchResponse = search(QueryBuilders.matchQuery("dc:nature", "A"));
        assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = search(QueryBuilders.matchQuery("dc:nature", "B"));
        assertEquals(8, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexBinaryFulltext() throws Exception {
        startTransaction();
        activateSynchronousMode(); // this is to prevent race condition that happen NXP-16169
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        // we need to wait for the async fulltext indexing
        WorkManager wm = Framework.getService(WorkManager.class);
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext='search'"));
        assertEquals(1, ret.totalSize());
    }

    @Test
    public void shouldIndexLargeBinaryFulltext() throws Exception {
        startTransaction();
        activateSynchronousMode(); // this is to prevent race condition that happen NXP-16169
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        // lucene don't allow term > 32k so use 20k (all db backend don't support either > 32k field)
        holder.setBlob(new StringBlob("search " + createBigString(20000, 'a') + "  foo"));
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext='search'"));
        assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext='foo'"));
        assertEquals(1, ret.totalSize());
    }

    protected String createBigString(int length, char c) {
        return new String(new char[length]).replace('\0', c);
    }

    @Test
    public void shouldIndexLargeToken() throws Exception {
        assumeTrue("DB backend needs to support fields bigger than 32k",
                coreFeature.getStorageConfiguration().isVCSH2());

        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "search " + createBigString(40000, 'a') + " bar");
        // term > 32k cannot be indexed by lucene
        // but es discard them with the ignore_above and the with the custom tokenizer
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'search'"));
        assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'bar'"));
        assertEquals(1, ret.totalSize());

    }

    @Test
    public void shouldIndexOnPublishing() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(4);

        startTransaction();
        SearchResponse searchResponse = searchAll();
        // folder, version, file and proxy
        assertEquals(4, searchResponse.getHits().getTotalHits());

        // unpublish
        session.removeDocument(proxy.getRef());
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(4, docs.totalSize());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = searchAll();
        assertEquals(3, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldIndexOnRePublishing() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:description", "foo");
        doc = session.createDocument(doc);
        session.publishDocument(doc, folder);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext = 'foo' AND ecm:isVersion = 0"));
        assertEquals(2, docs.totalSize());

        doc.setPropertyValue("dc:description", "bar");
        session.saveDocument(doc);
        session.publishDocument(doc, folder);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        docs = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext = 'bar' AND ecm:isVersion = 0"));
        assertEquals(2, docs.totalSize());
    }

    @Test
    public void shouldUnIndexUsingTrashService() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        shouldUnIndexUsingTrashService(doc);
    }

    @Test
    public void shouldUnIndexUsingTrashServiceWithoutRenaming() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        doc.putContextData(TrashService.DISABLE_TRASH_RENAMING, Boolean.TRUE);
        shouldUnIndexUsingTrashService(doc);
    }

    protected void shouldUnIndexUsingTrashService(DocumentModel doc) throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(2); // 2 creations

        startTransaction();
        trashService.trashDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // 1 update

        startTransaction();
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isTrashed = 0"));
        assertEquals(1, ret.totalSize());
        doc = session.getDocument(doc.getRef());
        trashService.untrashDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isTrashed = 0"));
        assertEquals(2, ret.totalSize());

        SearchResponse searchResponse = searchAll();
        assertEquals(2, searchResponse.getHits().getTotalHits());

        trashService.purgeDocuments(session, Collections.singletonList(doc.getRef()));

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = searchAll();
        assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexOnCopy() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(2);

        startTransaction();
        DocumentRef src = doc.getRef();
        DocumentRef dst = new PathRef("/");
        session.copy(src, dst, "file2");
        // turn the sync flag after the action
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        SearchResponse searchResponse = searchAll();
        assertEquals(3, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexTag() throws Exception {

        boolean facetedTags = tagService instanceof FacetedTagService;
        assumeTrue("DBS does not support tags based on SQL relations",
                !coreFeature.getStorageConfiguration().isDBS() || facetedTags);

        // ElasticSearchInlineListener.useSyncIndexing.set(true);
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        tagService.tag(session, doc.getId(), "mytag");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        ElasticSearchInlineListener.useSyncIndexing.set(true);

        assertNumberOfCommandProcessed(facetedTags ? 1 : 3); // doc, tagging relation and tag

        startTransaction();
        SearchResponse searchResponse = search(QueryBuilders.termQuery("ecm:tag", "mytag"));

        tagService.tag(session, doc.getId(), "mytagbis");
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(facetedTags ? 1 : 3); // doc

        startTransaction();
        searchResponse = search(QueryBuilders.termQuery("ecm:tag", "mytagbis"));
        assertEquals(1, searchResponse.getHits().getTotalHits());

        tagService.untag(session, doc.getId(), "mytag");
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(facetedTags ? 1 : 2); // still doc

        startTransaction();
        searchResponse = search(QueryBuilders.termQuery("ecm:tag", "mytagbis"));
        assertEquals(1, searchResponse.getHits().getTotalHits());
        searchResponse = search(QueryBuilders.termQuery("ecm:tag", "mytag"));
        assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldHandleCreateDelete() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/folder", "note", "Note");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        // we don't wait for async
        TransactionHelper.startTransaction();
        session.removeDocument(folder.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

    @Test
    public void shouldHandleUpdateOnTransientDoc() throws Exception {
        startTransaction();
        DocumentModel tmpDoc = session.createDocumentModel("/", "file", "File");
        tmpDoc.setPropertyValue("dc:title", "TestMe");
        DocumentModel doc = session.createDocument(tmpDoc); // Send an ES_INSERT cmd
        session.saveDocument(doc); // Send an ES_UPDATE merged with ES_INSERT

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        // here we manipulate the transient doc with a null docid
        Assert.assertNull(tmpDoc.getId());
        tmpDoc.setPropertyValue("dc:title", "NewTitle");
        logFeature.hideWarningFromConsoleLog();
        session.saveDocument(tmpDoc);
        logFeature.restoreConsoleLog();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document Where dc:title='NewTitle'"));
        assertEquals(1, docs.totalSize());
    }

    @Test
    public void shouldHandleUpdateOnTransientDocBis() throws Exception {
        startTransaction();
        DocumentModel tmpDoc = session.createDocumentModel("/", "file", "File");
        tmpDoc.setPropertyValue("dc:title", "TestMe");
        DocumentModel doc = session.createDocument(tmpDoc); // Send an ES_INSERT cmd
        logFeature.hideWarningFromConsoleLog();
        session.saveDocument(doc); // Send an ES_UPDATE merged with ES_INSERT

        tmpDoc.setPropertyValue("dc:title", "NewTitle"); // ES_UPDATE with transient, merged
        session.saveDocument(tmpDoc);
        logFeature.restoreConsoleLog();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        // commands are not factored due to the misusage of the transient docs, we don't mind
        assertNumberOfCommandProcessed(2);

        startTransaction();
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document Where dc:title='NewTitle'"));
        assertEquals(1, docs.totalSize());
    }

    @Test
    public void shouldHandleUpdateBeforeInsertOnTransientDoc() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        session.createDocument(folder);
        logFeature.hideWarningFromConsoleLog();
        folder = session.saveDocument(folder); // generate a WARN and an UPDATE command
        logFeature.restoreConsoleLog();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(2);

        startTransaction();
    }

    @Test
    public void shouldIndexOrderedFolder() throws Exception {
        startTransaction();
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
        DocumentModel file = session.createDocumentModel("/ofolder/folder4", "testfile", "File");
        file = session.createDocument(file);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(6);
        startTransaction();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(
                String.format("SELECT * FROM Document WHERE ecm:parentId='%s' ORDER BY ecm:pos", ofolder.getId())));
        assertEquals(4, ret.totalSize());
        assertEquals(file1.getId(), ret.get(0).getId());
        assertEquals(file2.getId(), ret.get(1).getId());
        assertEquals(file3.getId(), ret.get(2).getId());

        session.orderBefore(ofolder.getRef(), "testfile3", "testfile2");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        ret = ess.query(new NxQueryBuilder(session).nxql(
                String.format("SELECT * FROM Document WHERE ecm:parentId='%s' ORDER BY ecm:pos", ofolder.getId())));
        assertEquals(4, ret.totalSize());
        assertEquals(file1.getId(), ret.get(0).getId());
        assertEquals(file3.getId(), ret.get(1).getId());
        assertEquals(file2.getId(), ret.get(2).getId());
    }

    @Test
    public void shouldNotIndexRecursivelyVersionFolder() throws Exception {
        startTransaction();
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

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        // 3 docs (2 files + 1 folder checkout) + 2 versions of folder
        assertNumberOfCommandProcessed(5);
        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(5, ret.totalSize());

        // delete the first version
        session.removeDocument(v1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);
        startTransaction();

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(4, ret.totalSize());
    }

    @Test
    public void shouldIndexLatestVersions() throws Exception {
        createADocumentWith3Versions();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(4, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestVersion = 1"));
        assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        assertEquals(1, ret.totalSize());
        assertEquals("v3", ret.get(0).getTitle());
        String versionSeriesId = ret.get(0).getVersionSeriesId();

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:versionVersionableId = '" + versionSeriesId + "'"));
        assertEquals(3, ret.totalSize());
    }

    /**
     * This test should be disabled now that we have an efficient way to reindex previous latest versions
     */
    @Test
    public void shouldNotIndexLatestVersions() throws Exception {
        System.setProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY, "true");
        try {
            createADocumentWith3Versions();
        } finally {
            System.clearProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY);
        }

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(4, ret.totalSize());

        // but isLatestVersion and isLatestMajorVersion are not updated
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestVersion = 1"));
        assertEquals(3, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        assertEquals(3, ret.totalSize());

    }

    /*
     * NXP-23033
     */
    @Test
    public void shouldIndexAfterVersionRestored() throws Exception {
        createADocumentWith3Versions();

        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isVersion = 0 AND dc:title='v3'"));
        assertEquals(1, ret.totalSize());
        DocumentModel doc = ret.get(0);
        assertEquals("v3", doc.getTitle());

        // document in ES is the last version
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        assertEquals(1, ret.totalSize());
        assertEquals("v3", ret.get(0).getTitle());

        // restore the document to v2 and check version in ES
        VersionModel v2VM = new VersionModelImpl();
        v2VM.setLabel("2.0");
        DocumentModel v2 = session.getDocumentWithVersion(doc.getRef(), v2VM);
        session.restoreToVersion(doc.getRef(), v2.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isVersion = 0 AND dc:title='v2'"));
        assertEquals(1, ret.totalSize());
        assertEquals("v2", ret.get(0).getTitle());
    }

    /*
     * NXP-23033
     */
    @Test
    public void shouldIndexAfterPublishThenRestore() throws Exception {
        startTransaction();
        // create a document
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "v0.1");
        doc = session.saveDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        assertEquals("0.1", proxy.getVersionLabel());

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // update document and version it
        doc.setPropertyValue("dc:title", "v0.2");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        doc = session.saveDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // check ES - document might have v1.1
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:path = '/file' and ecm:isVersion = 0 AND dc:title='v0.2'"));
        assertEquals(1, ret.totalSize());
        assertEquals("v0.2", ret.get(0).getTitle());

        // restore document to 1.0
        VersionModel versionModel = new VersionModelImpl();
        versionModel.setLabel("0.1");
        DocumentModel v1 = session.getDocumentWithVersion(doc.getRef(), versionModel);
        session.restoreToVersion(doc.getRef(), v1.getRef());

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // check ES - document might have v1.0
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:path = '/file' and ecm:isVersion = 0 AND dc:title='v0.1'"));
        assertEquals(1, ret.totalSize());
        assertEquals("v0.1", ret.get(0).getTitle());
    }

    protected void createADocumentWith3Versions() throws Exception {
        startTransaction();
        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        file1.setPropertyValue("dc:title", "v1");
        file1 = session.saveDocument(file1);
        DocumentRef v1 = file1.checkIn(VersioningOption.MAJOR, "init v1");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        file1.setPropertyValue("dc:title", "v2");
        file1 = session.saveDocument(file1);
        DocumentRef v2 = file1.checkIn(VersioningOption.MAJOR, "update v2");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        file1.setPropertyValue("dc:title", "v3");
        file1 = session.saveDocument(file1);
        DocumentRef v3 = file1.checkIn(VersioningOption.MAJOR, "update v3");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

    @Test
    public void testReadACLOnVersions() throws Exception {
        startTransaction();
        // NXP-30578 sticking to the scenario
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        // Give access to the data structure to user1
        setPermission(folder, "user1", READ_WRITE);

        Set<String> versionIds = new HashSet<>();
        try (CloseableCoreSession user1Session = CoreInstance.openCoreSession(session.getRepositoryName(), "user1")) {
            // Check in level 1
            for (int i = 0; i < 5; i++) {
                versionIds.add(versionDocument(user1Session, "/folder", "file" + i, "File"));
            }
            // Check in level 2
            versionIds.add(versionDocument(user1Session, "/folder", "subfolder", "Folder"));
            versionIds.add(versionDocument(user1Session, "/folder/subfolder", "file", "File"));
        }

        // Give access to the data structure to user2
        setPermission(folder, "user2", READ);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // user1 can find the versions
        versionIds.forEach(id -> assertCanQuery(id, "user1"));
        // user2 can also find the versions even if they were checked in before he gets access to the live documents
        versionIds.forEach(id -> assertCanQuery(id, "user2"));
    }

    protected void setPermission(DocumentModel doc, String user, String permission) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        ACE ace = new ACE(user, permission, true);
        localACL.add(ace);
        doc.setACP(acp, true);
    }

    protected String versionDocument(CoreSession userSession, String path, String name, String type) {
        DocumentModel file = userSession.createDocumentModel(path, name, type);
        file = userSession.createDocument(file);
        DocumentRef versionRef = userSession.checkIn(file.getRef(), VersioningOption.MINOR, null);
        return userSession.getDocument(versionRef).getId();
    }

    protected void assertCanQuery(String id, String userId) {
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "user1")) {
            DocumentModelList result = ess.query(
                    new NxQueryBuilder(userSession).nxql("SELECT * FROM Document WHERE ecm:uuid ='" + id + "'"));
            assertEquals(1, result.totalSize());
        }
    }

    @Test
    public void shouldIndexUpdatedProxy() throws Exception {
        startTransaction();
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);
        folder1 = session.saveDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        file1.setPropertyValue("dc:title", "Title before proxy update");
        file1 = session.saveDocument(file1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Create proxy
        DocumentModel proxy = session.createProxy(file1.getRef(), folder1.getRef());
        proxy = session.saveDocument(proxy);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Now update it
        proxy.setPropertyValue("dc:title", "Title after proxy update");
        proxy = session.saveDocument(proxy);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(3, ret.totalSize());

        // Check that proxy was updated in ES
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:isProxy = 1 and dc:title='Title after proxy update'"));
        assertEquals(1, ret.totalSize());
        assertEquals("Title after proxy update", ret.get(0).getTitle());

        // Check that live document was updated in ES
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:isProxy = 0 and dc:title='Title after proxy update'"));
        assertEquals(1, ret.totalSize());
        assertEquals("Title after proxy update", ret.get(0).getTitle());

    }

    // NXP-30219
    @Test
    public void shouldIndexUpdatedProxyAfterDocumentTrashed() throws Exception {
        startTransaction();
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1 = session.createDocument(file1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Create proxy
        session.createProxy(file1.getRef(), folder1.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Now trash live document
        trashService.trashDocument(file1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        assertEquals(3, ret.totalSize());

        // Check that live document was updated in ES
        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isProxy = 0 and ecm:isTrashed = 1"));
        assertEquals(1, ret.totalSize());
        Assert.assertTrue(ret.get(0).isTrashed());

        // Check that proxy was updated in ES
        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isProxy = 1 and ecm:isTrashed = 1"));
        assertEquals(1, ret.totalSize());
        Assert.assertTrue(ret.get(0).isTrashed());
    }

    // NXP-31007
    @Test
    public void shouldIndexProxyAfterVersionUpdate() throws Exception {
        startTransaction();
        DocumentModel folder1 = session.createDocumentModel("/", "testfolder1", "Folder");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = session.createDocumentModel("/", "testfile1", "File");
        file1.setPropertyValue("dc:description", "An old description");
        file1 = session.createDocument(file1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Publish the document
        DocumentModel proxy = session.publishDocument(file1, folder1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Update the version
        DocumentModel version = session.getLastDocumentVersion(file1.getRef());
        version.setPropertyValue("dc:description", "A new description");
        version.putContextData(CoreSession.ALLOW_VERSION_WRITE, Boolean.TRUE);
        session.saveDocument(version);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Check the proxy is updated
        proxy = session.getDocument(proxy.getRef());
        assertEquals("A new description", proxy.getPropertyValue("dc:description"));

        // Check the proxy is indexed
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dc:description = 'A new description' AND ecm:isProxy = 1"));
        assertEquals(1, docs.totalSize());
    }

    @Test
    public void shouldIndexComplexCase() throws Exception {
        startTransaction();

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
        File fieldAsJsonFile = FileUtils.getResourceFileFromContext("blob.json");
        try {
            Blob fb = Blobs.createBlob(fieldAsJsonFile, "image/jpeg");
            DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        doc = session.createDocument(doc);
        // session.saveDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(3);
        startTransaction();

    }

    @Test
    public void sortOnUnmappedField() throws Exception {

        // sort on a field that does not exist on the mapping and not present in the index
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document ORDER BY dc:source"));
        assertEquals(0, ret.totalSize());

        // sort on internal field
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document ORDER BY ecm:pos"));
        assertEquals(0, ret.totalSize());

    }

    @Test
    public void pathLevelFieldMustBeSeenAsKeyword() throws Exception {
        // Creates folders with names that can be taken as timestamp
        DocumentModel folder = session.createDocumentModel("/", "1530083790734003", "Folder");
        session.createDocument(folder);
        folder = session.createDocumentModel("/1530083790734003", "1530083790734004", "Folder");
        session.createDocument(folder);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();

        startTransaction();
        // Now creates folders with normal names to check that ecm:path@level# fields are typed as keyword and not as
        // date
        folder = session.createDocumentModel("/", "a-folder-name", "Folder");
        session.createDocument(folder);
        folder = session.createDocumentModel("/a-folder-name", "foo", "Folder");
        session.createDocument(folder);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

    @Test
    public void shouldIndexUpdatedRecord() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // no record found in index
        String nxql = "SELECT * FROM Document WHERE ecm:isRecord = 1";
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(0, ret.totalSize());

        // make the doc a record
        session.makeRecord(doc.getRef());
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // update
        startTransaction();

        // record is now found
        ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(1, ret.totalSize());

    }

    @Test
    public void shouldIndexUpdatedRetention() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        session.save();

        // no retention found in index
        String nxql1 = "SELECT * FROM Document WHERE ecm:retainUntil IS NOT NULL";
        DocumentModelList ret1 = ess.query(new NxQueryBuilder(session).nxql(nxql1));
        assertEquals(0, ret1.totalSize());

        // set retention to five seconds in the future
        Calendar fiveSeconds = Calendar.getInstance();
        fiveSeconds.add(Calendar.SECOND, 5);
        session.makeRecord(doc.getRef());
        session.setRetainUntil(doc.getRef(), fiveSeconds, null);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // update
        startTransaction();

        // retention is now found
        ret1 = ess.query(new NxQueryBuilder(session).nxql(nxql1));
        assertEquals(1, ret1.totalSize());

        // wait 8s to pass retention expiration date
        Thread.sleep(8_000);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        // wait for all bulk commands to be executed
        TransactionHelper.commitOrRollbackTransaction();
        Assert.assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // update
        startTransaction();

        // null retention is not found anymore
        ret1 = ess.query(new NxQueryBuilder(session).nxql(nxql1));
        assertEquals(0, ret1.totalSize());
    }

    @Test
    public void shouldIndexUpdatedLegalHold() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "File");
        doc = session.createDocument(doc);
        session.save();

        // no retention found in index
        String nxql = "SELECT * FROM Document WHERE ecm:hasLegalHold = 1";
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(0, ret.totalSize());

        // set legal hold
        session.makeRecord(doc.getRef());
        session.setLegalHold(doc.getRef(), true, null);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // update
        startTransaction();

        // legal hold is now found
        ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(1, ret.totalSize());

        // remove legal hold
        session.setLegalHold(doc.getRef(), false, null);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1); // update
        startTransaction();

        // legal hold is not found anymore
        ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        assertEquals(0, ret.totalSize());
    }

    @Test
    public void shouldExtractTextFromHtmlWhenIndexingNote() throws Exception {
        // Create a plain text note with html content
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "note", "Note");
        doc.setPropertyValue("note:note", "<guten>tag</guten><i>some</i> <b>text</b> to <img src='data:image/png;base64,ABC;'/> search");
        doc.setPropertyValue("note:mime_type", "text/plain");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
        // Text is searchable
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Note Where ecm:isVersion=0 AND ecm:fulltext='some text to search'"));
        assertEquals(1, ret.totalSize());
        // HTML tags are indexed and searchable because note is plain text
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Note WHERE ecm:isVersion=0 AND ecm:fulltext='guten tag base64'"));
        assertEquals(1, ret.totalSize());

        // Fix the mime type
        doc.setPropertyValue("note:mime_type", "text/html");
        session.saveDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
        // Text is searchable
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Note Where ecm:isVersion=0 AND ecm:fulltext='some text to search'"));
        assertEquals(1, ret.totalSize());
        // no more HTML tags
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Note WHERE ecm:isVersion=0 AND ecm:fulltext='guten tag base64'"));
        assertEquals(0, ret.totalSize());
    }

}
