package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestFetchDocumentsFromEs {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void buildTree() throws ClientException {
        String root = "/";
        for (int i = 0; i < 10; i++) {
            String name = "folder" + i;
            DocumentModel doc = session.createDocumentModel(root, name,
                    "Folder");
            doc.setPropertyValue("dc:title", "Folder" + i);
            session.createDocument(doc);
            root = root + name + "/";
        }
    }

    protected void waitForAsyncIndexing() throws Exception {
        // wait for indexing
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());
    }

    protected void buildAndIndexTree() throws Exception {

        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }

        // build the tree
        buildTree();

        int n = esa.getTotalCommandProcessed();

        TransactionHelper.commitOrRollbackTransaction();

        waitForAsyncIndexing();

        Assert.assertEquals(10, esa.getTotalCommandProcessed() - n);
        esa.refresh();

        TransactionHelper.startTransaction();

        // check indexing
        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME)
                .setTypes(TYPE_NAME)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0)
                .setSize(60).execute().actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());

    }

    @Test
    public void shouldLoadDocumentFromEs() throws Exception {
        buildAndIndexTree();
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(
                "select * from Document").limit(20).fetchFromElasticsearch());
        Assert.assertEquals(10, docs.totalSize());
        /*
        for (DocumentModel doc : docs) {
            System.out.println(doc);
        }
        */

    }

}
