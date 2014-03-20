package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-config-test-contrib.xml")
public class TestTreeIndexing {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    @Before
    public void initIndex() throws Exception {
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        esa.initIndexes(true);
    }

    protected void buildTree() throws ClientException {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            doc = session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void buildAndIndexTree() throws Exception {

        // build the tree
        buildTree();

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertTrue(esa.getPendingCommands()>1);
        Assert.assertTrue(esa.getPendingDocs()>1);

        // wait for indexing
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esi.flush();

        // check indexing
        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        TransactionHelper.startTransaction();
    }

    @Test
    public void shouldIndexTree() throws Exception {

        buildAndIndexTree();

        // check sub tree search
        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path",
                        "/folder0/folder1/folder2")).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldUnIndexSubTree() throws Exception {

        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        session.removeDocument(ref);

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        // async command is not yet scheduled
        Assert.assertEquals(1, esa.getPendingCommands());

        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldIndexMovedSubTree() throws Exception {

        buildAndIndexTree();

        DocumentRef ref = new PathRef("/folder0/folder1/folder2");
        Assert.assertTrue(session.exists(ref));
        DocumentModel doc = session.getDocument(ref);

        // move in the same folder : rename
        session.move(ref, doc.getParentRef(), "folderA");

        TransactionHelper.commitOrRollbackTransaction();

        Assert.assertEquals(1, esa.getPendingDocs());
        Assert.assertEquals(1, esa.getPendingCommands());

        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esi.flush();

        SearchResponse searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

        // check sub tree search
        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path",
                        "/folder0/folder1/folder2")).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path",
                        "/folder0/folder1/folderA")).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.prefixQuery("ecm:path",
                        "/folder0/folder1")).execute().actionGet();
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

        
        
    }
    
    

}
