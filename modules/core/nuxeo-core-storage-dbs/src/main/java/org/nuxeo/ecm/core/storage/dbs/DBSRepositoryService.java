/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the configuration for DBS repositories.
 *
 * @since 7.10-HF04, 8.1
 */
public class DBSRepositoryService extends DefaultComponent {

    protected DBSRepositoryDescriptorRegistry registry = new DBSRepositoryDescriptorRegistry();

    protected static class DBSRepositoryDescriptorRegistry extends SimpleContributionRegistry<DBSRepositoryDescriptor> {

        @Override
        public String getContributionId(DBSRepositoryDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public DBSRepositoryDescriptor clone(DBSRepositoryDescriptor orig) {
            return orig.clone();
        }

        @Override
        public void merge(DBSRepositoryDescriptor src, DBSRepositoryDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public DBSRepositoryDescriptor getRepositoryDescriptor(String id) {
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

    public void addContribution(DBSRepositoryDescriptor descriptor,
            Class<? extends DBSRepositoryFactory> factoryClass) {
        registry.addContribution(descriptor);
        updateRegistration(descriptor.name, factoryClass);
    }

    public void removeContribution(DBSRepositoryDescriptor descriptor,
            Class<? extends DBSRepositoryFactory> factoryClass) {
        registry.removeContribution(descriptor);
        updateRegistration(descriptor.name, factoryClass);
    }

    /**
     * Update repository registration in high-level repository service.
     */
    protected void updateRegistration(String repositoryName, Class<? extends DBSRepositoryFactory> factoryClass) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        DBSRepositoryDescriptor descriptor = registry.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            // last contribution removed
            repositoryManager.removeRepository(repositoryName);
            return;
        }
        // extract label, isDefault
        // and pass it to high-level registry
        RepositoryFactory repositoryFactory;
        try {
            repositoryFactory = factoryClass.getConstructor(String.class).newInstance(repositoryName);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
        if (descriptor.isCacheEnabled()) {
            repositoryFactory = new DBSCachingRepositoryFactory(repositoryName, repositoryFactory);
        }
        Repository repository = new Repository(repositoryName, descriptor.label, descriptor.isDefault(),
                repositoryFactory);
        repositoryManager.addRepository(repository);
    }

    public DBSRepositoryDescriptor getRepositoryDescriptor(String name) {
        return registry.getRepositoryDescriptor(name);
    }

}
