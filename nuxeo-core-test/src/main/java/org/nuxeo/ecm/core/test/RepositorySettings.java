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
package org.nuxeo.ecm.core.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.NuxeoRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Provider;

public class RepositorySettings implements Provider<CoreSession> {

    private static final Log log = LogFactory.getLog(RepositorySettings.class);
    
    protected NuxeoRunner runner;
    protected BackendType type;
    protected String username;
    protected RepositoryInit initializer;
    protected Granularity granularity;

    protected TestRepositoryHandler repo;
    protected CoreSession session;
    
    public RepositorySettings(NuxeoRunner runner) {
        this.runner = runner;
        Description description = runner.getDescription();
        RepositoryConfig repo = description.getAnnotation(RepositoryConfig.class);
        if (repo == null) {
            repo = Defaults.of(RepositoryConfig.class);
        }
        type = repo.type();        
        username = repo.user();
        granularity = repo.cleanup(); 
        Class<? extends RepositoryInit> clazz = repo.init();
        if (clazz != RepositoryInit.class) {
            try {
                initializer = clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
    }

    public BackendType getBackendType() {
        return type;
    }

    public void setBackendType(BackendType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public RepositoryInit getInitializer() {
        return initializer;
    }
    
    public void setInitializer(RepositoryInit initializer) {
        this.initializer = initializer;
    }

    public Granularity getGranularity() {
        return granularity;
    }
    
    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }
    
    
    public void initialize() {
        try {
            RuntimeHarness harness = runner.getHarness();
            BackendType repoType = getBackendType();
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
    
    @SuppressWarnings("deprecation")
    public void shutdown() {
        if (repo != null) {
            if (session != null) {
                repo.releaseSession(session);
                session = null;
            }
            for (CoreSession cs : CoreInstance.getInstance().getSessions()) {
                CoreInstance.getInstance().close(cs);
            }
            repo.releaseRepository();
            repo = null;
        }
    }

    private String getRepoName() {
        // Small hacks since test repo name differs between implementation
        if (getBackendType() == BackendType.JCR) {
            return "demo";
        } else {
            return "test";
        }
    }
    
    public TestRepositoryHandler getRepositoryHandler() {
        if (repo == null) {
            try {
                repo = new TestRepositoryHandler(
                        getRepoName());
                repo.openRepository();
            } catch (Exception e) {
                log.error(e.toString(), e);
                return null;
            }
        }
        return repo;
    }

    public CoreSession getSession() {
        if (session == null) {
            try {
                session = getRepositoryHandler().openSessionAs(getUsername());
            } catch (Exception e) {
                log.error(e.toString(), e);
                return null;
            }
        }
        return session;
    }
    
    public CoreSession get() {
        return getSession();
    }

}
