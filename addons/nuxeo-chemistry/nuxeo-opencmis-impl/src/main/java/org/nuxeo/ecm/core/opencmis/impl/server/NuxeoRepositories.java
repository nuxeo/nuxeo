/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Information about all Nuxeo repositories.
 * <p>
 * Information is cached, and an initial connection to the repository is needed
 * to get the root folder id.
 */
public class NuxeoRepositories extends DefaultComponent {

    protected static Map<String, NuxeoRepository> repositories;

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        clear();
    }

    public static void clear() {
        repositories = null;
    }

    public static NuxeoRepository getRepository(String repositoryId) {
        if (repositories == null) {
            initRepositories();
        }
        return repositories.get(repositoryId);
    }

    public static List<NuxeoRepository> getRepositories() {
        if (repositories == null) {
            initRepositories();
        }
        return new ArrayList<NuxeoRepository>(repositories.values());
    }

    protected static void initRepositories() {
        repositories = Collections.synchronizedMap(new HashMap<String, NuxeoRepository>());
        try {
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            for (Repository repo : repositoryManager.getRepositories()) {
                String repositoryId = repo.getName();
                String rootFolderId;
                CoreSession coreSession = null;
                try {
                    coreSession = repositoryManager.getRepository(repositoryId).open();
                    rootFolderId = coreSession.getRootDocument().getId();
                } catch (ClientException e) {
                    throw new CmisRuntimeException(e.toString(), e);
                } finally {
                    if (coreSession != null) {
                        Repository.close(coreSession);
                    }
                }
                repositories.put(repositoryId, new NuxeoRepository(
                        repositoryId, rootFolderId));
            }
        } catch (CmisRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

}
