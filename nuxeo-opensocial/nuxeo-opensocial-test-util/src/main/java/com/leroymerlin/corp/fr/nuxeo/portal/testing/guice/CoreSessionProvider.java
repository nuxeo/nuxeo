package com.leroymerlin.corp.fr.nuxeo.portal.testing.guice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.RepoType;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRepositoryHandler;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

public class CoreSessionProvider implements Provider<CoreSession> {

    private RepoType repoType;

    Logger logger = LoggerFactory.getLogger(CoreSessionProvider.class);

    @Inject
    public CoreSessionProvider(TestRuntimeHarness harness, SchemaManager sm) {
        assert sm != null;
        try {
            repoType = NuxeoRunner.getInstance().getRepoType();

            // the core bundles

            harness.deployBundle("org.nuxeo.ecm.core.api");
            harness.deployBundle("org.nuxeo.ecm.core.event");
            harness.deployBundle("org.nuxeo.ecm.core");

            DatabaseHelper dbHelper = null;
            if (repoType == RepoType.JCR) {
                logger.info("Deploying a JCR repo implementation");
                harness.deployBundle("org.nuxeo.ecm.core.jcr");
                harness.deployBundle("org.nuxeo.ecm.core.jcr-connector");

            } else {
                logger.info("Deploying a VCS repo implementation");
                harness.deployBundle("org.nuxeo.ecm.core.storage.sql");

                // TODO: should use a factory
                if (repoType == RepoType.H2) {
                    logger.info("VCS relies on H2");
                    dbHelper = DatabaseHelper.DATABASE;
                } else {
                    logger.info("VCS relies on Postgres");
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
            e.printStackTrace();
        }

    }

    private String getRepoName() {
        // Small hacks since test repo name differs between implementation
        if (repoType == RepoType.JCR) {
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
            return repo.openSessionAs(NuxeoRunner.getInstance().getSettings()
                    .getRepoUsername());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
