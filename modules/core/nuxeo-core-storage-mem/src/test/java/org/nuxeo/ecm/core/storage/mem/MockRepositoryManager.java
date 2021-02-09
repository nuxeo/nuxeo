/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.storage.mem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Mock repository manager for tests.
 * <p>
 * Implemented methods prevent further setup of repository: tests will only check addition of the repository through the
 * {@link #addRepository(Repository)} api.
 *
 * @since 11.5
 */
public class MockRepositoryManager extends DefaultComponent implements RepositoryManager {

    protected Map<String, Repository> repos;

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }

    @Override
    public void start(ComponentContext context) {
        repos = new HashMap<>();
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        repos = null;
    }

    @Override
    public Collection<Repository> getRepositories() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRepositoryNames() {
        return Collections.emptyList();
    }

    @Override
    public Repository getRepository(String name) {
        return null;
    }

    @Override
    public void addRepository(Repository repository) {
        repos.put(repository.getName(), repository);
    }

    @Override
    public void removeRepository(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository getDefaultRepository() {
        return null;
    }

    @Override
    public String getDefaultRepositoryName() {
        throw new UnsupportedOperationException();
    }

    public Map<String, Repository> getResolvedRepositories() {
        return Collections.unmodifiableMap(repos);
    }

}
