package org.nuxeo.ecm.core.test;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class,
        LogCaptureFeature.class })
@TransactionalConfig(autoStart = false)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@LogCaptureFeature.FilterWith(QueryResultsAreAutomaticallyClosedTest.LogFilter.class)
public class QueryResultsAreAutomaticallyClosedTest {

    public static class LogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            if (!SessionImpl.class.getName().equals(event.getLoggerName())) {
                return false;
            }
            if (!SessionImpl.QueryResultContextException.class.isAssignableFrom(event.getThrowableInformation().getThrowable().getClass())) {
                return false;
            }
            return true;
        }
    }

    @Inject
    protected RepositorySettings settings;

    @Inject
    protected LogCaptureFeature.Result logCaptureResults;

    @Test
    public void testWithoutTransaction() throws Exception {
        IterableQueryResult results;
        try (CoreSession session = settings.openSessionAsSystemUser()) {
            results = session.queryAndFetch("SELECT * from Document", "NXQL");
        }
        Assert.assertFalse(results.isLife());
        logCaptureResults.assertHasEvent();
    }

    // needs a JCA connection for this to work
    @Ignore
    @Test
    public void testTransactional() throws Exception {
        TransactionHelper.startTransaction();
        try (CoreSession session = settings.openSessionAsSystemUser()) {
            IterableQueryResult results = session.queryAndFetch(
                    "SELECT * from Document", "NXQL");
            TransactionHelper.commitOrRollbackTransaction();
            logCaptureResults.assertHasEvent();
            Assert.assertFalse(results.isLife());
        }
    }

    protected static class NestedQueryRunner extends UnrestrictedSessionRunner {

        public NestedQueryRunner(String reponame) {
            super(reponame);
        }

        protected IterableQueryResult result;

        @Override
        public void run() throws ClientException {
            result = session.queryAndFetch("SELECT * from Document", "NXQL");
        }

    }

    @Test
    public void testNested() throws Exception {
        TransactionHelper.startTransaction();
        IterableQueryResult mainResults;
        try (CoreSession main = settings.openSessionAsSystemUser()) {
            mainResults = main.queryAndFetch(
                    "SELECT * from Document", "NXQL");
            NestedQueryRunner runner = new NestedQueryRunner(
                    settings.repositoryName);
            runner.runUnrestricted();
            Assert.assertFalse(runner.result.isLife());
            Assert.assertTrue(mainResults.isLife());
            logCaptureResults.assertHasEvent();
            logCaptureResults.clear();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Assert.assertFalse(mainResults.isLife());
        logCaptureResults.assertHasEvent();
    }
}
