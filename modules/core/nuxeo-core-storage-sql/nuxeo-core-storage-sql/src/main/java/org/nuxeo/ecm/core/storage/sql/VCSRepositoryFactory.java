/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Repository factory for VCS, the repository implements internal pooling of sessions.
 *
 * @since 11.1
 */
public class VCSRepositoryFactory implements RepositoryFactory {

    protected final String repositoryName;

    public VCSRepositoryFactory(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        RepositoryDescriptor descriptor = sqlRepositoryService.getRepositoryDescriptor(repositoryName);
        return new RepositoryImpl(descriptor);
    }

}
