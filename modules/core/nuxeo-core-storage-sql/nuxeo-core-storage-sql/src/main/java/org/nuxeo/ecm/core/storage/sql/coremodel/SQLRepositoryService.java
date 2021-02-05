/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.VCSRepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the configuration for VCS repositories.
 *
 * @since 5.9.3
 */
public class SQLRepositoryService extends DefaultComponent {

    private static final String XP_REPOSITORY = "repository";

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.REPOSITORY - 1;
    }

    @Override
    public void start(ComponentContext context) {
        // update repository registrations in high-level repository service
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        this.<RepositoryDescriptor> getRegistryContributions(XP_REPOSITORY).forEach(desc -> {
            // extract name label, isDefault, and pass it to high-level registry
            String name = desc.name;
            RepositoryFactory repositoryFactory = new VCSRepositoryFactory(name);
            Repository repository = new Repository(name, desc.label, desc.isDefault(), desc.isHeadless(),
                    repositoryFactory, desc.pool);
            repositoryManager.addRepository(repository);
        });
    }

    public RepositoryDescriptor getRepositoryDescriptor(String name) {
        return this.<RepositoryDescriptor> getRegistryContribution(XP_REPOSITORY, name).orElse(null);
    }

    /**
     * Gets the list of SQL repository names.
     *
     * @return the list of SQL repository names
     * @since 5.9.5
     */
    public List<String> getRepositoryNames() {
        return new ArrayList<>(this.<MapRegistry> getExtensionPointRegistry(XP_REPOSITORY).getContributions().keySet());
    }

    /**
     * Gets the low-level SQL Repository of the given name.
     *
     * @param repositoryName the repository name
     * @return the repository
     * @since 5.9.5
     */
    public RepositoryManagement getRepository(String repositoryName) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        org.nuxeo.ecm.core.model.Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            throw new RuntimeException("Unknown repository: " + repositoryName);
        }
        if (repository instanceof org.nuxeo.ecm.core.storage.sql.Repository) {
            // (JCA) ConnectionFactoryImpl already implements Repository
            return (org.nuxeo.ecm.core.storage.sql.Repository) repository;
        } else {
            throw new RuntimeException("Unknown repository class: " + repository.getClass().getName());
        }
    }

    public RepositoryImpl getRepositoryImpl(String repositoryName) {
        return (RepositoryImpl) getRepository(repositoryName);
    }

    /**
     * Gets the repositories as a list of {@link RepositoryManagement} objects.
     *
     * @since 5.9.5
     * @return a list of {@link RepositoryManagement}
     */
    public List<RepositoryManagement> getRepositories() {
        List<RepositoryManagement> repositories = new ArrayList<>();
        for (String repositoryName : getRepositoryNames()) {
            repositories.add(getRepository(repositoryName));
        }
        return repositories;
    }

    public FulltextConfiguration getFulltextConfiguration(String repositoryName) {
        return getRepositoryImpl(repositoryName).getModel().getFulltextConfiguration();
    }

}
