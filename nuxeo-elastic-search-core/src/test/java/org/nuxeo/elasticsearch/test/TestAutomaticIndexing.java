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

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.listener.EventConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
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
public class TestAutomaticIndexing {

    @Inject
    protected CoreSession session;

    @Inject
    ElasticSearchIndexing esi;

    @Before
    public void initIndex() throws Exception {
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        esa.initIndexes(false);
    }

    @After
    public void cleanupIndexed() throws Exception {
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        esa.initIndexes(true);
    }

    @Test
    public void shouldIndexDocumentSynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
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
        // update 5
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/testDoc" + i));
            doc.setPropertyValue("dc:description", "Description TestMe" + i);
            doc = session.saveDocument(doc);
        }

        // this should not be needed !
        // session.save();
        TransactionHelper.commitOrRollbackTransaction();

        int nbTry = 0;
        while (esa.getPendingCommands() > 0 && nbTry < 20) {
            Thread.sleep(1000);
            nbTry++;
        }

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldNotIndexDocumentSynchronouslyBecauseOfRollback()
            throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
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

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);

        }

        // this should not be needed !
        // session.save();
        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(10, esa.getPendingCommands());
        Assert.assertEquals(10, esa.getPendingDocs());

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldNotIndexDocumentAsynchronouslyBecauseOfRollback()
            throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        // create 10 docs
        for (int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i,
                    "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);

        }

        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldUnIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingDocs());

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingCommands());
        Assert.assertEquals(1, esa.getPendingDocs());

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        // now delete the document
        session.removeDocument(doc.getRef());

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingCommands());
        Assert.assertEquals(1, esa.getPendingDocs());

        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldReIndexDocumentAsynchronously() throws Exception {

        Assert.assertTrue(TransactionHelper.isTransactionActive());

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
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

        // this should not be needed !
        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(10, esa.getPendingCommands());
        Assert.assertEquals(10, esa.getPendingDocs());

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        int i = 0;
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

        Assert.assertEquals(8, esa.getPendingCommands());
        Assert.assertEquals(8, esa.getPendingDocs());

        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        esi.flush();

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "A")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:nature", "B")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

    }

}
