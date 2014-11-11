/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import java.net.URL;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.DatabaseHelperFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.runtime.model.persistence.Contribution;
import org.nuxeo.runtime.model.persistence.fs.ContributionLocation;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.osgi.framework.Bundle;

import com.google.inject.Provider;

/**
 * Repository configuration that can be set using {@link RepositoryConfig} annotations.
 * <p>
 * If you are modifying fields in this class do not forget to update the
 * {@link RepositorySettings#importSettings(RepositorySettings) method.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RepositorySettings implements Provider<CoreSession> {

    private static final Log log = LogFactory.getLog(RepositorySettings.class);

    protected FeaturesRunner runner;

    protected BackendType type;

    protected String repositoryName;

    protected String databaseName;

    protected String username;

    protected RepositoryInit repoInitializer;

    protected Granularity granularity;

    protected DatabaseHelperFactory databaseFactory;

    protected TestRepositoryHandler repo;

    protected CoreSession session;

    /**
     * Do not use this ctor - it will be used by {@link MultiNuxeoCoreRunner}.
     */
    protected RepositorySettings() {
        importAnnotations(Defaults.of(RepositoryConfig.class));
    }

    protected RepositorySettings(RepositoryConfig config) {
        importAnnotations(config);
    }

    protected RepositorySettings(FeaturesRunner runner, RepositoryConfig config) {
        this.runner = runner;
        importAnnotations(config);
    }

    public RepositorySettings(FeaturesRunner runner) {
        this.runner = runner;
        Description description = runner.getDescription();
        RepositoryConfig repo = description.getAnnotation(RepositoryConfig.class);
        if (repo == null) {
            repo = Defaults.of(RepositoryConfig.class);
        }
        importAnnotations(repo);
    }

    public void importAnnotations(RepositoryConfig repo) {
        type = repo.type();
        repositoryName = repo.repositoryName();
        databaseName = repo.databaseName();
        username = repo.user();
        granularity = repo.cleanup();
        databaseFactory = newInstance(repo.factory());
        repoInitializer = newInstance(repo.init());
    }

    protected <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new Error("Cannot instanciate " + clazz.getSimpleName(), e);
        }
    }

    public void importSettings(RepositorySettings settings) {
        shutdown();
        // override only the user name and the type.
        // overriding initializer and granularity may broke tests that are using
        // specific initializers
        RepositoryConfig defaultConfig = Defaults.of(RepositoryConfig.class);
        if (defaultConfig.type() != settings.type) {
            type = settings.type;
        }
        username = settings.username;
        repositoryName = settings.repositoryName;
        databaseName = settings.databaseName;
    }

    public BackendType getBackendType() {
        return type;
    }

    public void setBackendType(BackendType type) {
        this.type = type;
    }

    public String getName() {
        return repositoryName;
    }

    public void setName(String name) {
        this.repositoryName = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public RepositoryInit getInitializer() {
        return repoInitializer;
    }

    public void setInitializer(RepositoryInit initializer) {
        this.repoInitializer = initializer;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }

    public void initialize() {
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            log.info("Deploying a VCS repo implementation");
            // DatabaseHelper.DATABASE is already initialized so we should do:
            // DatabaseHelper dbHelper = DatabaseHelper.DATABASE;
            // dbHelper.setRepositoryName(repositoryName);
            // but let's keep the old method and replace DatabaseHelper.DATABASE
            DatabaseHelper dbHelper = databaseFactory.getHelper(type,
                    databaseName, repositoryName);
            DatabaseHelper.DATABASE = dbHelper;
            dbHelper.setUp();
            OSGiAdapter osgi = harness.getOSGiAdapter();
            Bundle bundle = osgi.getRegistry().getBundle(
                    "org.nuxeo.ecm.core.storage.sql.test");
            URL contribURL = bundle.getEntry(dbHelper.getDeploymentContrib());
            Contribution contrib = new ContributionLocation(repositoryName,
                    contribURL);
            harness.getContext().deploy(contrib);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    @SuppressWarnings("deprecation")
    public void shutdown() {
        try {
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
        } finally {
            try {
                DatabaseHelper.DATABASE.tearDown();
            } catch (SQLException e) {
                throw new RuntimeException("Cannot release database", e);
            }
        }
    }

    public TestRepositoryHandler getRepositoryHandler() {
        if (repo == null) {
            try {
                repo = new TestRepositoryHandler(repositoryName);
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

    @Override
    public CoreSession get() {
        return getSession();
    }

}
