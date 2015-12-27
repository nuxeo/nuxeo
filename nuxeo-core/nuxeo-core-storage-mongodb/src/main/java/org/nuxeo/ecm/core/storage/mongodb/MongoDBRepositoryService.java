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
package org.nuxeo.ecm.core.storage.mongodb;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the configuration for MongoDB repositories.
 *
 * @since 5.9.4
 */
public class MongoDBRepositoryService extends DefaultComponent {

    private static final String XP_REPOSITORY = "repository";

    protected RepositoryDescriptorRegistry registry = new RepositoryDescriptorRegistry();

    protected static class RepositoryDescriptorRegistry extends SimpleContributionRegistry<MongoDBRepositoryDescriptor> {

        @Override
        public String getContributionId(MongoDBRepositoryDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public MongoDBRepositoryDescriptor clone(MongoDBRepositoryDescriptor orig) {
            return new MongoDBRepositoryDescriptor(orig);
        }

        @Override
        public void merge(MongoDBRepositoryDescriptor src, MongoDBRepositoryDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public MongoDBRepositoryDescriptor getRepositoryDescriptor(String id) {
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
            addContribution((MongoDBRepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            removeContribution((MongoDBRepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(MongoDBRepositoryDescriptor descriptor) {
        registry.addContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    protected void removeContribution(MongoDBRepositoryDescriptor descriptor) {
        registry.removeContribution(descriptor);
        updateRegistration(descriptor.name);
    }

    /**
     * Update repository registration in high-level repository service.
     */
    protected void updateRegistration(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        MongoDBRepositoryDescriptor descriptor = registry.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault
        // and pass it to high-level registry
        RepositoryFactory repositoryFactory = new MongoDBRepositoryFactory();
        repositoryFactory.init(repositoryName);
        Repository repository = new Repository(repositoryName, descriptor.label, descriptor.isDefault(),
                repositoryFactory);
        repositoryManager.addRepository(repository);
    }

    public MongoDBRepositoryDescriptor getRepositoryDescriptor(String name) {
        return registry.getRepositoryDescriptor(name);
    }

}
