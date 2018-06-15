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
 *     Delbosc Benoit
 */

package org.nuxeo.elasticsearch.test;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.ws")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-mapping-contrib.xml")
public class TestMapping {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    ElasticSearchAdmin esa;

    private int commandProcessed;

    public void assertNumberOfCommandProcessed(int processed) throws Exception {
        Assert.assertEquals(processed, esa.getTotalCommandProcessed() - commandProcessed);
    }

    /**
     * Wait for sync and async job and refresh the index
     */
    public void waitForIndexing() throws Exception {
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
    }

    public void startTransaction() {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        commandProcessed = esa.getTotalCommandProcessed();
    }

    @Before
    public void setUpMapping() throws Exception {
        esa.initIndexes(true);
    }

    @Test
    public void testIlikeSearch() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "upper case");
        doc.setPropertyValue("dc:description", "UPPER CASE DESC");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "Mixed Case");
        doc.setPropertyValue("dc:description", "MiXeD cAsE dEsC");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc3", "File");
        doc.setPropertyValue("dc:title", "lower case");
        doc.setPropertyValue("dc:description", "lower case desc");
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();
        assertNumberOfCommandProcessed(3);

        startTransaction();
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description ILIKE '%Case%'"));
        Assert.assertEquals(3, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description ILIKE 'Upper%'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE dc:description ILIKE 'mixED case desc'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title LIKE 'case%'"));
        Assert.assertEquals(3, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title LIKE 'Case%'"));
        Assert.assertEquals(3, ret.totalSize());

        // case sensitive for other operation
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE '%Case%'"));
        Assert.assertEquals(0, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE 'Upper%'"));
        Assert.assertEquals(0, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:description LIKE 'UPPER%'"));
        Assert.assertEquals(0, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE '%Case%'"));
        Assert.assertEquals(3, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE 'Upper%'"));
        Assert.assertEquals(1, ret.totalSize());
        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:description LIKE 'UPPER%'"));
        Assert.assertEquals(1, ret.totalSize());

        Assert.assertEquals(1, ret.totalSize());

    }

    @Test
    public void testFulltextAnalyzer() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "new-york.jpg");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "York.jpg");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc3", "File");
        doc.setPropertyValue("dc:title", "foo_jpg");
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();
        DocumentModelList ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new-york.jpg'"));
        Assert.assertEquals(1, ret.totalSize());

        // The standard tokenizer first split new-york.jpg in "new" "york.jpg"
        // then the word delimiter gives: "new" york" "jpg" "york.jpg"
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new york jpg'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'new-york'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'york new'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'york -new-york'"));
        Assert.assertEquals(1, ret.totalSize());
        Assert.assertEquals("testDoc2", ret.get(0).getName());

        ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'NewYork'"));
        Assert.assertEquals(1, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE ecm:fulltext.dc:title = 'jpg'"));
        Assert.assertEquals(3, ret.totalSize());

    }

    @Test
    public void testNgramSearch() throws Exception {
        startTransaction();
        DocumentModel doc = session.createDocumentModel("/", "testDoc1", "File");
        doc.setPropertyValue("dc:title", "FooBar12 test");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "testDoc2", "File");
        doc.setPropertyValue("dc:title", "foobar42");
        session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        waitForIndexing();

        startTransaction();

        // Common left/right truncature with a ILIKE, translated into wilcard search *oba* with poor performance
        DocumentModelList ret = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM Document WHERE dc:title ILIKE '%Oba%'"));
        Assert.assertEquals(2, ret.totalSize());

        // Use an ngram index
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'ObA'"));
        Assert.assertEquals(2, ret.totalSize());

        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'fOObar42'"));
        Assert.assertEquals(1, ret.totalSize());

        // No tokenizer mind the space
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = '2 t'"));
        Assert.assertEquals(1, ret.totalSize());

        // need to provide min_ngram (3) or more characters
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = '42'"));
        Assert.assertEquals(0, ret.totalSize());

        // If we don't set the proper analyzer the searched term is also ngramized matching too much
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) OPERATOR(match) */ dc:title = 'ZOObar'"));
        Assert.assertEquals(2, ret.totalSize());

        // Using the lowercase analyzer for the search term and a ngram max_gram greater than the search term make it
        // work
        ret = ess.query(new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE /*+ES: INDEX(dc:title.ngram) ANALYZER(lowercase_analyzer) OPERATOR(match) */ dc:title = 'ZOObar'"));
        Assert.assertEquals(0, ret.totalSize());

    }
}
