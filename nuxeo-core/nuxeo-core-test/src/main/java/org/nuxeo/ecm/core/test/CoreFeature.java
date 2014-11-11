/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;

/**
 * The core feature provides deployments needed to have a nuxeo core running.
 * Several annotations can be used:
 * <ul>
 * <li>FIXME
 * <li>FIXME
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
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
public class CoreFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(CoreFeature.class);

    private RepositorySettings repository;

    public RepositorySettings getRepository() {
        return repository;
    }

    public BackendType getBackendType() {
        return repository.getBackendType();
    }

    @Override
    public void initialize(FeaturesRunner runner)
            throws Exception {
        repository = new RepositorySettings(runner);
        runner.getFeature(RuntimeFeature.class).addServiceProvider(CoreSession.class, repository);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        repository.initialize();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(RepositorySettings.class).toInstance(repository);
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        initializeSession(runner);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        //TODO cleanupSession(runner);
        Framework.getService(EventService.class).waitForAsyncCompletion();
        repository.shutdown();
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        if (repository.getGranularity() == Granularity.METHOD) {
            cleanupSession(runner);
        }
    }

    protected void cleanupSession(FeaturesRunner runner) {
        CoreSession session = runner.getInjector().getInstance(CoreSession.class);

        try {
            session.removeChildren(new PathRef("/"));
        } catch (ClientException e) {
            log.error("Unable to reset repository", e);
        }

        initializeSession(runner);
    }

    protected void initializeSession(FeaturesRunner runner) {
        CoreSession session = runner.getInjector().getInstance(CoreSession.class);

        RepositoryInit factory = repository.getInitializer();
        if (factory != null) {
            try {
                factory.populate(session);
                session.save();
            } catch (ClientException e) {
                log.error(e.toString(), e);
            }
        }
    }

    public void setRepositorySettings(RepositorySettings settings) {
        repository.importSettings(settings);
    }

}
