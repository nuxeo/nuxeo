package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.ElasticSearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features({RepositoryElasticSearchFeature.class})
public class ElasticSearchServiceIndexingTest {

    @Inject
    protected CoreSession session;

    @Test
    public void shouldIndexDocument() throws Exception {

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        Assert.assertNotNull(esa);
        Assert.assertEquals(0, esa.getPendingIndexingTasksCount());

        for(int i = 0; i < 10; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc"+ i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
        }
        session.save();

        Assert.assertTrue(esa.getPendingIndexingTasksCount()>0);

        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.awaitCompletion( 20, TimeUnit.SECONDS);

        SearchResponse searchResponse = ess.getClient().prepareSearch(ElasticSearchComponent.MAIN_IDX)
                //.setTypes("doc")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                //.setQuery(QueryBuilders.termQuery("type", "File"))             // Query
                //.setQuery(QueryBuilders.termQuery("title", "Test Me"))             // Query
                //.setQuery(QueryBuilders.termQuery("dc:title", "Test"))             // Query
                //.setQuery(QueryBuilders.termQuery("uid", doc.getId()))             // Query
                .setFrom(0).setSize(60)
                .execute()
                .actionGet();
        Assert.assertEquals(10, searchResponse.getHits().getTotalHits());


    }






}
