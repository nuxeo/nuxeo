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

package org.nuxeo.ecm.core.repository.ejb;

import java.util.Collection;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Exposes a local repository manager as an stateless session bean.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Stateless
@Remote(RepositoryManager.class)
@Local(RepositoryManagerLocal.class)
public class RepositoryManagerBean implements RepositoryManagerLocal {

    private final RepositoryManager mgr;

    public RepositoryManagerBean() {
        // use the local runtime service as the backend
        mgr = Framework.getLocalService(RepositoryManager.class);
    }

    public void addRepository(Repository repository) {
        mgr.addRepository(repository);
    }

    public Collection<Repository> getRepositories() {
        return mgr.getRepositories();
    }

    public Repository getRepository(String name) {
        return mgr.getRepository(name);
    }

    public void removeRepository(String name) {
        mgr.removeRepository(name);
    }

    public void clear() {
        mgr.clear();
    }

    public Repository getDefaultRepository() {
        return mgr.getDefaultRepository();
    }

}
