package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-config-test-contrib.xml")

public class ElasticSearchIndexTree {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

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

    @Test
    public void verifyIndexingOnTree() throws Exception {

        // build the tree
        buildTree();

        TransactionHelper.commitOrRollbackTransaction();

        // wait for indexing
        Assert.assertEquals(10, esa.getPendingCommands());
        Assert.assertEquals(10, esa.getPendingDocs());
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

        searchResponse = ess.getClient().prepareSearch(
                ElasticSearchComponent.MAIN_IDX).setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString("ecm\\:path.children: \"/folder0/folder1/folder2\"")).execute().actionGet();
        Assert.assertEquals(8, searchResponse.getHits().getTotalHits());

    }

}
