/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Information about all Nuxeo repositories.
 * <p>
 * Information is cached, and an initial connection to the repository is needed to get the root folder id.
 */
public class NuxeoRepositories extends DefaultComponent {

    protected Map<String, NuxeoRepository> repositories;

    @Override
    public void start(ComponentContext context) {
        repositories = new ConcurrentHashMap<>();
    }

    @Override
    public void stop(ComponentContext context) {
        repositories = null;
    }

    public NuxeoRepository getRepository(String repositoryId) {
        initRepositories();
        return repositories.get(repositoryId);
    }

    public List<NuxeoRepository> getRepositories() {
        initRepositories();
        return new ArrayList<>(repositories.values());
    }

    protected void initRepositories() {
        if (!repositories.isEmpty()) {
            return;
        }
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            try (CloseableCoreSession coreSession = CoreInstance.openCoreSession(repositoryName)) {
                String rootFolderId = coreSession.getRootDocument().getId();
                repositories.put(repositoryName, new NuxeoRepository(repositoryName, rootFolderId));
            }
        }
    }

}
