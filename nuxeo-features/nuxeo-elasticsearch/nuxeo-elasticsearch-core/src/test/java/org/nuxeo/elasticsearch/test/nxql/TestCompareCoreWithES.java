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
 *     Thierry Delprat
 */
package org.nuxeo.elasticsearch.test.nxql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
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
public class TestCompareCoreWithES {

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
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc.setPropertyValue("dc:subjects",
                    (i % 2 == 0) ? new String[] { "Subjects1" } : new String[] { "Subjects1", "Subjects2" });
            doc.setPropertyValue("relatedtext:relatedtextresources",
                    (Serializable) Arrays.asList(Collections.singletonMap("relatedtextid", "123")));
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

        DocumentModel orphan = session.createDocumentModel(null, "orphan", "File");
        orphan.setPropertyValue("dc:title", "orphan document");
        session.createDocument(orphan);

        trashService.trashDocument(session.getDocument(new PathRef("/file1")));
        trashService.trashDocument(session.getDocument(new PathRef("/note5")));

        session.checkIn(new PathRef("/file2"), VersioningOption.MINOR, "for testing");

        TransactionHelper.commitOrRollbackTransaction();

        // wait for async jobs
        Framework.getService(WorkManager.class).awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        TransactionHelper.startTransaction();
    }

    @Before
    public void setupIndex() throws Exception {
        esa.initIndexes(true);
    }

    @After
    public void cleanWorkingDocuments() throws Exception {
        // prevent NXP-14686 bug that prevent cleanupSession to remove version
        session.removeDocument(new PathRef(proxyPath));
    }

    protected String getDigest(DocumentModelList docs) {
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : docs) {
            String nameOrTitle = doc.getName();
            if (nameOrTitle == null || nameOrTitle.isEmpty()) {
                nameOrTitle = doc.getTitle();
            }
            sb.append(nameOrTitle);
            sb.append("[" + doc.getPropertyValue("dc:nature") + "]");
            sb.append("[" + doc.getPropertyValue("dc:rights") + "]");
            sb.append(",");
        }
        return sb.toString();
    }

    protected void assertSameDocumentLists(DocumentModelList expected, DocumentModelList actual) throws Exception {
        Assert.assertEquals(expected.size(), actual.size());
        // quick check for some props for better failure messages
        for (int i = 0; i < expected.size(); i++) {
            DocumentModel expecteDdoc = expected.get(i);
            DocumentModel actualDoc = actual.get(i);
            for (String xpath : Arrays.asList("dc:title", "dc:nature", "dc:rights", "dc:subjects",
                    "relatedtext:relatedtextresources")) {
                Serializable expectedValue = getProperty(expecteDdoc, xpath);
                Serializable actualValue = getProperty(actualDoc, xpath);
                Assert.assertEquals(xpath, expectedValue, actualValue);
            }
        }
        Assert.assertEquals(getDigest(expected), getDigest(actual));
    }

    protected Serializable getProperty(DocumentModel doc, String xpath) {
        Serializable value;
        try {
            value = doc.getPropertyValue(xpath);
        } catch (PropertyNotFoundException e) {
            value = "__NOTFOUND__";
        }
        if (value instanceof Object[]) {
            value = (Serializable) Arrays.asList(((Object[]) value));
        }
        if (value instanceof List && ((List<?>) value).isEmpty()) {
            value = null;
        }
        return value;
    }

    protected void dump(DocumentModelList docs) {
        for (DocumentModel doc : docs) {
            System.out.println(doc);
        }
    }

    protected void compareESAndCore(String nxql) throws Exception {

        DocumentModelList coreResult = session.query(nxql);
        NxQueryBuilder nxQueryBuilder = new NxQueryBuilder(session).nxql(nxql).limit(30);
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                nxQueryBuilder = nxQueryBuilder.fetchFromElasticsearch();
            }
            DocumentModelList esResult = ess.query(nxQueryBuilder);
            try {
                assertSameDocumentLists(coreResult, esResult);
            } catch (AssertionError e) {
                // System.out.println("Error while executing " + nxql);
                // System.out.println("Core result : ");
                // dump(coreResult);
                // System.out.println("elasticsearch result : ");
                // dump(esResult);
                // e.printStackTrace();
                throw e;
            }
        }
    }

    protected void testQueries(String[] testQueries) throws Exception {
        for (String nxql : testQueries) {
            // System.out.println("test " + nxql);
            compareESAndCore(nxql);
        }
    }

    @Test
    public void testSimpleSearchWithSort() throws Exception {
        testQueries(new String[] { "select * from Document order by dc:title, dc:created",
                "select * from Document where ecm:isTrashed = 0 order by dc:title",
                "select * from File order by dc:title", });
    }

    @Test
    public void testSearchOnProxies() throws Exception {
        testQueries(new String[] { "select * from Document where ecm:isProxy=0 order by dc:title",
                "select * from Document where ecm:isProxy=1 order by dc:title", });
    }

    @Test
    public void testSearchOnVersions() throws Exception {
        testQueries(new String[] { "select * from Document where ecm:isVersion = 0 order by dc:title",
                "select * from Document where ecm:isVersion = 1 order by dc:title",
                "select * from Document where ecm:isCheckedInVersion = 0 order by dc:title",
                "select * from Document where ecm:isCheckedInVersion = 1 order by dc:title",
                // TODO: fix, ES results sounds correct
                // "select * from Document where ecm:isCheckedIn = 0 order by dc:title",
                // "select * from Document where ecm:isCheckedIn = 1 order by dc:title"
        });
    }

    @Test
    public void testSearchOnTypes() throws Exception {
        testQueries(new String[] { "select * from File order by dc:title", "select * from Folder order by dc:title",
                "select * from Note order by dc:title",
                "select * from Note where ecm:primaryType IN ('Note', 'Folder') order by dc:title",
                "select * from Document where ecm:mixinType = 'Folderish' order by dc:title",
                "select * from Document where ecm:mixinType != 'Folderish' order by dc:title", });
    }

    @Test
    public void testSearchWithLike() throws Exception {
        // Validate that NXP-14338 is fixed
        testQueries(new String[] { "SELECT * FROM Document WHERE dc:title LIKE 'nomatch%'",
                "SELECT * from Document WHERE dc:title LIKE 'File%' ORDER BY dc:title",
                "SELECT * from Document WHERE dc:title LIKE '%ile%' ORDER BY dc:title",
                "SELECT * from Document WHERE dc:title NOT LIKE '%ile%' ORDER BY dc:title",
                "SELECT * from Document WHERE dc:title NOT LIKE '%i%e%' ORDER BY dc:title", });
    }

    @Test
    public void testSearchWithStartsWith() throws Exception {
        testQueries(new String[] {
                // Note that there are differnces between ES and VCS:
                // ES version document has a path and is searchable with startswith
                "SELECT * from Document WHERE ecm:path STARTSWITH '/nomatch' ORDER BY dc:title",
                "SELECT * from Document WHERE ecm:path STARTSWITH '/folder' AND ecm:path != '/folder' ORDER BY dc:title",
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/' AND ecm:isVersion = 0 ORDER BY dc:title", });
    }

    @Test
    public void testSearchWithAncestorId() throws Exception {
        DocumentModel folder = session.getDocument(new PathRef("/folder"));
        Assert.assertNotNull(folder);
        String fid = folder.getId();
        testQueries(new String[] { "SELECT * from Document WHERE ecm:ancestorId = 'non-esisting-id' ORDER BY dc:title",
                "SELECT * from Document WHERE ecm:ancestorId != 'non-existing-id' ORDER BY dc:title",
                "SELECT * FROM Document WHERE ecm:ancestorId = '" + fid + "' ORDER BY dc:title", });
    }

}
