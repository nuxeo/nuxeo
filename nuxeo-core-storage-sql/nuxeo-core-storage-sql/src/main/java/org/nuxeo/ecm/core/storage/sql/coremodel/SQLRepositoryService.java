/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextParser;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
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

    protected static class RepositoryDescriptorRegistry extends
            SimpleContributionRegistry<RepositoryDescriptor> {

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
    public void activate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint,
            ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            addContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint,
            ComponentInstance contributor) throws Exception {
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
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        RepositoryDescriptor descriptor = registry.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault and factory
        // and pass it to high-level registry
        Class<? extends RepositoryFactory> repositoryFactoryClass = descriptor.getRepositoryFactoryClass();
        if (repositoryFactoryClass == null) {
            // not the main contribution, just an override with
            // much less info
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        RepositoryFactory repositoryFactory;
        try {
            repositoryFactory = repositoryFactoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate repository: "
                    + repositoryName, e);
        }
        repositoryFactory.init(repositoryName);
        Repository repository = new Repository(repositoryName,
                descriptor.label, descriptor.isDefault(), repositoryFactory);
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

    protected final Map<String, RepositoryImpl> testRepositories = new HashMap<String, RepositoryImpl>();

    public void registerTestRepository(RepositoryImpl repository) {
        testRepositories.put(repository.getName(), repository);
    }

    /**
     * Gets the low-level SQL Repository of the given name.
     *
     * @param repositoryName the repository name
     * @return the repository
     * @since 5.9.5
     */
    public RepositoryManagement getRepository(String repositoryName) {
        RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
        org.nuxeo.ecm.core.model.Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            RepositoryImpl repo = testRepositories.get(repositoryName);
            if (repo != null) {
                return repo;
            }
        }
        if (repository == null) {
            throw new RuntimeException("Unknown repository: " + repositoryName);
        }
        if (repository instanceof org.nuxeo.ecm.core.storage.sql.Repository) {
            // (JCA) ConnectionFactoryImpl already implements Repository
            return (org.nuxeo.ecm.core.storage.sql.Repository) repository;
        } else if (repository instanceof SQLRepository) {
            // (LocalSession not pooled) SQLRepository
            // from SQLRepositoryFactory called by descriptor at registration
            return ((SQLRepository) repository).repository;
        } else {
            throw new RuntimeException("Unknown repository class: "
                    + repository.getClass().getName());
        }
    }

    protected RepositoryImpl getRepositoryImpl(String repositoryName) {
        RepositoryManagement repository = getRepository(repositoryName);
        if (repository instanceof RepositoryImpl) {
            return (RepositoryImpl) repository;
        }
        if (!CONNECTIONFACTORYIMPL_CLASS.equals(repository.getClass().getName())) {
            throw new RuntimeException("Unknown repository class: "
                    + repository.getClass());
        }
        try {
            Field f1 = repository.getClass().getDeclaredField(
                    "managedConnectionFactory");
            f1.setAccessible(true);
            Object factory = f1.get(repository);
            Field f2 = factory.getClass().getDeclaredField("repository");
            f2.setAccessible(true);
            return (RepositoryImpl) f2.get(factory);
        } catch (SecurityException | NoSuchFieldException
                | IllegalAccessException e) {
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
        List<RepositoryManagement> repositories = new ArrayList<RepositoryManagement>();
        for (String repositoryName : getRepositoryNames()) {
            repositories.add(getRepository(repositoryName));
        }
        return repositories;
    }

    public Class<? extends FulltextParser> getFulltextParserClass(
            String repositoryName) {
        return getRepositoryImpl(repositoryName).getFulltextParserClass();
    }

    public FulltextConfiguration getFulltextConfiguration(String repositoryName) {
        return getRepositoryImpl(repositoryName).getModel().getFulltextConfiguration();
    }

    /**
     * Returns the datasource definition for the given repository and fills the
     * properties map with the datasource configuration.
     *
     * @param repositoryName the repository name
     * @param properties a return map of properties
     * @return the XA datasource name, or null if single datasource is
     *         configured
     * @since 5.9.5
     */
    public String getRepositoryDataSourceAndProperties(String repositoryName,
            Map<String, String> properties) {
        RepositoryDescriptor desc = getRepositoryImpl(repositoryName).getRepositoryDescriptor();
        if (desc.properties != null) {
            properties.putAll(desc.properties);
        }
        return desc.xaDataSourceName;
    }

}
