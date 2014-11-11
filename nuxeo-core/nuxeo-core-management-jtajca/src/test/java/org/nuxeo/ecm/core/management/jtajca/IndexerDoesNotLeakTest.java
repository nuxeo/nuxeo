package org.nuxeo.ecm.core.management.jtajca;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ JtajcaManagementFeature.class})
@RepositoryConfig(cleanup=Granularity.METHOD, repositoryFactoryClass=PoolingRepositoryFactory.class, singleDatasource="jdbc/NuxeoTestDS")
public class IndexerDoesNotLeakTest {

    @Inject CoreSession repo;

    @Inject WorkManager works;

    @Inject @Named("repository/test")
    ConnectionPoolMonitor repoMonitor;

    @Inject @Named("jdbc/repository_test")
    ConnectionPoolMonitor dbMonitor;

    @Test
    public void indexerWorkDoesNotLeak() throws ClientException, InterruptedException {
        int repoCount = repoMonitor.getConnectionCount();
        int dbCount = dbMonitor.getConnectionCount();
        DocumentModel doc = repo.createDocumentModel("/", "note", "Note");
        repo.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        works.awaitCompletion(10, TimeUnit.SECONDS);
        assertThat(repoCount, is(repoMonitor.getConnectionCount()));
        assertThat(dbCount, is(dbMonitor.getConnectionCount()));

    }
}
