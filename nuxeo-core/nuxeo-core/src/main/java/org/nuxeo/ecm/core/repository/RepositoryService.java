/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.api.local.LocalSession;
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
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        { // open repositories without a tx active
            Transaction tx = TransactionHelper.suspendTransaction();
            try {
                for (String name : repositoryManager.getRepositoryNames()) {
                    openRepository(name);
                }
            } finally {
                TransactionHelper.resumeTransaction(tx);
            }
        }
        // give up if no handler configured
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        // invoke handler with a tx active
        {
            boolean started = false;
            boolean ok = false;
            try {
                started = !TransactionHelper.isTransactionActive() && TransactionHelper.startTransaction();
                for (String name : repositoryManager.getRepositoryNames()) {
                    initializeRepository(handler, name);
                }
                ok = true;
            } finally {
                if (started) {
                    try {
                        if (!ok) {
                            TransactionHelper.setTransactionRollbackOnly();
                        }
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return (T) this;
        }
        if (adapter.isAssignableFrom(CoreSession.class)) {
            return (T) LocalSession.createInstance();
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
