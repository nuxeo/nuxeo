/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.runtime.api.Framework;

/**
 * Locates a repository given its name, and other low-level repository operations.
 */
public class RepositoryResolver {

    public static final Map<String, RepositoryImpl> testRepositories = new HashMap<String, RepositoryImpl>();

    private RepositoryResolver() {
    }

    public static void registerTestRepository(RepositoryImpl repo) {
        testRepositories.put(repo.getName(), repo);
    }

    public static List<RepositoryManagement> getRepositories() {
        RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
        List<String> repositoryNames = repositoryService.getRepositoryNames();
        List<RepositoryManagement> repositories = new ArrayList<RepositoryManagement>(
                repositoryNames.size());
        for (String repositoryName : repositoryNames) {
            repositories.add(getRepository(repositoryName));
        }
        return repositories;
    }

    public static Repository getRepository(String repositoryName) {
        RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
        Object repo = repositoryService.getRepository(repositoryName);
        if (repo == null) {
            // check test repository
            repo = testRepositories.get(repositoryName);
        }
        if (repo == null) {
            throw new ClientRuntimeException("Cannot find repository "
                    + repositoryName);
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

    /**
     * @deprecated since 5.9.4, use BinaryManagerService instead
     */
    @Deprecated
    public static BinaryManager getBinaryManager(String repositoryName) {
        return getRepositoryImpl(getRepository(repositoryName)).getBinaryManager();
    }

    public static Class<? extends FulltextParser> getFulltextParserClass(
            String repositoryName) {
        return getRepositoryImpl(getRepository(repositoryName)).getFulltextParserClass();
    }

    public static ModelFulltext getModelFulltext(String repositoryName) {
        return getRepositoryImpl(getRepository(repositoryName)).getModel().getFulltextInfo();
    }

    private static final String CONNECTIONFACTORYIMPL_CLASS = "org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl";

    public static RepositoryImpl getRepositoryImpl(Repository repository) {
        if (repository instanceof RepositoryImpl) {
            return (RepositoryImpl) repository;
        }
        if (CONNECTIONFACTORYIMPL_CLASS.equals(repository.getClass().getName())) {
            try {
                Field f1 = repository.getClass().getDeclaredField(
                        "managedConnectionFactory");
                f1.setAccessible(true);
                Object factory = f1.get(repository);
                Field f2 = factory.getClass().getDeclaredField("repository");
                f2.setAccessible(true);
                return (RepositoryImpl) f2.get(factory);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Unknown repository class: "
                + repository.getClass());
    }

}
