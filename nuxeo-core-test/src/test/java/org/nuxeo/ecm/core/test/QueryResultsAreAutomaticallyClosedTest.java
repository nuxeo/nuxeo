package org.nuxeo.ecm.core.test;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
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
            if (!CoreSession.class.getName().equals(event.getLoggerName())) {
                return false;
            }
            if (!AbstractSession.QueryAndFetchExecuteContextException.class.isAssignableFrom(event.getThrowableInformation().getThrowable().getClass())) {
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
    public void testAutoCommit() throws Exception {
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        settings.getRepositoryHandler().releaseSession(session);
        Assert.assertFalse(results.isLife());
        logCaptureResults.assertHasEvent();
    }

    @Test
    public void testTransactional() throws Exception {
        TransactionHelper.startTransaction();
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        TransactionHelper.commitOrRollbackTransaction();
        try {
            logCaptureResults.assertHasEvent();
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
            result = session.queryAndFetch("SELECT * from Document", "NXQL");
        }

    }

    @Test
    public void testNested() throws Exception {
        TransactionHelper.startTransaction();
        CoreSession main = settings.openSessionAsSystemUser();
        IterableQueryResult mainResults = main.queryAndFetch(
                "SELECT * from Document", "NXQL");
        NestedQueryRunner runner = new NestedQueryRunner(
                settings.repositoryName);
        runner.runUnrestricted();
        try {
            Assert.assertFalse(runner.result.isLife());
            Assert.assertTrue(mainResults.isLife());
            logCaptureResults.assertHasEvent();
            logCaptureResults.clear();
        } finally {
            settings.getRepositoryHandler().releaseSession(main);
            TransactionHelper.commitOrRollbackTransaction();
        }
        Assert.assertFalse(mainResults.isLife());
        logCaptureResults.assertHasEvent();
    }
}
