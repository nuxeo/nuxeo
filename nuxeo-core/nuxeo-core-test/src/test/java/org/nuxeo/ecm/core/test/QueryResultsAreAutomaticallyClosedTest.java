/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.storage.sql.ra.ConnectionImpl;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@LogCaptureFeature.FilterWith(QueryResultsAreAutomaticallyClosedTest.LogFilter.class)
public class QueryResultsAreAutomaticallyClosedTest {

    private static final String VCS_CLOSING_WARN = "Closing a query results for you, check stack trace for allocating point";

    public static class LogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            if (!ConnectionImpl.class.getName().equals(event.getLoggerName())) {
                return false;
            }
            return ConnectionImpl.QueryResultContextException.class.isAssignableFrom(event.getThrown().getClass());
        }
    }

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResults;

    protected void assertWarnInLogs() throws NoLogCaptureFilterException {
        if (coreFeature.getStorageConfiguration().isVCS()) {
            logCaptureResults.assertHasEvent();
            LogEvent event = logCaptureResults.getCaughtEvents().get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertEquals(VCS_CLOSING_WARN, event.getMessage().getFormattedMessage());
        }
    }

    @Test
    public void testWithoutTransaction() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            coreFeature.openCoreSessionSystem();
            fail("Should not allow creation of CoreSession outside a transaction");
        } catch (NuxeoException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Cannot create a CoreSession outside a transaction"));
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    // needs a JCA connection for this to work
    @Test
    public void testTransactional() throws Exception {
        try (CloseableCoreSession session = coreFeature.openCoreSessionSystem()) {
            IterableQueryResult results = session.queryAndFetch("SELECT * from Document", "NXQL");
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            assertFalse(results.mustBeClosed());
            assertWarnInLogs();
        }
    }

    protected static class NestedQueryRunner extends UnrestrictedSessionRunner {

        public NestedQueryRunner(String reponame) {
            super(reponame);
        }

        protected IterableQueryResult result;

        @Override
        public void run() {
            result = session.queryAndFetch("SELECT * from Document", "NXQL");
        }

    }

    @Test
    public void testNested() throws Exception {
        IterableQueryResult mainResults;
        try (CloseableCoreSession main = coreFeature.openCoreSessionSystem()) {
            NestedQueryRunner runner = new NestedQueryRunner(main.getRepositoryName());
            mainResults = main.queryAndFetch("SELECT * from Document", "NXQL");
            runner.runUnrestricted();
            assertFalse(runner.result.mustBeClosed());
            if (coreFeature.getStorageConfiguration().isVCS()) {
                assertTrue(mainResults.mustBeClosed());
            }
            assertWarnInLogs();
            logCaptureResults.clear();
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        assertFalse(mainResults.mustBeClosed());
        assertWarnInLogs();
    }
}
