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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test servcie declaration as well as basic indexing API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
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
    protected WorkManager workManager;

    @Inject
    protected BlobManager manager;

    @Inject
    ElasticSearchAdmin esa;

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

    protected void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void checkIndexing() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "Testme");
        // disable automatic indexing to control manually the indexing command
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // sync non recursive
        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        esi.indexNonRecursive(cmd);
        assertNumberOfCommandProcessed(1);

        esa.refresh();
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        SearchResponse searchResponse = esa.getClient().search(request);
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        request.source(new SearchSourceBuilder().query(QueryBuilders.matchQuery("ecm:title", "Testme")));
        searchResponse = esa.getClient().search(request);
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

    @Test
    public void shouldHandleMissingBlob() throws Exception {

        // Create the document with a string blob
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myBlobFile", "File");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc.setPropertyValue("dc:title", "myBlob");
        Blob fb = Blobs.createBlob("Not worth it", "image/jpeg");
        DocumentHelper.addBlob(doc.getProperty("file:content"), fb);
        doc = session.createDocument(doc);
        session.save();

        // Remove the binary
        BlobProperty blobProperty = (BlobProperty) doc.getProperty("file:content");
        String blobDigest = (String) blobProperty.getValue("digest");
        manager.getBlobProvider(session.getRepositoryName())
               .getBinaryManager()
               .getBinary(blobDigest)
               .getFile()
               .delete();

        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, true, false);
        esi.indexNonRecursive(cmd);
        assertNumberOfCommandProcessed(1);

        esi.indexNonRecursive(Arrays.asList(cmd, cmd));
        assertNumberOfCommandProcessed(3);
    }

    @Test
    public void checkManualAsyncIndexing() throws Exception {
        DocumentModel doc0 = session.createDocumentModel("/", "testNote", "Note");
        doc0.setPropertyValue("dc:title", "TestNote");
        doc0 = session.createDocument(doc0);
        session.save();

        // init index
        IndexingCommand cmd0 = new IndexingCommand(doc0, Type.INSERT, true, false);
        esi.indexNonRecursive(cmd0);

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "TestMe");
        doc.putContextData(ElasticSearchConstants.DISABLE_AUTO_INDEXING, Boolean.TRUE);
        doc = session.createDocument(doc);
        session.save();

        // only one doc should be indexed for now
        SearchRequest request = new SearchRequest(IDX_NAME).searchType(SearchType.DFS_QUERY_THEN_FETCH)
                                                           .source(new SearchSourceBuilder().from(0).size(60));
        SearchResponse searchResponse = esa.getClient().search(request);
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        request.source(new SearchSourceBuilder().query(QueryBuilders.matchQuery("ecm:title", "TestMe")));
        searchResponse = esa.getClient().search(request);
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        // now commit and wait for post commit indexing
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, false);
        esi.runIndexingWorker(Arrays.asList(cmd));
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        assertNumberOfCommandProcessed(1);

        // both docs are here
        startTransaction();
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));
        searchResponse = esa.getClient().search(request);
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());
        Assert.assertEquals(2, searchResponse.getHits().getTotalHits());
        request.source(new SearchSourceBuilder().query(QueryBuilders.matchQuery("ecm:title", "TestMe")));
        searchResponse = esa.getClient().search(request);
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
    }

}
