/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.api.WorkManager;
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
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test "on the fly" indexing via the listener system
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.tag", "org.nuxeo.ecm.platform.ws", "org.nuxeo.ecm.automation.core" })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestAutomaticIndexing {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected TrashService trashService;

    @Inject
    ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected TagService tagService;

    @Inject
    protected WorkManager workManager;

    private boolean syncMode = false;

    private Priority consoleThresold;

    private int commandProcessed;

    // Number of processed command since the startTransaction
    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    /**
     * Wait for async worker completion then wait for indexing completion
     */
    public void waitForCompletion() throws Exception {
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
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
        restoreConsoleLog();
    }

    protected void startTransaction() {
        if (syncMode) {
            ElasticSearchInlineListener.useSyncIndexing.set(true);
        }
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    protected void hideWarningFromConsoleLog() {
        Logger rootLogger = Logger.getRootLogger();
        ConsoleAppender consoleAppender = (ConsoleAppender) rootLogger.getAppender("CONSOLE");
        consoleThresold = consoleAppender.getThreshold();
        consoleAppender.setThreshold(Level.ERROR);
    }

    protected void restoreConsoleLog() {
        if (consoleThresold == null) {
            return;
        }
        Logger rootLogger = Logger.getRootLogger();
        ConsoleAppender consoleAppender = (ConsoleAppender) rootLogger.getAppender("CONSOLE");
        consoleAppender.setThreshold(consoleThresold);
        consoleThresold = null;
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
        SearchResponse searchResponse = esa.getClient()
                                           .prepareSearch(IDX_NAME)
                                           .setTypes(TYPE_NAME)
                                           .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                                           .setFrom(0)
                                           .setSize(60)
                                           .execute()
                                           .actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
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
        SearchResponse searchResponse = esa.getClient()
                                           .prepareSearch(IDX_NAME)
                                           .setTypes(TYPE_NAME)
                                           .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                                           .setFrom(0)
                                           .setSize(60)
                                           .execute()
                                           .actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
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
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());
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
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
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
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        // now delete the document
        session.removeDocument(doc.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
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
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

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
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchQuery("dc:nature", "B")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());
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
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext='search'"));
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    public void shouldIndexLargeBinaryFulltext() throws Exception {
        startTransaction();
        activateSynchronousMode(); // this is to prevent race condition that happen NXP-16169
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob(new String(new char[33000]).replace('\0', 'a') + " search"));
        // Note that token > 32k fails only when using a disk storage elastic configuration
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext='search'"));
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    public void shouldIndexLargeToken() throws Exception {
        assumeTrue("DB backend needs to support fields bigger than 32k",
                coreFeature.getStorageConfiguration().isVCSH2());

        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:source", "search foo" + new String(new char[33000]).replace('\0', 'a'));
        // Note that token > 32k error is raised only when using a disk storage elastic configuration
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        waitForCompletion();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:source LIKE 'search*'"));
        Assert.assertEquals(1, ret.totalSize());
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
        assertNumberOfCommandProcessed(5);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        // folder, version, file and proxy
        Assert.assertEquals(4, searchResponse.getHits().getTotalHits());

        // unpublish
        session.removeDocument(proxy.getRef());
        DocumentModelList docs = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document"));
        Assert.assertEquals(4, docs.totalSize());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(3, searchResponse.getHits().getTotalHits());

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
        DocumentModelList docs = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document WHERE ecm:fulltext = 'foo' AND ecm:isVersion = 0"));
        Assert.assertEquals(2, docs.totalSize());

        doc.setPropertyValue("dc:description", "bar");
        session.saveDocument(doc);
        session.publishDocument(doc, folder);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        docs = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document WHERE ecm:fulltext = 'bar' AND ecm:isVersion = 0"));
        Assert.assertEquals(2, docs.totalSize());
    }

    @Test
    public void shouldUnIndexUsingTrashService() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        trashService.trashDocuments(Arrays.asList(doc));

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(2);

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(1, ret.totalSize());
        trashService.undeleteDocuments(Arrays.asList(doc));

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(2, ret.totalSize());

        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        trashService.purgeDocuments(session, Collections.singletonList(doc.getRef()));

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
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

        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(3, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexTag() throws Exception {
        assumeTrue("DBS does not support tags", !coreFeature.getStorageConfiguration().isDBS());

        // ElasticSearchInlineListener.useSyncIndexing.set(true);
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        tagService.tag(session, doc.getId(), "mytag", "Administrator");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        assertNumberOfCommandProcessed(3); // doc, tagging relation and tag

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).setQuery(
                QueryBuilders.termQuery("ecm:tag", "mytag")).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        tagService.tag(session, doc.getId(), "mytagbis", "Administrator");
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(3); // doc, tagging and new tag

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).setQuery(
                QueryBuilders.termQuery("ecm:tag", "mytagbis")).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        tagService.untag(session, doc.getId(), "mytag", "Administrator");
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(2); // doc, tagging

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).setQuery(
                QueryBuilders.termQuery("ecm:tag", "mytagbis")).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).setQuery(
                QueryBuilders.termQuery("ecm:tag", "mytag")).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
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
        hideWarningFromConsoleLog();
        session.saveDocument(tmpDoc);
        restoreConsoleLog();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document Where dc:title='NewTitle'"));
        Assert.assertEquals(1, docs.totalSize());
    }

    @Test
    public void shouldHandleUpdateOnTransientDocBis() throws Exception {
        startTransaction();
        DocumentModel tmpDoc = session.createDocumentModel("/", "file", "File");
        tmpDoc.setPropertyValue("dc:title", "TestMe");
        DocumentModel doc = session.createDocument(tmpDoc); // Send an ES_INSERT cmd
        hideWarningFromConsoleLog();
        session.saveDocument(doc); // Send an ES_UPDATE merged with ES_INSERT

        tmpDoc.setPropertyValue("dc:title", "NewTitle"); // ES_UPDATE with transient, merged
        session.saveDocument(tmpDoc);
        restoreConsoleLog();

        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        // commands are not factored due to the misusage of the transient docs, we don't mind
        assertNumberOfCommandProcessed(2);

        startTransaction();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document Where dc:title='NewTitle'"));
        Assert.assertEquals(1, docs.totalSize());
    }

    @Test
    public void shouldHandleUpdateBeforeInsertOnTransientDoc() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        session.createDocument(folder);
        hideWarningFromConsoleLog();
        folder = session.saveDocument(folder); // generate a WARN and an UPDATE command
        restoreConsoleLog();

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
        Assert.assertEquals(4, ret.totalSize());
        Assert.assertEquals(file1.getId(), ret.get(0).getId());
        Assert.assertEquals(file2.getId(), ret.get(1).getId());
        Assert.assertEquals(file3.getId(), ret.get(2).getId());

        session.orderBefore(ofolder.getRef(), "testfile3", "testfile2");
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        // only the 4 direct children are reindexed
        assertNumberOfCommandProcessed(4);
        startTransaction();

        ret = ess.query(new NxQueryBuilder(session).nxql(
                String.format("SELECT * FROM Document WHERE ecm:parentId='%s' ORDER BY ecm:pos", ofolder.getId())));
        Assert.assertEquals(4, ret.totalSize());
        Assert.assertEquals(file1.getId(), ret.get(0).getId());
        Assert.assertEquals(file3.getId(), ret.get(1).getId());
        Assert.assertEquals(file2.getId(), ret.get(2).getId());
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
        // 3 docs (2 files + 1 folder checkout) + 2 versions of folder + 2 versions (because of isLastVersions)
        assertNumberOfCommandProcessed(7);
        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(5, ret.totalSize());

        // delete the first version
        session.removeDocument(v1);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);
        startTransaction();

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(4, ret.totalSize());
    }

    @Test
    public void shouldIndexLatestVersions() throws Exception {
        createADocumentWith3Versions();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(4, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestVersion = 1"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("v3", ret.get(0).getTitle());
    }

    @Test
    public void shouldNotIndexLatestVersions() throws Exception {
        System.setProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY, "true");
        try {
            createADocumentWith3Versions();
        } finally {
            System.clearProperty(AbstractSession.DISABLED_ISLATESTVERSION_PROPERTY);
        }

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(4, ret.totalSize());

        // but isLatestVersion and isLatestMajorVersion are not updated
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestVersion = 1"));
        Assert.assertEquals(3, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        Assert.assertEquals(3, ret.totalSize());

    }

    /*
     * NXP-23033
     */
    @Test
    public void shouldIndexAfterVersionRestored() throws Exception {
        createADocumentWith3Versions();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isVersion = 0 AND dc:title='v3'"));
        Assert.assertEquals(1, ret.totalSize());
        DocumentModel doc = ret.get(0);
        Assert.assertEquals("v3", doc.getTitle());

        // document in ES is the last version
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isLatestMajorVersion = 1"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("v3", ret.get(0).getTitle());

        // restore the document to v2 and check version in ES
        VersionModel v2VM = new VersionModelImpl();
        v2VM.setLabel("2.0");
        DocumentModel v2 = session.getDocumentWithVersion(doc.getRef(), v2VM);
        session.restoreToVersion(doc.getRef(), v2.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:isVersion = 0 AND dc:title='v2'"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("v2", ret.get(0).getTitle());
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
        Assert.assertEquals("0.1", proxy.getVersionLabel());

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
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("v0.2", ret.get(0).getTitle());

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
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("v0.1", ret.get(0).getTitle());
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
        Assert.assertEquals(3, ret.totalSize());

        // Check that proxy was updated in ES
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:isProxy = 1 and dc:title='Title after proxy update'"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("Title after proxy update", ret.get(0).getTitle());

        // Check that live document was updated in ES
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:isProxy = 0 and dc:title='Title after proxy update'"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("Title after proxy update", ret.get(0).getTitle());

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

}
