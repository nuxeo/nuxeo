/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

import junit.framework.AssertionFailedError;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class CanRollbackDatabaseTest {

    @Inject
    protected RuntimeHarness harness;

    @BeforeClass
    public static void createTable() throws NamingException, SQLException {
        RuntimeContext context = ((OSGiRuntimeService) Framework.getRuntime()).getContext("org.nuxeo.runtime.datasource");
        context.deploy("ds-contrib.xml");
        try {
            DataSource ds = DataSourceHelper.getDataSource("jdbc/canrollback");
            try (Connection db = ds.getConnection()) {
                try (Statement st = db.createStatement()) {
                    st.execute("CREATE TABLE footest(a INTEGER PRIMARY KEY)");
                }
            }
        } finally {
            context.undeploy("ds-contrib.xml");
        }
    }

    // don't use LocalDeploy, it fails on SQL Server (deploy is done on a
    // connection with tx)
    @Test(expected = SQLException.class)
    public void testFatalRollback() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.management.jtajca.test", "ds-contrib-with-fatal.xml");
        try {
            insertWrongReference();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.management.jtajca.test", "ds-contrib-with-fatal.xml");
        }
    }

    @Test(expected = SQLException.class)
    public void testNoFatalRollback() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.core.management.jtajca.test", "ds-contrib.xml");
        try {
            insertWrongReference();
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.management.jtajca.test", "ds-contrib.xml");
        }
    }

    private void insertWrongReference() throws NamingException, SQLException, AssertionFailedError {
        DataSource ds = DataSourceHelper.getDataSource("jdbc/canrollback");
        try (Connection db = ds.getConnection()) {
            try (Statement st = db.createStatement()) {
                st.execute("INSERT INTO footest (a) VALUES (0)");
                st.execute("INSERT INTO footest (a) VALUES (1)");
                st.execute("INSERT INTO footest (a) VALUES (1)");
            }
        } catch (SQLException cause) {
            TransactionHelper.setTransactionRollbackOnly();
            throw cause;
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            try (Connection db = ds.getConnection()) {
                try (Statement st = db.createStatement()) {
                    try (ResultSet rs = st.executeQuery("SELECT a FROM footest WHERE a = 0")) {
                        if (rs.next()) {
                            throw new AssertionFailedError("connection was not rollbacked");
                        }
                    }
                }
            }
        }
    }

}
