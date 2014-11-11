/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test "on the fly" indexing via the listener system
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestAutomaticIndexing {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;
    @Inject
    protected TrashService trashService;
    @Inject
    ElasticSearchAdmin esa;
    private int commandProcessed;
    private boolean syncMode = false;

    public void startCountingCommandProcessed() {
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed()
                - commandProcessed);
    }

    /**
     * Wait for sync and async job and refresh the index
     */
    public void waitForIndexing() throws Exception {
        for (int i = 0; (i < 100) && esa.isIndexingInProgress(); i++) {
            Thread.sleep(100);
        }
        Assert.assertFalse("Still indexing in progress",
                esa.isIndexingInProgress());
        esa.refresh();
    }

    public void activateSynchronousMode() throws Exception {
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        syncMode = true;
    }

    @After
    public void disableSynchronousMode() {
        ElasticSearchInlineListener.useSyncIndexing.set(false);
        syncMode = false;
    }

    public void startTransaction() {
        if (syncMode) {
            ElasticSearchInlineListener.useSyncIndexing.set(true);
        }
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    @After
    public void cleanupIndexed() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void shouldIndexDocument() throws Exception {
        startTransaction();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        // merge 5
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session
                    .getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
        }
        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldNotIndexDocumentBecauseOfRollback() throws Exception {
        startTransaction();
        // create 10 docs
        activateSynchronousMode();
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        // Save session to prevent NXP-14494
        startCountingCommandProcessed();
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(0);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldUnIndexDocument() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        // now delete the document
        session.removeDocument(doc.getRef());
        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldReIndexDocument() throws Exception {
        startTransaction();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc.setPropertyValue("dc:nature", "A");
            doc = session.createDocument(doc);

        }
        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(10);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("dc:nature", "A"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        int i = 0;
        startCountingCommandProcessed();
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
        waitForIndexing();
        assertNumberOfCommandProcessed(8);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("dc:nature", "A"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("dc:nature", "B"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexBinaryFulltext() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        // we need to wait for the async fulltext indexing
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        waitForIndexing();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document").limit(10));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext='search'")
                .limit(10));
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    public void shouldIndexOnPublishing() throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(4);

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        // folder, version, file and proxy
        Assert.assertEquals(4, searchResponse.getHits().getTotalHits());

        // unpublish
        startCountingCommandProcessed();
        session.removeDocument(proxy.getRef());
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session) // .fetchFromElasticsearch()
                        .nxql("SELECT * FROM Document"));

        Assert.assertEquals(4, docs.totalSize());
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(3, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldUnIndexUsingTrashService() throws Exception {
        startTransaction();
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        trashService.trashDocuments(Arrays.asList(doc));
        ElasticSearchService ess = Framework
                .getLocalService(ElasticSearchService.class);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(2);

        startTransaction();

        DocumentModelList ret = ess
                .query(new NxQueryBuilder(session)
                        .nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(1, ret.totalSize());
        trashService.undeleteDocuments(Arrays.asList(doc));

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        ret = ess
                .query(new NxQueryBuilder(session)
                        .nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(2, ret.totalSize());

        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        trashService.purgeDocuments(session,
                Collections.singletonList(doc.getRef()));

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexOnCopy() throws Exception {
        startTransaction();
        startCountingCommandProcessed();
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(2);

        startTransaction();
        DocumentRef src = doc.getRef();
        DocumentRef dst = new PathRef("/");
        session.copy(src, dst, "file2");
        // turn the sync flag after the action
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(3, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldHandleCreateDelete() throws Exception {
        startTransaction();
        startCountingCommandProcessed();
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/folder", "note", "Note");
        doc = session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        // we don't wait for async
        startTransaction();
        session.removeDocument(folder.getRef());
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        startTransaction();
    }
}
