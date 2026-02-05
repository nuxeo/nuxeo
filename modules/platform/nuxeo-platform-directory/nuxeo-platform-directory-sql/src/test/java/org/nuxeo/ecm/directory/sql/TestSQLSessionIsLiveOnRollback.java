/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.directory.sql;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Regression test for NXP-33503: SQLSession.isLive() returns false during transaction rollback when using DBCP shared
 * connections.
 *
 * @since 2025.15
 */
@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, CoreFeature.class, SQLDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory:test-sql-directories-schema-override.xml")
@Deploy("org.nuxeo.ecm.directory.sql:test-sql-directories-bundle.xml")
@LogCaptureFeature.FilterWith(TestSQLSessionIsLiveOnRollback.CloseSessionFilter.class)
public class TestSQLSessionIsLiveOnRollback {

    public static class CloseSessionFilter implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LogEvent event) {
            if (!SQLDirectory.class.getName().equals(event.getLoggerName())) {
                return false;
            }
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            String msg = event.getMessage().getFormattedMessage();
            return msg.startsWith("Closing a sql directory session");
        }
    }

    protected Directory userDirectory;

    @Inject
    protected LogCaptureFeature.Result caughtEvents;

    @Before
    public void fetchUserDirectory() {
        userDirectory = Framework.getService(DirectoryService.class).getDirectory("userDirectory");
        Assert.assertNotNull(userDirectory);
    }

    /**
     * Tests that an unclosed directory session is detected and logged during transaction rollback.
     */
    @Test
    public void testSessionIsLiveOnRollback() throws NoLogCaptureFilterException {
        userDirectory.getSession();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        caughtEvents.assertHasEvent();
    }
}
