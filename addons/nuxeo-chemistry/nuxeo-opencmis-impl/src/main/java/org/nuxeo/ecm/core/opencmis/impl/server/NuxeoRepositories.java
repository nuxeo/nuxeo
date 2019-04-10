/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
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

    protected Map<String, NuxeoRepository> repositories;

    @Override
    public void activate(ComponentContext context) throws Exception {
        repositories = new ConcurrentHashMap<String, NuxeoRepository>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        repositories = null;
    }

    public NuxeoRepository getRepository(String repositoryId) {
        initRepositories();
        return repositories.get(repositoryId);
    }

    public List<NuxeoRepository> getRepositories() {
        initRepositories();
        return new ArrayList<NuxeoRepository>(repositories.values());
    }

    protected void initRepositories() {
        if (!repositories.isEmpty()) {
            return;
        }
        try {
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            for (String repositoryName : repositoryManager.getRepositoryNames()) {
                try (CoreSession coreSession = CoreInstance.openCoreSession(repositoryName)) {
                    String rootFolderId = coreSession.getRootDocument().getId();
                    repositories.put(repositoryName, new NuxeoRepository(
                            repositoryName, rootFolderId));
                } catch (ClientException e) {
                    throw new CmisRuntimeException(e.toString(), e);
                }
            }
        } catch (CmisRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

}
