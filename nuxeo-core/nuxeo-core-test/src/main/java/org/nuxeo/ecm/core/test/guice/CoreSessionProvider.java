/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test.guice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.ecm.core.test.NuxeoCoreRunner;
import org.nuxeo.ecm.core.test.TestRepositoryHandler;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CoreSessionProvider implements Provider<CoreSession> {

    private static final Log log = LogFactory.getLog(CoreSessionProvider.class);

    private BackendType repoType;

    @Inject
    public CoreSessionProvider(BackendType repoType, RuntimeHarness harness,
            SchemaManager sm) {
        assert sm != null;
        try {
            this.repoType = repoType;
            // the core bundles
            harness.deployBundle("org.nuxeo.ecm.core.api");
            harness.deployBundle("org.nuxeo.ecm.core.event");
            harness.deployBundle("org.nuxeo.ecm.core");

            if (repoType == BackendType.JCR) {
                log.info("Deploying a JCR repo implementation");
                harness.deployBundle("org.nuxeo.ecm.core.jcr");
                harness.deployBundle("org.nuxeo.ecm.core.jcr-connector");

            } else {
                log.info("Deploying a VCS repo implementation");
                harness.deployBundle("org.nuxeo.ecm.core.storage.sql");

                // TODO: should use a factory
                DatabaseHelper dbHelper;
                if (repoType == BackendType.H2) {
                    log.info("VCS relies on H2");
                    dbHelper = DatabaseH2.DATABASE;
                } else {
                    log.info("VCS relies on Postgres");
                    dbHelper = DatabasePostgreSQL.DATABASE;
                }
                harness.deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                        dbHelper.getDeploymentContrib());
                dbHelper.setUp();

                if (dbHelper instanceof DatabasePostgreSQL) {
                    Class.forName("org.postgresql.Driver");
                    String url = String.format("jdbc:postgresql://%s:%s/%s",
                            "localhost", "5432", "nuxeojunittests");
                    Connection connection = DriverManager.getConnection(url,
                            "postgres", "");
                    Statement st = connection.createStatement();
                    String sql = "CREATE LANGUAGE plpgsql";
                    st.execute(sql);
                    st.close();
                    connection.close();
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private String getRepoName() {
        // Small hacks since test repo name differs between implementation
        if (repoType == BackendType.JCR) {
            return "demo";
        } else {
            return "test";
        }
    }

    public CoreSession get() {
        try {
            TestRepositoryHandler repo = new TestRepositoryHandler(
                    getRepoName());
            repo.openRepository();
            CoreSession session = repo.openSessionAs(NuxeoCoreRunner.getSettings().getRepositoryUsername());
            NuxeoCoreRunner.onSessionInjected(session);
            return session;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

}
