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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.test.nxql;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_AVG;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.AGG_CARDINALITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.core.AggregateDescriptor;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.aggregate.SingleValueMetricAggregate;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.core.EsResultSetImpl;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestCompareQueryAndFetch {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected TrashService trashService;

    private String proxyPath;

    @Before
    public void initWorkingDocuments() throws Exception {
        esa.initIndexes(true);
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2000, 1 - 1, 2, 3, 4, 5);
        cal.set(Calendar.MILLISECOND, 6);
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc.setPropertyValue("dc:issued", cal);
            doc = session.createDocument(doc);
        }
        for (int i = 5; i < 10; i++) {
            String name = "note" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "Note");
            doc.setPropertyValue("dc:title", "Note" + i);
            doc.setPropertyValue("note:note", "Content" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }

        DocumentModel doc = session.createDocumentModel("/", "hidden", "HiddenFolder");
        doc.setPropertyValue("dc:title", "HiddenFolder");
        doc = session.createDocument(doc);

        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.getDocument(new PathRef("/file3"));
        DocumentModel proxy = session.publishDocument(file, folder);
        proxyPath = proxy.getPathAsString();

        trashService.trashDocument(session.getDocument(new PathRef("/file1")));
        trashService.trashDocument(session.getDocument(new PathRef("/note5")));

        session.checkIn(new PathRef("/file2"), VersioningOption.MINOR, "for testing");

        TransactionHelper.commitOrRollbackTransaction();

        // wait for async jobs
        WorkManager wm = Framework.getService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());

        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @After
    public void cleanWorkingDocuments() throws Exception {
        // prevent NXP-14686 bug that prevent cleanupSession to remove version
        session.removeDocument(new PathRef(proxyPath));
    }

    protected String getDigest(IterableQueryResult docs) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Serializable> doc : docs) {
            List<String> keys = new ArrayList<>(doc.keySet());
            Collections.sort(keys);
            Map<String, Serializable> sortedMap = new LinkedHashMap<>();
            for (String key : keys) {
                Serializable value = doc.get(key);
                if (value instanceof Calendar) {
                    // ISO 8601
                    value = String.format("%tFT%<tT.%<tL%<tz", (Calendar) value);
                }
                if (coreFeature.getStorageConfiguration().isDBS()) {
                    if (key.equals("ecm:name") || key.equals("ecm:parentId")) {
                        // MongoDB has extra keys in the result set, ignore them
                        continue;
                    }
                    if (value == null) {
                        // MongoDB returns explicit nulls
                        continue;
                    }
                }
                sortedMap.put(key, value);
            }
            sb.append(sortedMap.entrySet().toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    protected void assertSameDocumentLists(IterableQueryResult expected, IterableQueryResult actual) throws Exception {
        Assert.assertEquals(getDigest(expected), getDigest(actual));
    }

    protected void compareESAndCore(String nxql) throws Exception {
        IterableQueryResult coreResult = session.queryAndFetch(nxql, NXQL.NXQL);
        EsResult esRes = ess.queryAndAggregate(new NxQueryBuilder(session).nxql(nxql).limit(20));
        IterableQueryResult esResult = esRes.getRows();
        assertSameDocumentLists(coreResult, esResult);
        coreResult.close();
        esResult.close();
    }

    @Test
    public void testSimpleSearchWithSort() throws Exception {
        compareESAndCore("select ecm:uuid, dc:title, dc:nature from Document order by ecm:uuid");
        compareESAndCore(
                "select ecm:uuid, dc:title from Document where ecm:isTrashed = 0 order by ecm:uuid");
        compareESAndCore("select ecm:uuid, dc:nature from File order by dc:nature, ecm:uuid");
        // TODO some timezone issues here...
        // compareESAndCore("select ecm:uuid, dc:issued from File order by ecm:uuid");
    }

    @Test
    public void testIteratorWithLimit() throws Exception {
        int LIMIT = 5;
        EsResult esRes = ess.queryAndAggregate(
                new NxQueryBuilder(session).nxql("select ecm:uuid From Document").limit(LIMIT));
        try (IterableQueryResult res = esRes.getRows()) {
            // the number of doc in the iterator
            Assert.assertEquals(LIMIT, res.size());
            // the total number of docs that match for the query
            Assert.assertEquals(20, ((EsResultSetImpl) res).totalSize());
        }
    }

    @Test
    public void testAggregates() throws Exception {
        AggregateDefinition aggDef = new AggregateDescriptor();
        aggDef.setId("cardinal");
        aggDef.setType(AGG_CARDINALITY);
        aggDef.setDocumentField("dc:title");

        AggregateDefinition avggDef = new AggregateDescriptor();
        avggDef.setId("average");
        avggDef.setType(AGG_AVG);
        avggDef.setDocumentField("dc:created");

        SingleValueMetricAggregate cardinality = (SingleValueMetricAggregate) AggregateFactory.create(aggDef, null);
        SingleValueMetricAggregate average = (SingleValueMetricAggregate) AggregateFactory.create(avggDef, null);

        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("SELECT * FROM Document")
                                                       .limit(0)
                                                       .addAggregate(cardinality)
                                                       .addAggregate(average)
                                                       .onlyElasticsearchResponse();
        EsResult esRes = ess.queryAndAggregate(qb);
        Assert.assertNotNull(cardinality.getValue());
    }

}
