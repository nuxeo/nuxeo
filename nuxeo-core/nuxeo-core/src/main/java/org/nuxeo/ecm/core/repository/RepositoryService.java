/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component and service managing low-level repository instances.
 */
public class RepositoryService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.repository.RepositoryService");

    private static final Log log = LogFactory.getLog(RepositoryService.class);

    public static final String XP_REPOSITORY = "repository";

    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();

    public void shutdown() {
        log.info("Shutting down repository manager");
        repositories.values().forEach(Repository::shutdown);
        repositories.clear();
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.REPOSITORY;
    }

    @Override
    public void start(ComponentContext context) {
        TransactionHelper.runInTransaction(this::doCreateRepositories);
        Framework.getRuntime().getComponentManager().addListener(new ComponentManager.Listener() {
            @Override
            public void afterStart(ComponentManager mgr, boolean isResume) {
                initRepositories(); // call all RepositoryInitializationHandler
            }

            @Override
            public void afterStop(ComponentManager mgr, boolean isStandby) {
                Framework.getRuntime().getComponentManager().removeListener(this);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) {
        TransactionHelper.runInTransaction(this::shutdown);
    }

    /**
     * Start a tx and initialize repositories content. This method is publicly exposed since it is needed by tests to
     * initialize repositories after cleanups (see CoreFeature).
     *
     * @since 8.4
     */
    public void initRepositories() {
        TransactionHelper.runInTransaction(this::doInitRepositories);
    }

    /**
     * Creates all the repositories. Requires an active transaction.
     *
     * @since 9.3
     */
    protected void doCreateRepositories() {
        repositories.clear();
        for (String repositoryName : getRepositoryNames()) {
            RepositoryFactory factory = getFactory(repositoryName);
            if (factory == null) {
                continue;
            }
            Repository repository = (Repository) factory.call();
            repositories.put(repositoryName, repository);
        }
    }

    /**
     * Initializes all the repositories. Requires an active transaction.
     *
     * @since 9.3
     */
    protected void doInitRepositories() {
        // give up if no handler configured
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        // invoke handlers
        for (String name : getRepositoryNames()) {
            initializeRepository(handler, name);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return adapter.cast(this);
        }
        return null;
    }

    protected void initializeRepository(final RepositoryInitializationHandler handler, String name) {
        new UnrestrictedSessionRunner(name) {
            @Override
            public void run() {
                handler.initializeRepository(session);
            }
        }.runUnrestricted();
    }

    /**
     * Gets a repository given its name.
     * <p>
     * Null is returned if no repository with that name was registered.
     *
     * @param repositoryName the repository name
     * @return the repository instance or null if no repository with that name was registered
     */
    public Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    protected RepositoryFactory getFactory(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        if (repositoryManager == null) {
            // tests with no high-level repository manager
            return null;
        }
        org.nuxeo.ecm.core.api.repository.Repository repo = repositoryManager.getRepository(repositoryName);
        if (repo == null) {
            return null;
        }
        RepositoryFactory repositoryFactory = (RepositoryFactory) repo.getRepositoryFactory();
        if (repositoryFactory == null) {
            throw new NullPointerException("Missing repositoryFactory for repository: " + repositoryName);
        }
        return repositoryFactory;
    }

    public List<String> getRepositoryNames() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return repositoryManager.getRepositoryNames();
    }

}
