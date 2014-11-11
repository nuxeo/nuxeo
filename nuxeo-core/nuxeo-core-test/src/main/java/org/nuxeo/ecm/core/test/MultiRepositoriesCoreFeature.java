/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin 
 */
package org.nuxeo.ecm.core.test;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfigs;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

@Deploy({
    "org.nuxeo.ecm.core.schema",
    "org.nuxeo.ecm.core.query",
    "org.nuxeo.ecm.core.api",
    "org.nuxeo.ecm.core.event",
    "org.nuxeo.ecm.core",
    "org.nuxeo.ecm.core.convert",
    "org.nuxeo.ecm.core.storage.sql",
    "org.nuxeo.ecm.core.storage.sql.test"
})
@Features(RuntimeFeature.class)
public class MultiRepositoriesCoreFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(CoreFeature.class);

    private final Map<String,RepositorySettings> repositories =
        new HashMap<String,RepositorySettings>();

    public RepositorySettings getRepository(String name) {
        return repositories.get(name);
    }

    public BackendType getBackendType(String name) {
        return repositories.get(name).getBackendType();
    }

    protected void setupRepos(FeaturesRunner runner) {
        RepositoryConfigs configs = runner.getDescription().getAnnotation(RepositoryConfigs.class);
        if (configs == null) {
            RepositorySettings repo = new RepositorySettings(runner);
            repositories.put(repo.repositoryName, repo);
        } else {
            for (RepositoryConfig config:configs.value()) {
                RepositorySettings repository = new RepositorySettings(runner, config);
                repositories.put(repository.repositoryName, repository);
            }
        }
    }

    @Override
    public void initialize(FeaturesRunner runner)
            throws Exception {
        setupRepos(runner);
//        for (RepositorySettings repo:repositories.values()) {
//            runner.getFeature(RuntimeFeature.class).addServiceProvider(CoreSession.class, repo);
//        }
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        for (RepositorySettings repository:repositories.values()) {
            repository.initialize();
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        for (RepositorySettings repository:repositories.values()) {
            binder.bind(RepositorySettings.class).
                annotatedWith(Names.named(repository.repositoryName)).
                toInstance(repository);
            binder.bind(CoreSession.class).
                annotatedWith(Names.named(repository.repositoryName)).
                toProvider(repository).in(Scopes.SINGLETON);
        }
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        for (RepositorySettings repository:repositories.values()) {
            initializeSession(runner, repository);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        //TODO cleanupSession(runner);
        Framework.getService(EventService.class).waitForAsyncCompletion();
        for (RepositorySettings repository:repositories.values()) {
            repository.shutdown();
        }
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        for (RepositorySettings repository:repositories.values()) {
            if (repository.getGranularity() == Granularity.METHOD) {
                cleanupSession(runner, repository);
            }
        }
    }

    protected void cleanupSession(FeaturesRunner runner, RepositorySettings repository) {
        CoreSession session = runner.getInjector().getInstance(CoreSession.class);

        try {
            session.removeChildren(new PathRef("/"));
        } catch (ClientException e) {
            log.error("Unable to reset repository", e);
        }

        initializeSession(runner, repository);
    }

    protected void initializeSession(FeaturesRunner runner, RepositorySettings repository) {
        CoreSession session = repository.get();
        RepositoryInit initializer = repository.getInitializer();
        if (initializer != null) {
            try {
                initializer.populate(session);
                session.save();
            } catch (ClientException e) {
                log.error(e.toString(), e);
            }
        }
    }

    public void setRepositorySettings(RepositorySettings settings) {
        repositories.clear();
        repositories.put(settings.repositoryName, settings);
    }

}
