/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Base DBS implementation of a {@link RepositoryFactory}, creating a subclass of {@link DBSRepository}.
 *
 * @since 7.10-HF04, 8.1
 */
public abstract class DBSRepositoryFactory implements RepositoryFactory {

    protected final String repositoryName;

    public DBSRepositoryFactory(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public DBSRepositoryDescriptor getRepositoryDescriptor() {
        DBSRepositoryService repositoryService = Framework.getService(DBSRepositoryService.class);
        DBSRepositoryDescriptor descriptor = repositoryService.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor registered for: " + repositoryName);
        }
        return descriptor;
    }

    /**
     * This is done so that the connection pool monitor has something to return. In the future we may have an actual
     * pool.
     */
    protected ConnectionManager installPool() {
        NuxeoConnectionManagerConfiguration pool = new NuxeoConnectionManagerConfiguration();
        pool.setName("repository/" + repositoryName);
        return NuxeoContainer.initConnectionManager(pool);
    }

}
