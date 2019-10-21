/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test for native ES queries
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-nested-mapping-contrib.xml")
public class TestElasticSearchQuery {

    private static final String SCORE = "score";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected WorkManager workManager;

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
    public void searchWithESNestedQuery() throws Exception {
        createDocumentWithFiles();

        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(1, ret.totalSize());
        // search without correlation works
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE files:files/*/file/name = 'testfile1.txt' AND files:files/*/file/length=3"));
        Assert.assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE files:files/file/name = 'testfile1.txt' AND files:files/file/length=3"));
        Assert.assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE files:files/file/name = 'testfile4.txt' AND files:files/file/length=3"));
        Assert.assertEquals(1, ret.totalSize());
        // search with correlation is not supported in NXQL
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE files:files/*1/file/name = 'testfile3.txt' AND files:files/*1/file/length=3"));
        Assert.assertEquals(0, ret.totalSize());

        // But it is possible using ES nested query along with a proper mapping
        QueryBuilder qb = QueryBuilders.nestedQuery("files:files.file",
                QueryBuilders.boolQuery()
                             .must(QueryBuilders.termQuery("files:files.file.name", "testfile1.txt"))
                             .must(QueryBuilders.termQuery("files:files.file.length", 3)),
                ScoreMode.Avg);
        ret = ess.query(new NxQueryBuilder(session).esQuery(qb));
        Assert.assertEquals(0, ret.totalSize());

        qb = QueryBuilders.nestedQuery("files:files.file",
                QueryBuilders.boolQuery()
                             .must(QueryBuilders.termQuery("files:files.file.name", "testfile3.txt"))
                             .must(QueryBuilders.termQuery("files:files.file.length", 3)),
                ScoreMode.Avg);
        ret = ess.query(new NxQueryBuilder(session).esQuery(qb));
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-nested-contrib.xml")
    public void searchWithNestedNXQLQuery() throws Exception {
        createDocumentWithFiles();

        // Use an ES Hint Nested provided by a contribution (elasticsearch-test-nested-contrib.xml)
        String nxql = "SELECT * FROM Document WHERE /*+ES: INDEX(files:files.file.name, files:files.file.length) OPERATOR(nestedFilesQuery) */ nested:value IN ('my-text-file', '0')";
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(0, ret.totalSize());

        nxql = "SELECT * FROM Document WHERE /*+ES: INDEX(files:files.file.name, files:files.file.length) OPERATOR(nestedFilesQuery) */ nested:value IN ('testfile3.txt', '3')";
        ret = ess.query(new NxQueryBuilder(session).nxql(nxql));
        Assert.assertEquals(1, ret.totalSize());
    }

    @Test
    public void testMoreLikeThisQuery() throws Exception {

        // Create test data
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "very nice title");
        doc = session.createDocument(doc);

        for (int i = 1; i <= 5; i++) {
            DocumentModel likeDoc = session.createDocumentModel("/", "myFile" + i, "File");
            String title = i % 2 == 0 ? "nice title" : "nice";
            likeDoc.setPropertyValue("dc:title", title + i);
            session.createDocument(likeDoc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // Query More Like This
        MoreLikeThisQueryBuilder.Item item = new MoreLikeThisQueryBuilder.Item(null, null, doc.getId());
        QueryBuilder elasticBuilder = QueryBuilders.moreLikeThisQuery(new String[] { "dc:title.fulltext" }, null,
                new MoreLikeThisQueryBuilder.Item[] { item })
                                                   .minTermFreq(1)
                                                   .minWordLength(3)
                                                   .maxQueryTerms(20)
                                                   .minimumShouldMatch("45%");

        NxQueryBuilder nxQuery = new NxQueryBuilder(session).esQuery(elasticBuilder)
                                                            .fetchFromElasticsearch()
                                                            .hitDocConsumer((searchHit, aDoc) -> {
                                                                aDoc.putContextData(SCORE, searchHit.getScore());
                                                            })
                                                            .limit(20);
        DocumentModelList ret = ess.query(nxQuery);
        Assert.assertEquals(5, ret.totalSize());
        Assert.assertNotNull(ret.get(0).getContextData(SCORE));
    }

    @Test
    public void testSecurity() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "aFile", "File");
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();

        // match the document with system user
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document"));
        Assert.assertEquals(1, ret.totalSize());
        QueryBuilder qb = QueryBuilders.matchAllQuery();
        ret = ess.query(new NxQueryBuilder(session).esQuery(qb));
        Assert.assertEquals(1, ret.totalSize());

        // no match for unknown user
        try (CloseableCoreSession restrictedSession = CoreInstance.openCoreSession(session.getRepositoryName(),
                "bob")) {
            ret = ess.query(new NxQueryBuilder(restrictedSession).nxql("SELECT * FROM Document"));
            Assert.assertEquals(0, ret.totalSize());
            ret = ess.query(new NxQueryBuilder(restrictedSession).esQuery(qb));
            Assert.assertEquals(0, ret.totalSize());
        }
    }

    protected void createDocumentWithFiles() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        // create doc with a list of blob attachment (textfile1.txt length=1,textfile2.txt length=2, ...)
        var blobs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Blob blob = Blobs.createBlob(new String(new char[i]).replace('\0', 'a'));
            blob.setFilename(String.format("testfile%d.txt", i));
            blobs.add(Map.of("file", (Serializable) blob));
        }
        doc.setPropertyValue("files:files", blobs);
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        waitForCompletion();
        startTransaction();
    }

}
