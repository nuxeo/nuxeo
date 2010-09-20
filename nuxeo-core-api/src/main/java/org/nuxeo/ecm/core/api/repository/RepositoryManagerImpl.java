/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryManagerImpl extends DefaultComponent implements
        RepositoryManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.api.repository.RepositoryManager");

    private Map<String, Repository> repositories = Collections.synchronizedMap(new LinkedHashMap<String, Repository>());

    public void addRepository(Repository repository) {
        repositories.put(repository.getName(), repository);
    }

    public Collection<Repository> getRepositories() {
        return new ArrayList<Repository>(repositories.values());
    }

    public Repository getRepository(String name) {
        return repositories.get(name);
    }

    public void removeRepository(String name) {
        repositories.remove(name);
    }

    public void clear() {
        repositories.clear();
    }

    public Repository getDefaultRepository() {
        Iterator<Repository> it = repositories.values().iterator();

        Repository defaultRepo = null;

        // search for user defined
        while (it.hasNext()) {
            Repository repo = it.next();
            if (repo.isDefault()) {
                return repo;
            }
            if ("default".equals(repo.getName())) {
                defaultRepo = repo;
            }
        }

        // "default" fallback
        if (defaultRepo != null) {
            return defaultRepo;
        }

        // first in list "fallback"
        if (!repositories.isEmpty()) {
            return repositories.values().iterator().next();
        }

        // no repository at all
        return null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        repositories = new Hashtable<String, Repository>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        repositories.clear();
        repositories = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("repositories".equals(extensionPoint)) {
            addRepository((Repository) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("repositories".equals(extensionPoint)) {
            removeRepository(((Repository) contribution).getName());
        }
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
