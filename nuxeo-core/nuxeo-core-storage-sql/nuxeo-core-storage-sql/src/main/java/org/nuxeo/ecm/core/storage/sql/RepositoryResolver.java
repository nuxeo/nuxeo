/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.storage.sql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;

/**
 * Locate a repository given its name. Try access through NXCore first (JNDI)
 * and then fall-back by using the repository manager.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public class RepositoryResolver {

    public static final Map<String,RepositoryImpl> repositories = new HashMap<String,RepositoryImpl>();

    private RepositoryResolver() {
    }

    public static void registerTestRepository(RepositoryImpl repo) {
        repositories.put(repo.getName(), repo);
    }

    public static List<Repository> getRepositories() {
        List<Repository> repositories = new ArrayList<Repository>();
        for (RepositoryDescriptor desc : NXCore.getRepositoryService().getRepositoryManager().getDescriptors()) {
            repositories.add(getRepository(desc.getName()));
        }
        return repositories;
    }

    public static Repository getRepository(String repositoryName) {
        Object repo = null;
        try {
            repo = NXCore.getRepository(repositoryName);
        } catch (NoSuchRepositoryException e) {
            // No JNDI binding (embedded or unit tests)
            try {
                repo = NXCore.getRepositoryService().getRepositoryManager().getRepository(repositoryName);
            } catch (Exception e1) {
                ;
            }
            if (repo == null) {
                repo = repositories.get(repositoryName);
            }
            if (repo == null) {
                throw new ClientRuntimeException("Cannot find repository " + repositoryName);
            }
        }
        if (repo instanceof Repository) {
            // (JCA) ConnectionFactoryImpl already implements Repository
            return (Repository)repo;
        } else if (repo instanceof SQLRepository) {
            // (LocalSession not pooled) SQLRepository
            // from SQLRepositoryFactory called by descriptor at registration
            return ((SQLRepository) repo).repository;
        } else {
            throw new RuntimeException("Unknown repository class: "
                    + repo.getClass().getName());
        }
    }

    public static BinaryManager getBinaryManager(String repositoryName) {
        Repository repo = getRepository(repositoryName);
        if (repo instanceof RepositoryImpl) {
            return ((RepositoryImpl)repo).getBinaryManager();
        }
        Class<? extends Repository> repoClass = repo.getClass();
        if ("org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl".equals(repoClass.getCanonicalName())) {
            try {
                return getBinaryManagerFromConnectionFactory(repo);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Cannot get access to binary manager through the connection factory",
                        e);
            }
        }
        throw new RuntimeException("Unknown repository class: " + repoClass);
    }

    protected static BinaryManager getBinaryManagerFromConnectionFactory(Repository repo) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = repo.getClass().getDeclaredField("managedConnectionFactory");
        field.setAccessible(true);
        Object factory = field.get(repo);
        field = factory.getClass().getDeclaredField("repository");
        field.setAccessible(true);
        return ((RepositoryImpl)field.get(factory)).getBinaryManager();
    }

}
