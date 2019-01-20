/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryFactory;

/**
 * PostgreSQL+JSON implementation of a {@link RepositoryFactory}, creating a {@link PGJSONRepository}.
 *
 * @since 11.1
 */
public class PGJSONRepositoryFactory extends DBSRepositoryFactory {

    public PGJSONRepositoryFactory(String repositoryName) {
        super(repositoryName);
    }

    @Override
    public Object call() {
        return new PGJSONRepository(installPool(), (PGJSONRepositoryDescriptor) getRepositoryDescriptor());
    }

}
