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

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.annotations.BackendType;
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
import org.nuxeo.runtime.test.runner.ServiceProvider;
import org.osgi.framework.Bundle;

import com.google.inject.Scope;

/**
 * Repository configuration that can be set using {@link RepositoryConfig} annotations.
 * <p>
 * If you are modifying fields in this class do not forget to update the
 * {@link RepositorySettings#importSettings(RepositorySettings) method.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RepositorySettings extends ServiceProvider<CoreSession> {

    private static final Log log = LogFactory.getLog(RepositorySettings.class);

    protected FeaturesRunner runner;

    protected BackendType type;

    protected String repositoryName;

    protected String databaseName;

    protected String username;

    protected RepositoryInit repoInitializer;

    protected Granularity granularity;

    protected Class<? extends RepositoryFactory> repositoryFactoryClass;

    protected CoreSession session;

    /**
     * Do not use this ctor - it will be used by {@link MultiNuxeoCoreRunner}.
     */
    protected RepositorySettings() {
        super(CoreSession.class);
        importAnnotations(Defaults.of(RepositoryConfig.class));
    }

    protected RepositorySettings(RepositoryConfig config) {
        super(CoreSession.class);
        importAnnotations(config);
    }

    protected RepositorySettings(FeaturesRunner runner, RepositoryConfig config) {
        super(CoreSession.class);
        this.runner = runner;
        importAnnotations(config);
    }

    public RepositorySettings(FeaturesRunner runner) {
        super(CoreSession.class);
        this.runner = runner;
        RepositoryConfig conf = runner.getConfig(RepositoryConfig.class);
        if (conf == null) {
            conf = Defaults.of(RepositoryConfig.class);
        }
        importAnnotations(conf);
    }

    public void importAnnotations(RepositoryConfig repo) {
        type = repo.type();
        repositoryName = repo.repositoryName();
        databaseName = repo.databaseName();
        username = repo.user();
        granularity = repo.cleanup();
        repositoryFactoryClass = repo.repositoryFactoryClass();
        repoInitializer = newInstance(repo.init());
    }

    protected <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getSimpleName(), e);
        }
    }

    public void importSettings(RepositorySettings settings) {
        shutdown();
        // override only the user name and the type.
        // overriding initializer and granularity may broke tests that are
        // using specific initializers
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
        DatabaseHelper dbHelper = DatabaseHelper.DATABASE;
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            log.info("Deploying a VCS repo implementation");
            // type is ignored, the config inferred by DatabaseHelper from
            // system properties will be used
            dbHelper.setRepositoryName(repositoryName);
            dbHelper.setUp(repositoryFactoryClass);
            OSGiAdapter osgi = harness.getOSGiAdapter();
            Bundle bundle = osgi.getRegistry().getBundle(
                    "org.nuxeo.ecm.core.storage.sql.test");
            String contribPath = dbHelper.getDeploymentContrib();
            URL contribURL = bundle.getEntry(contribPath);
            assertNotNull("deployment contrib " + contribPath + " not found",
                    contribURL);
            Contribution contrib = new ContributionLocation(repositoryName,
                    contribURL);
            harness.getContext().deploy(contrib);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void shutdown() {
        try {
            if (session != null) {
                releaseSession();
            }
        } finally {
            try {
                DatabaseHelper.DATABASE.tearDown();
            } catch (SQLException e) {
                throw new RuntimeException("Cannot release database", e);
            }
        }
    }

    public CoreSession createSession() {
        assert session == null;
        try {
            session = openSessionAs(getUsername(), true, false);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return session;
    }

    /**
     * @since 5.6
     */
    public void releaseSession() {
        assert session != null;
        session.close();
        session = null;
    }

    public CoreSession getSession() {
        return session;
    }

    /**
     * Opens a {@link CoreSession} for the currently logged-in user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @return the session
     * @since 5.9.3
     */
    public CoreSession openSession() throws ClientException {
        return CoreInstance.openCoreSession(repositoryName);
    }

    /**
     * Opens a {@link CoreSession} for the given user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param username the user name
     * @return the session
     */
    public CoreSession openSessionAs(String username) throws ClientException {
        return CoreInstance.openCoreSession(repositoryName, username);
    }

    /**
     * Opens a {@link CoreSession} for the given principal.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param principal the principal
     * @return the session
     * @since 5.9.3
     */
    public CoreSession openSessionAs(Principal principal)
            throws ClientException {
        return CoreInstance.openCoreSession(repositoryName, principal);
    }

    public CoreSession openSessionAsAdminUser(String username)
            throws ClientException {
        return openSessionAs(username, true, false);
    }

    public CoreSession openSessionAsAnonymousUser(String username)
            throws ClientException {
        return openSessionAs(username, false, true);
    }

    public CoreSession openSessionAsSystemUser() throws ClientException {
        return openSessionAs(SecurityConstants.SYSTEM_USERNAME, true, false);
    }

    public CoreSession openSessionAs(String username, boolean isAdmin,
            boolean isAnonymous) throws ClientException {
        UserPrincipal principal = new UserPrincipal(username,
                new ArrayList<String>(), isAnonymous, isAdmin);
        return CoreInstance.openCoreSession(repositoryName, principal);
    }

    @Override
    public CoreSession get() {
        return getSession();
    }

    @Override
    public Scope getScope() {
        return CoreScope.INSTANCE;
    }
}
