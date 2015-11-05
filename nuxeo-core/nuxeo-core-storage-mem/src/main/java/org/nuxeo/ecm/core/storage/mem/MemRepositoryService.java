/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.storage.mem;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the configuration for Memory repositories.
 *
 * @since 8.1
 */
public class MemRepositoryService extends DefaultComponent {

    private static final String XP_REPOSITORY = "repository";

    protected MemRepositoryDescriptorRegistry registry = new MemRepositoryDescriptorRegistry();

    protected static class MemRepositoryDescriptorRegistry extends SimpleContributionRegistry<MemRepositoryDescriptor> {

        @Override
        public String getContributionId(MemRepositoryDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public MemRepositoryDescriptor clone(MemRepositoryDescriptor orig) {
            return new MemRepositoryDescriptor(orig);
        }

        @Override
        public void merge(MemRepositoryDescriptor src, MemRepositoryDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public MemRepositoryDescriptor getRepositoryDescriptor(String id) {
            return getCurrentContribution(id);
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
            addContribution((MemRepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            removeContribution((MemRepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(MemRepositoryDescriptor descriptor) {
        registry.addContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    protected void removeContribution(MemRepositoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    /**
     * Update repository registration in high-level repository service.
     */
    protected void updateRegistration(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        MemRepositoryDescriptor descriptor = registry.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault
        // and pass it to high-level registry
        RepositoryFactory repositoryFactory = new MemRepositoryFactory();
        repositoryFactory.init(repositoryName);
        Repository repository = new Repository(repositoryName, descriptor.label, descriptor.isDefault(),
                repositoryFactory);
        repositoryManager.addRepository(repository);
    }

    public MemRepositoryDescriptor getRepositoryDescriptor(String name) {
        return registry.getRepositoryDescriptor(name);
    }

}
