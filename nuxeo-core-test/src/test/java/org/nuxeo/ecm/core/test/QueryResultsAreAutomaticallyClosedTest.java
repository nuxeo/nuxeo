package org.nuxeo.ecm.core.test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, CoreFeature.class})
@TransactionalConfig(autoStart=false)
@RepositoryConfig(init=DefaultRepositoryInit.class)
public class QueryResultsAreAutomaticallyClosedTest {

    
    @Inject RepositorySettings settings;
    
    @Test
    public void testAutoCommit() throws ClientException {
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        settings.getRepositoryHandler().releaseSession(session);
        Assert.assertFalse(results.isLife());
    }
    
    @Test
    public void testTransactional() throws ClientException {
        TransactionHelper.startTransaction();
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        TransactionHelper.commitOrRollbackTransaction();
        try {
            Assert.assertFalse(results.isLife());
        } finally {
            settings.getRepositoryHandler().releaseSession(session);            
        }
    }
    
    protected static class NestedQueryRunner extends UnrestrictedSessionRunner {

        public NestedQueryRunner(String reponame) {
            super(reponame);
        }
        
        protected IterableQueryResult result;
        
        @Override
        public void run() throws ClientException {
             result = session.queryAndFetch(
                    "SELECT * from Document", "NXQL");
        }
        
    }
    @Test
    public void testNested() throws ClientException {
        TransactionHelper.startTransaction();
        CoreSession main = settings.openSessionAs("toto");
        IterableQueryResult mainResults = main.queryAndFetch(
                "SELECT * from Document", "NXQL");
        IterableQueryResult subResults;
        NestedQueryRunner runner = new NestedQueryRunner(settings.repositoryName);
        runner.runUnrestricted();
        try {
            Assert.assertFalse(runner.result.isLife());
            Assert.assertTrue(mainResults.isLife());
        } finally {
            settings.getRepositoryHandler().releaseSession(main);
        }
    }
}
