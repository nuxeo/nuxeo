/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.EventConstants;
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
    ElasticSearchAdmin esa;

    @Inject
    protected TrashService trashService;

    private int commandProcessed;

    @After
    public void cleanupIndexed() throws Exception {
        esa.initIndexes(true);
    }

    private void startCountingCommandProcessed() {
        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    private void assertNumberOfCommandProcessed(int processed)
            throws InterruptedException {
        Assert.assertNotNull(esa);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
        Assert.assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    @Test
    public void shouldIndexDocumentSynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc.getContextData().put(EventConstants.ES_SYNC_INDEXING_FLAG, true);
            doc = session.createDocument(doc);

        }
        // merge 5
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();

        int nbTry = 0;
        while (esa.getPendingCommands() > 0 && nbTry < 20) {
            Thread.sleep(1000);
            nbTry++;
        }

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldNotIndexDocumentSynchronouslyBecauseOfRollback()
            throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc.getContextData().put(EventConstants.ES_SYNC_INDEXING_FLAG, true);
            doc = session.createDocument(doc);
        }
        // Save session to prevent NXP-14494
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        int nbTry = 0;
        while (esa.getPendingCommands() > 0 && nbTry < 20) {
            Thread.sleep(1000);
            nbTry++;
        }

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);

        }
        startCountingCommandProcessed();

        TransactionHelper.commitOrRollbackTransaction();

        assertNumberOfCommandProcessed(10);

        TransactionHelper.startTransaction();

        esa.refresh();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldNotIndexDocumentAsynchronouslyBecauseOfRollback()
            throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());
        startCountingCommandProcessed();
        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);

        }
        // Save session to prevent NXP-14494
        session.save();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        assertNumberOfCommandProcessed(0);

        TransactionHelper.startTransaction();

        esa.refresh();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldUnIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(1);

        TransactionHelper.startTransaction();

        esa.refresh();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        // now delete the document
        session.removeDocument(doc.getRef());
        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(1);

        TransactionHelper.startTransaction();

        esa.refresh();

        searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

    }


    @Test
    public void shouldReIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

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
        assertNumberOfCommandProcessed(10);

        TransactionHelper.startTransaction();
        esa.refresh();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        int i = 0;
        startCountingCommandProcessed();
        for (SearchHit hit : searchResponse.getHits()) {
            i++;
            if (i> 8) {
                break;
            }
            DocumentModel doc = session.getDocument(new IdRef(hit.getId()));
            doc.setPropertyValue("dc:nature", "B");
            session.saveDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(8);

        TransactionHelper.startTransaction();

        esa.refresh();

        searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "B")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldIndexBinaryFulltext() throws Exception {
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        esa.refresh();

        TransactionHelper.startTransaction();
        DocumentModelList ret = ess.query(session, "SELECT * FROM Document", 10, 0);
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(session, "SELECT * FROM Document WHERE ecm:fulltext='search'", 10, 0);
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    public void shouldIndexOnPublishing() throws Exception {

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(4);

        TransactionHelper.startTransaction();
        esa.refresh();

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        // folder, version, file and proxy
        Assert.assertEquals(4, searchResponse.getHits().getTotalHits());

        // unpublish
        startCountingCommandProcessed();
        session.removeDocument(proxy.getRef());
        DocumentModelList docs = ess
                .query(new NxQueryBuilder(session) // .fetchFromElasticsearch()
                        .nxql("SELECT * FROM Document"));

        Assert.assertEquals(4, docs.totalSize());
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(1);

        TransactionHelper.startTransaction();

        esa.refresh();

        searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(3, searchResponse.getHits().getTotalHits());

    }


    @Test
    public void shouldUnIndexUsingTrashService() throws Exception {

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
        assertNumberOfCommandProcessed(2);

        TransactionHelper.startTransaction();
        esa.refresh();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(1, ret.totalSize());
        trashService.undeleteDocuments(Arrays.asList(doc));

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(1);

        TransactionHelper.startTransaction();

        esa.refresh();
        ret = ess.query(new NxQueryBuilder(session)
                .nxql("SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'deleted'"));
        Assert.assertEquals(2, ret.totalSize());

        SearchResponse searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60)
                .execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        trashService.purgeDocuments(session,
                Collections.singletonList(doc.getRef()));

        startCountingCommandProcessed();
        TransactionHelper.commitOrRollbackTransaction();
        assertNumberOfCommandProcessed(1);

        TransactionHelper.startTransaction();

        esa.refresh();

        searchResponse = esa.getClient().prepareSearch(
                IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60)
                .execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

    }
}
