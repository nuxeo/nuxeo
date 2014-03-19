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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.listener.EventConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test servcie declaration as well as basic indexing API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:disable-listener-contrib.xml")
public class TestManualIndexing {

    @Inject
    protected CoreSession session;

    @After
    public void cleanupIndexed() throws Exception {
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        esa.initIndexes(true);
    }


    @Test
    public void checkManualSyncIndexing() throws Exception {

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        Assert.assertNotNull(ess);
        Assert.assertNotNull(esi);

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(EventConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        IndexingCommand cmd = new IndexingCommand(doc, true, false);
        esi.indexNow(cmd);

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("ecm:title", "TestMe")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void checkManualPostCommitIndexing() throws Exception {

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);

        Assert.assertNotNull(ess);
        Assert.assertNotNull(esi);
        Assert.assertNotNull(esa);

        DocumentModel doc0 = session.createDocumentModel("/", "testNote",
                "Note");
        doc0.setPropertyValue("dc:title", "TesNote");
        doc0 = session.createDocument(doc0);
        session.save();

        // init index
        IndexingCommand cmd0 = new IndexingCommand(doc0, true, false);
        esi.indexNow(cmd0);

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(EventConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // ask for postcommit indexing
        IndexingCommand cmd = new IndexingCommand(doc, true, false);
        esi.scheduleIndexing(cmd);

        esi.flush();

        // only one doc should be indexed for now
        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("ecm:title", "TestMe")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        Assert.assertEquals(1, esa.getPendingCommands());
        Assert.assertEquals(1, esa.getPendingDocs());

        // now commit and wait for post commit indexing
        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        int nbTry = 0;
        while (esa.getPendingCommands() > 0 && nbTry < 20) {
            Thread.sleep(400);
            nbTry++;
        }

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        TransactionHelper.startTransaction();

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("ecm:title", "TestMe")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void checkManualAsyncIndexing() throws Exception {

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);

        Assert.assertNotNull(ess);
        Assert.assertNotNull(esi);
        Assert.assertNotNull(esa);

        DocumentModel doc0 = session.createDocumentModel("/", "testNote",
                "Note");
        doc0.setPropertyValue("dc:title", "TesNote");
        doc0 = session.createDocument(doc0);
        session.save();

        // init index
        IndexingCommand cmd0 = new IndexingCommand(doc0, true, false);
        esi.indexNow(cmd0);

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(EventConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // ask for postcommit indexing
        IndexingCommand cmd = new IndexingCommand(doc, false, false);
        esi.scheduleIndexing(cmd);

        esi.flush();

        // only one doc should be indexed for now
        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("ecm:title", "TestMe")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        Assert.assertEquals(1, esa.getPendingCommands());
        Assert.assertEquals(1, esa.getPendingDocs());

        // now commit and wait for post commit indexing
        TransactionHelper.commitOrRollbackTransaction();

        WorkManager wm = Framework.getLocalService(WorkManager.class);

        wm.awaitCompletion(5, TimeUnit.SECONDS);

        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esi.flush();

        TransactionHelper.startTransaction();

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("ecm:title", "TestMe")).setFrom(0).setSize(
                60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

}
