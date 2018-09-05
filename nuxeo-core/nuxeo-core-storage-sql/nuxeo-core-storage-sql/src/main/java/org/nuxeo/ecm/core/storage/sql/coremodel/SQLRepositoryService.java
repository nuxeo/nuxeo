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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.FulltextParser;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the configuration for VCS repositories.
 *
 * @since 5.9.3
 */
public class SQLRepositoryService extends DefaultComponent {

    private static final String XP_REPOSITORY = "repository";

    protected static final String CONNECTIONFACTORYIMPL_CLASS = "org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl";

    protected RepositoryDescriptorRegistry registry = new RepositoryDescriptorRegistry();

    protected static class RepositoryDescriptorRegistry extends SimpleContributionRegistry<RepositoryDescriptor> {

        @Override
        public String getContributionId(RepositoryDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public RepositoryDescriptor clone(RepositoryDescriptor orig) {
            return new RepositoryDescriptor(orig);
        }

        @Override
        public void merge(RepositoryDescriptor src, RepositoryDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public RepositoryDescriptor getRepositoryDescriptor(String id) {
            return getCurrentContribution(id);
        }

        public List<String> getRepositoryIds() {
            return new ArrayList<>(currentContribs.keySet());
        }
    }

    @Override
    public void activate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            addContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            removeContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(RepositoryDescriptor descriptor) {
        registry.addContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    protected void removeContribution(RepositoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    /**
     * Update repository registration in high-level repository service.
     */
    protected void updateRegistration(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        RepositoryDescriptor descriptor = registry.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault
        // and pass it to high-level registry
        RepositoryFactory repositoryFactory = new PoolingRepositoryFactory(repositoryName);
        Repository repository = new Repository(repositoryName, descriptor.label, descriptor.isDefault(),
                repositoryFactory);
        repositoryManager.addRepository(repository);
    }

    public RepositoryDescriptor getRepositoryDescriptor(String name) {
        return registry.getRepositoryDescriptor(name);
    }

    /**
     * Gets the list of SQL repository names.
     *
     * @return the list of SQL repository names
     * @since 5.9.5
     */
    public List<String> getRepositoryNames() {
        return registry.getRepositoryIds();
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
        RepositoryManagement repository = getRepository(repositoryName);
        if (repository instanceof RepositoryImpl) {
            return (RepositoryImpl) repository;
        }
        if (!CONNECTIONFACTORYIMPL_CLASS.equals(repository.getClass().getName())) {
            throw new RuntimeException("Unknown repository class: " + repository.getClass());
        }
        try {
            Field f1 = repository.getClass().getDeclaredField("managedConnectionFactory");
            f1.setAccessible(true);
            Object factory = f1.get(repository);
            Field f2 = factory.getClass().getDeclaredField("repository");
            f2.setAccessible(true);
            return (RepositoryImpl) f2.get(factory);
        } catch (SecurityException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    public Class<? extends FulltextParser> getFulltextParserClass(String repositoryName) {
        return getRepositoryImpl(repositoryName).getFulltextParserClass();
    }

    public FulltextConfiguration getFulltextConfiguration(String repositoryName) {
        return getRepositoryImpl(repositoryName).getModel().getFulltextConfiguration();
    }

}
