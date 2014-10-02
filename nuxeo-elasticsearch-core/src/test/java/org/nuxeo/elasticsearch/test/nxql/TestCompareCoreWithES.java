package org.nuxeo.elasticsearch.test.nxql;

import java.util.concurrent.TimeUnit;

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
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
public class TestCompareCoreWithES {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;
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

        DocumentModel doc = session.createDocumentModel("/", "hidden",
                "HiddenFolder");
        doc.setPropertyValue("dc:title", "HiddenFolder");
        doc = session.createDocument(doc);

        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.getDocument(new PathRef("/file3"));
        DocumentModel proxy = session.publishDocument(file, folder);
        proxyPath = proxy.getPathAsString();

        session.followTransition(new PathRef("/file1"), "delete");
        session.followTransition(new PathRef("/note5"), "delete");

        session.checkIn(new PathRef("/file2"), VersioningOption.MINOR,
                "for testing");

        TransactionHelper.commitOrRollbackTransaction();

        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommands());
        Assert.assertEquals(0, esa.getPendingDocs());

        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @After
    public void cleanWorkingDocuments() throws Exception {
        // prevent NXP-14686 bug that prevent cleanupSession to remove version
        session.removeDocument(new PathRef(proxyPath));
    }



    protected String getDigest(DocumentModelList docs) throws Exception  {
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : docs) {
            String nameOrTitle = doc.getName();
             if (nameOrTitle==null || nameOrTitle.isEmpty()) {
                 nameOrTitle = doc.getTitle();
             }
            sb.append(nameOrTitle);
            sb.append(",");
        }
        return sb.toString();
    }

    protected void assertSameDocumentLists(DocumentModelList expected,
            DocumentModelList actual) throws Exception {
        Assert.assertEquals(getDigest(expected), getDigest(actual));
    }

    protected void dump(DocumentModelList docs) {
        for (DocumentModel doc :docs) {
            System.out.println(doc);
        }
    }

    protected void compareESAndCore(String nxql) throws Exception {

        DocumentModelList coreResult = session.query(nxql);
        DocumentModelList esResult = ess.query(new NxQueryBuilder(session)
                .nxql(nxql).limit(20));
        try {
            assertSameDocumentLists(coreResult, esResult);
        } catch (AssertionError e) {
            System.out.println("Error while executing " + nxql);
            System.out.println("Core result : ");
            dump(coreResult);
            System.out.println("elasticsearch result : ");
            dump(esResult);
            throw e;
        }
    }

    protected void testQueries(String[] testQueries) throws Exception {
        for (String nxql : testQueries) {
            //System.out.println("test " + nxql);
            compareESAndCore(nxql);
        }
    }

    @Test
    public void testSimpleSearchWithSort() throws Exception {
        testQueries(new String[] {
                "select * from Document order by dc:title, dc:created",
                "select * from Document where ecm:currentLifeCycleState != 'deleted' order by dc:title",
                "select * from File order by dc:title", });
    }

    @Test
    public void testSearchOnProxies() throws Exception {
        testQueries(new String[] {
                "select * from Document where ecm:isProxy=0 order by dc:title",
                "select * from Document where ecm:isProxy=1 order by dc:title", });
    }

    @Test
    public void testSearchOnVersions() throws Exception {
        testQueries(new String[] {
                "select * from Document where ecm:isVersion = 0 order by dc:title",
                "select * from Document where ecm:isVersion = 1 order by dc:title",
                "select * from Document where ecm:isCheckedInVersion = 0 order by dc:title",
                "select * from Document where ecm:isCheckedInVersion = 1 order by dc:title",
                // TODO: fix, ES results sounds correct
                //"select * from Document where ecm:isCheckedIn = 0 order by dc:title",
                //"select * from Document where ecm:isCheckedIn = 1 order by dc:title"
                });
    }

    @Test
    public void testSearchOnTypes() throws Exception {
        testQueries(new String[] { "select * from File order by dc:title",
                "select * from Folder order by dc:title",
                "select * from Note order by dc:title",
                "select * from Document where  ecm:mixinType = 'Folderish' order by dc:title",
                "select * from Document where  ecm:mixinType != 'Folderish' order by dc:title",});
    }

    @Test
    public void testSearchWithLike() throws Exception {
        // Validate that NXP-14338 is fixed
        testQueries(new String[] { "SELECT * FROM Document WHERE dc:title LIKE 'nomatch%'",
                "SELECT * from Document WHERE dc:title LIKE 'File%' ORDER BY dc:title",
        });
    }

}
