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
package org.nuxeo.ecm.core.storage.mem;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * In-memory implementation of a {@link RepositoryFactory}, creating a {@link MemRepository}.
 *
 * @since 5.9.4
 */
public class MemRepositoryFactory implements RepositoryFactory {

    protected String repositoryName;

    @Override
    public void init(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        MemRepositoryService repositoryService = Framework.getLocalService(MemRepositoryService.class);
        MemRepositoryDescriptor descriptor = repositoryService.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor registered for: " + repositoryName);
        }
        return new MemRepository(descriptor);
    }

}
