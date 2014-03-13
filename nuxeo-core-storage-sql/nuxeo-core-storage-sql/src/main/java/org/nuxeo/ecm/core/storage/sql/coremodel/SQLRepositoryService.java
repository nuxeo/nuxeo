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

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
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
            repositoryManager.removeRepository(repositoryName);
        } else {
            // extract label, isDefault and factory
            // and pass it to high-level registry
            Class<? extends RepositoryFactory> repositoryFactoryClass = descriptor.getRepositoryFactoryClass();
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
    }

    public RepositoryDescriptor getRepositoryDescriptor(String name) {
        return registry.getRepositoryDescriptor(name);
    }

}
