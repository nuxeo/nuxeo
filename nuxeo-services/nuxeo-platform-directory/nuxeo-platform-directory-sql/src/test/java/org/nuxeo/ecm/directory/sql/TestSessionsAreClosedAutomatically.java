/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.sql;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, CoreFeature.class, SQLDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory:test-sql-directories-schema-override.xml")
@Deploy("org.nuxeo.ecm.directory.sql:test-sql-directories-bundle.xml")
@LogCaptureFeature.FilterWith(TestSessionsAreClosedAutomatically.CloseSessionFilter.class)
public class TestSessionsAreClosedAutomatically {

    public static class CloseSessionFilter implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LoggingEvent event) {
            if (!SQLDirectory.class.getName().equals(event.getLogger().getName())) {
                return false;
            }
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            String msg = event.getMessage().toString();
            if (!msg.startsWith("Closing a sql directory session")) {
                return false;
            }
            return true;
        }

    }

    protected Directory userDirectory;

    protected @Inject LogCaptureFeature.Result caughtEvents;

    @Before
    public void fetchUserDirectory() {
        userDirectory = Framework.getService(DirectoryService.class).getDirectory("userDirectory");
        Assert.assertNotNull(userDirectory);
    }

    @Test
    public void hasNoWarns() throws NoLogCaptureFilterException {
        try (Session session = userDirectory.getSession()) {
            // do nothing
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        Assert.assertTrue(caughtEvents.getCaughtEvents().isEmpty());
    }

    @Test
    public void hasWarnsOnCommit() throws NoLogCaptureFilterException {
        userDirectory.getSession();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        caughtEvents.assertHasEvent();
    }

    @Test
    public void hasWarnsOnRollback() throws NoLogCaptureFilterException {
        userDirectory.getSession();
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        caughtEvents.assertHasEvent();
    }
}
