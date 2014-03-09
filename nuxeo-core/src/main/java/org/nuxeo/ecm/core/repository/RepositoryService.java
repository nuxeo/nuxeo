/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component and service managing low-level repository instances.
 */
public class RepositoryService extends DefaultComponent implements RepositoryManager {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.repository.RepositoryService");

    private static final Log log = LogFactory.getLog(RepositoryService.class);

    public static final String XP_REPOSITORY = "repository";

    private final Map<String, RepositoryDescriptor> descriptors = new ConcurrentHashMap<String, RepositoryDescriptor>();

    // @GuardedBy("itself")
    private final Map<String, Repository> repositories = new HashMap<String, Repository>();

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        shutdown();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint,
            ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            registerRepository((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint,
            ComponentInstance contributor) throws Exception {
        if (XP_REPOSITORY.equals(xpoint)) {
            unregisterRepository((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void registerRepository(RepositoryDescriptor rd) {
        String name = rd.getName();
        log.info("Registering repository: " + name);
        if (descriptors.containsKey(name)) {
            throw new RuntimeException("Repository already registered: " + name);
        }
        descriptors.put(name, rd);
    }

    protected void unregisterRepository(RepositoryDescriptor rd) {
        String name = rd.getName();
        log.info("Unregistering repository: " + name);
        if (!descriptors.containsKey(name)) {
            log.error("Repository not registered: " + name);
            return;
        }
        synchronized (repositories) {
            descriptors.remove(name);
            Repository repository = repositories.remove(name);
            if (repository != null) {
                repository.shutdown();
            }
        }
    }

    protected void shutdown() {
        log.info("Shutting down repository manager");
        synchronized (repositories) {
            for (Repository repository : repositories.values()) {
                repository.shutdown();
            }
            repositories.clear();
            descriptors.clear();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return 100;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        boolean started = false;
        boolean ok = false;
        try {
            started = !TransactionHelper.isTransactionActive()
                    && TransactionHelper.startTransaction();
            for (String name : getRepositoryNames()) {
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

    protected void initializeRepository(
            final RepositoryInitializationHandler handler, String name) {
        try {
            new UnrestrictedSessionRunner(name) {
                @Override
                public void run() throws ClientException {
                    handler.initializeRepository(session);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new RuntimeException("Failed to initialize repository '"
                    + name + "': " + e.getMessage(), e);
        }
    }
    public RepositoryManager getRepositoryManager() {
        return this;
    }

    /**
     * Gets a repository given its name.
     * <p>
     * Null is returned if no repository with that name was registered.
     *
     * @param name the repository name
     * @return the repository instance or null if no repository with that name
     *         was registered
     */
    @Override
    public Repository getRepository(String name) {
        Repository repository;
        synchronized (repositories) {
            repository = repositories.get(name);
            if (repository == null) {
                RepositoryDescriptor rd = descriptors.get(name);
                if (rd != null) {
                    repository = rd.create();
                    repositories.put(name, repository);
                }
            }
        }
        return repository;
    }

    @Override
    public String[] getRepositoryNames() {
        return descriptors.keySet().toArray(new String[0]);
    }

    @Override
    public RepositoryDescriptor getDescriptor(String name) {
        return descriptors.get(name);
    }

}
