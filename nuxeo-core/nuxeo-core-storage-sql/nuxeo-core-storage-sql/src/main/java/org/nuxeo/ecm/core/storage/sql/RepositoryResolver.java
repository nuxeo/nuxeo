/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

/**
 *
 * Locate a repository giving it's name. Try access through NXCore first (JNDI)
 * and then fall-back by using the repository manager.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 *
 */
public class RepositoryResolver {

    private RepositoryResolver() {

    }

    public static Map<String,RepositoryImpl> repositories = new HashMap<String,RepositoryImpl>();

    public static void registerTestRepository(RepositoryImpl repo) {
        repositories.put(repo.getName(), repo);
    }

    public static List<Repository> getRepositories() {
        List<Repository> repositories = new ArrayList<Repository>();
        for (org.nuxeo.ecm.core.repository.RepositoryDescriptor desc:NXCore.getRepositoryService().getRepositoryManager().getDescriptors()) {
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
        } else if (repo instanceof org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository) {
            // (LocalSession not pooled) SQLRepository
            // from SQLRepositoryFactory called by descriptor at registration
            return ((org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository) repo).repository;
        } else {
            throw new Error("Unknown repository class: " + repo.getClass().getName());
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
                throw new Error("Cannot get access to binary manager through the connection factory", e);
            }
        }
        throw new Error("Unknown repository class: " + repoClass);
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
