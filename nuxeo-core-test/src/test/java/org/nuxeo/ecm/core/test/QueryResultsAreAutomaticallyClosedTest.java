package org.nuxeo.ecm.core.test;

import junit.framework.Assert;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
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
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, CoreFeature.class})
@TransactionalConfig(autoStart=false)
@RepositoryConfig(init=DefaultRepositoryInit.class)
public class QueryResultsAreAutomaticallyClosedTest {

    
    protected @Inject RepositorySettings settings;
    
    protected boolean seenWarn = false;
    
    protected Logger rootLogger = Logger.getRootLogger();
    
    protected Appender logAppender =  new AppenderSkeleton() {
        
        @Override
        public boolean requiresLayout() {
            return false;
        }
        
        @Override
        public void close() {
            
        }
        
        @Override
        protected void append(LoggingEvent event) {
            if (!Level.WARN.equals(event.getLevel())) {
                return;
            }
            if (!CoreSession.class.getName().equals(event.getLoggerName())) {
                return;
            }
            if (!AbstractSession.QueryAndFetchExecuteContextException.class.isAssignableFrom(event.getThrowableInformation().getThrowable().getClass())) {
                return;
            }
            seenWarn = true;
        }
    };
    
    @Before
    public void addLogAppender() {
        seenWarn = false;
        rootLogger.addAppender(logAppender);
    }
    
    @After
    public void removeLogAppender() {
        rootLogger.removeAppender(logAppender);
    }
    
    @Test
    public void testAutoCommit() throws ClientException {
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        settings.getRepositoryHandler().releaseSession(session);
        Assert.assertFalse(results.isLife());
        Assert.assertTrue(seenWarn);
    }
    
    @Test
    public void testTransactional() throws ClientException {
        TransactionHelper.startTransaction();
        CoreSession session = settings.openSessionAsSystemUser();
        IterableQueryResult results = session.queryAndFetch(
                "SELECT * from Document", "NXQL");
        TransactionHelper.commitOrRollbackTransaction();
        try {
            Assert.assertTrue(seenWarn);
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
        CoreSession main = settings.openSessionAsSystemUser();
        IterableQueryResult mainResults = main.queryAndFetch(
                "SELECT * from Document", "NXQL");
        NestedQueryRunner runner = new NestedQueryRunner(settings.repositoryName);
        runner.runUnrestricted();
        try {
            Assert.assertFalse(runner.result.isLife());
            Assert.assertTrue(mainResults.isLife());
            Assert.assertTrue(seenWarn);
            seenWarn = false;
        } finally {
            settings.getRepositoryHandler().releaseSession(main);
            TransactionHelper.commitOrRollbackTransaction();
        }
        Assert.assertFalse(mainResults.isLife());
        Assert.assertTrue(seenWarn);
    }
}
