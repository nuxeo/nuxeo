/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component and service managing low-level repository instances.
 */
public class RepositoryService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.repository.RepositoryService");

    private static final Log log = LogFactory.getLog(RepositoryService.class);

    public static final String XP_REPOSITORY = "repository";

    // @GuardedBy("itself")
    private final Map<String, Repository> repositories = new HashMap<>();

    public void shutdown() {
        log.info("Shutting down repository manager");
        synchronized (repositories) {
            for (Repository repository : repositories.values()) {
                repository.shutdown();
            }
            repositories.clear();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return 100;
    }

    @Override
    public void activate(ComponentContext context) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                shutdown();
            }
        });
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        TransactionHelper.runInTransaction(this::initRepositories);
    }

    /**
     * Initializes all repositories. Run in a transaction.
     *
     * @since 8.4
     */
    protected void initRepositories() {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        for (String name : repositoryManager.getRepositoryNames()) {
            openRepository(name);
        }
        // give up if no handler configured
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        // invoke handlers
        for (String name : repositoryManager.getRepositoryNames()) {
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

    protected void openRepository(String name) {
        new UnrestrictedSessionRunner(name) {

            @Override
            public void run() {
                ;
            }

        }.runUnrestricted();
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
     * @param repositoryName
     *            the repository name
     * @return the repository instance or null if no repository with that name
     *         was registered
     */
    public Repository getRepository(String repositoryName) {
        synchronized (repositories) {
            return doGetRepository(repositoryName);
        }
    }

    /**
     * Calls to that method should be synchronized on repositories
     *
     * @since 7.2
     * @see #getRepository(String)
     * @see #getSession(String, String)
     */
    protected Repository doGetRepository(String repositoryName) {
        Repository repository = repositories.get(repositoryName);
        if (repository == null) {
            RepositoryFactory factory = getFactory(repositoryName);
            if (factory == null) {
                return null;
            }
            repository = (Repository) factory.call();
            repositories.put(repositoryName, repository);
        }
        return repository;
    }

    protected RepositoryFactory getFactory(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
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
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        return repositoryManager.getRepositoryNames();
    }

    /**
     * Creates a new session with the given session id from the given
     * repository.
     * <p/>
     * Locks repositories before entering the pool. That allows concurrency with
     * shutdown.
     *
     * @since 7.2
     */
    public Session getSession(String repositoryName) {
        synchronized (repositories) {
            Repository repository = doGetRepository(repositoryName);
            if (repository == null) {
                throw new LocalException("No such repository: " + repositoryName);
            }
            return repository.getSession();
        }
    }

}
