/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * High-level service to get to a
 * {@link org.nuxeo.ecm.core.api.repository.Repository Repository} and from
 * there to {@link org.nuxeo.ecm.core.api.CoreSession CoreSession} objects.
 */
public class RepositoryManagerImpl extends DefaultComponent implements
        RepositoryManager {

    private static final Log log = LogFactory.getLog(RepositoryManagerImpl.class);

    private Map<String, Repository> repositories = Collections.synchronizedMap(new LinkedHashMap<String, Repository>());

    // compat from old extension point
    private Map<String, Repository> compatRepositories = new ConcurrentHashMap<String, Repository>();

    // compat
    private static final String XP_REPOSITORIES = "repositories";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_REPOSITORIES.equals(extensionPoint)) {
            Repository repo = (Repository) contribution;
            log.warn("Using old-style extension point"
                    + " org.nuxeo.ecm.core.api.repository.RepositoryManager"
                    + " for repository \""
                    + repo.getName()
                    + "\", use org.nuxeo.ecm.core.storage.sql.RepositoryService instead");
            compatRepositories.put(repo.getName(), repo);
        } else {
            throw new RuntimeException("Unknown extension point: "
                    + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_REPOSITORIES.equals(extensionPoint)) {
            Repository repo = (Repository) contribution;
            compatRepositories.remove(repo.getName());
        } else {
            throw new RuntimeException("Unknown extension point: "
                    + extensionPoint);
        }
    }

    // called by low-level repository service
    @Override
    public void addRepository(Repository repository) {
        String name = repository.getName();
        if (repositories.containsKey(name)) {
            log.info("Overriding repository: " + name);
        } else {
            log.info("Registering repository: " + name);
        }
        Repository compat = compatRepositories.get(name);
        if (compat != null) {
            if (repository.getLabel() == null) {
                repository.setLabel(compat.getLabel());
            }
            if (repository.getDefault() != null) {
                repository.setDefault(compat.getDefault());
            }
        }
        repositories.put(name, repository);
    }

    // call by low-level repository service
    @Override
    public void removeRepository(String name) {
        log.info("Removing repository: " + name);
        repositories.remove(name);
    }

    @Override
    public Collection<Repository> getRepositories() {
        return new ArrayList<Repository>(repositories.values());
    }

    @Override
    public List<String> getRepositoryNames() {
        return new ArrayList<String>(repositories.keySet());
    }

    @Override
    public Repository getRepository(String name) {
        return repositories.get(name);
    }

    @Override
    public Repository getDefaultRepository() {
        for (Repository repository : repositories.values()) {
            if (repository.isDefault()) {
                return repository;
            }
            if ("default".equals(repository.getName())) {
                return repository;
            }
        }
        // fallback to first in list
        if (!repositories.isEmpty()) {
            return repositories.values().iterator().next();
        }
        // no repository at all
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (RepositoryManager.class.isAssignableFrom(adapter)) {
            return (T) this;
        }
        return null;
    }

}
