/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.dbs;

import org.nuxeo.ecm.core.repository.RepositoryFactory;

/**
 * Factory used to initialize the DBS Cache layer in front of the real repository.
 *
 * @since 8.10
 */
public class DBSCachingRepositoryFactory extends DBSRepositoryFactory {

    private final RepositoryFactory factory;

    public DBSCachingRepositoryFactory(String repositoryName, RepositoryFactory factory) {
        super(repositoryName);
        this.factory = factory;
    }

    @Override
    public Object call() {
        return new DBSCachingRepository((DBSRepository) factory.call(), getRepositoryDescriptor());
    }

}
