/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import org.apache.geronimo.connector.outbound.GeronimoConnectionEventListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.Assertions;
import org.h2.tools.Server;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;
import org.nuxeo.runtime.datasource.TestValidateConnection.CaptureValidationErrors;
import org.nuxeo.runtime.datasource.TestValidateConnection.ReportException.CaughtSite;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoValidationSupport;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@TransactionalConfig(autoStart = false)
@Features({ TransactionalFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.runtime.datasource:sql-validate-datasource-contrib.xml")
@LogCaptureFeature.FilterWith(CaptureValidationErrors.class)
public class TestValidateConnection {

    @Inject
    PooledDataSourceRegistry registry;

    @Test
    public void testNoValidation() throws SQLException {
        try {
            testPooled("no-valid");
            throw new AssertionError("didn't caught connection error");
        } catch (ReportException cause) {
            Assert.assertEquals(cause.site, CaughtSite.onUse);
        }
    }

    public static class CaptureValidationErrors implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LogEvent event) {
            return acceptValidationError(event) || acceptConnectionErrors(event);
        }

        boolean acceptValidationError(LogEvent event) {
            if (event.getLevel() != Level.ERROR) {
                return false;
            }
            return event.getLoggerName().equals(NuxeoValidationSupport.class.getName());
        }

        boolean acceptConnectionErrors(LogEvent event) {
            if (event.getLevel() != Level.WARN) {
                return false;
            }
            return event.getLoggerName().equals(GeronimoConnectionEventListener.class.getName());
        }
    }

    @Inject
    LogCaptureFeature.Result events;

    @Test
    public void testSQLValidation() throws SQLException, ReportException {
        testPooled("sql-valid");
    }

    @Test
    public void testQuerySQLValidation() throws SQLException, ReportException {
        testPooled("query-valid");
    }

    protected void testPooled(String name) throws ReportException, SQLException {
        Server server = Server.createTcpServer("-tcpAllowOthers").start();
        String jdbcName = "jdbc/".concat(name);
        PooledDataSource ds = registry.getPool(jdbcName, PooledDataSource.class);
        try {
            checkPooledConnection(ds);
            server.stop();
            server.start();
            checkPooledConnection(ds);
        } finally {
            server.stop();
        }
        Assertions.assertThat(NuxeoContainer.getConnectionManager(jdbcName).listActive()).hasSize(0);
    }

    static class ReportException extends Exception {
        static final long serialVersionUID = 1L;

        enum CaughtSite {
            onBorrow, onUse, onReturn
        }

        final CaughtSite site;

        ReportException(CaughtSite site, SQLException cause) {
            super(cause);
            this.site = site;
        }
    }

    protected void checkPooledConnection(PooledDataSource ds) throws ReportException {
        TransactionHelper.startTransaction();
        try (Connection connection = ds.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            } catch (SQLException cause) {
                throw new ReportException(CaughtSite.onUse, cause);
            } finally {
                try {
                    connection.close();
                } catch (SQLException cause) {
                    throw new ReportException(CaughtSite.onReturn, cause);
                }
            }
        } catch (SQLException cause) {
            throw new ReportException(CaughtSite.onBorrow, cause);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

}
