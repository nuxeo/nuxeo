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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
@LocalDeploy({ "org.nuxeo.elasticsearch.core:disable-listener-contrib.xml",
        "org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml" })
public class TestManualIndexing {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchIndexing esi;

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
        Assert.assertFalse("Strill indexing in progress",
                esa.isIndexingInProgress());
        esa.refresh();
    }

    @After
    public void cleanupIndexed() throws Exception {
        ElasticSearchAdmin esa = Framework
                .getLocalService(ElasticSearchAdmin.class);
        esa.initIndexes(true);
    }

    @Test
    public void checkIndexing() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        // disable automatic indexing to control manually the indexing command
        doc.putContextData(EventConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        startCountingCommandProcessed();
        // sync non recursive
        IndexingCommand cmd = new IndexingCommand(doc, true, false);
        esi.indexNow(cmd);
        assertNumberOfCommandProcessed(1);

        esa.refresh();
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("ecm:title", "TestMe"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void checkPostCommitIndexing() throws Exception {
        // create one doc that is going to be indexed in an async job
        DocumentModel doc0 = session.createDocumentModel("/", "testNote",
                "Note");
        doc0.setPropertyValue("dc:title", "TesNote");
        doc0 = session.createDocument(doc0);
        session.save();

        // but ask to index it right now
        IndexingCommand cmd0 = new IndexingCommand(doc0, true, false);
        esi.indexNow(cmd0);

        // create another doc without the automatic indexing
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(EventConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // schedule the indexing in sync (i.e in postcommit)
        IndexingCommand cmd = new IndexingCommand(doc, true, false);
        startCountingCommandProcessed();
        esi.scheduleIndexing(cmd);
        esa.refresh();
        assertNumberOfCommandProcessed(0);

        // only the first doc testNote should be in the index
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("ecm:title", "TestMe"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        // now commit and wait for post commit indexing
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        // both document are present in the index
        TransactionHelper.startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("ecm:title", "TestMe"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void checkManualAsyncIndexing() throws Exception {
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

        // ask for async indexing
        startCountingCommandProcessed();
        IndexingCommand cmd = new IndexingCommand(doc, false, false);
        esi.scheduleIndexing(cmd);
        esa.refresh();
        assertNumberOfCommandProcessed(0);

        // only one doc should be indexed for now
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("ecm:title", "TestMe"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        // now commit and wait for post commit indexing
        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(1);

        // both docs are here
        TransactionHelper.startTransaction();
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());
        searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("ecm:title", "TestMe"))
                .setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

}
