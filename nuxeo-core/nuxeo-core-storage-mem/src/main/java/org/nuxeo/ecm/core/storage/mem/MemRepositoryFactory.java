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
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryFactory;

/**
 * In-memory implementation of a {@link RepositoryFactory}, creating a {@link MemRepository}.
 *
 * @since 5.9.4
 */
public class MemRepositoryFactory extends DBSRepositoryFactory {

    public MemRepositoryFactory(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public Object call() {
        return new MemRepository(installPool(), (MemRepositoryDescriptor) getRepositoryDescriptor());
    }

}
