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
package org.nuxeo.ecm.core.storage.dbs;

import java.util.List;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Registry for {@link DBSRepositoryDescriptor} contributions retrieval, based on {@link DBSRepositoryContributor}
 * contributions done through API.
 *
 * @since 11.5
 */
public class DBSRepositoryRegistry extends MapRegistry {

    protected static final String COMPONENT_NAME = "org.nuxeo.ecm.core.storage.dbs.DBSRepositoryService";

    protected static final String POINT_NAME = "repositoryContributor";

    // custom API

    public List<DBSRepositoryContributor> getRepositoryContributors() {
        return getContributionValues();
    }

    public DBSRepositoryDescriptor getRepositoryDescriptor(String name) {
        DBSRepositoryContributor contrib = this.<DBSRepositoryContributor> getContribution(
                name).orElseThrow(() -> new IllegalArgumentException("Unknown contribution with name " + name));
        MapRegistry targetRegistry = getTargetRegistry(contrib.target, contrib.point);
        return targetRegistry.<DBSRepositoryDescriptor> getContribution(
                name).orElseThrow(() -> new IllegalArgumentException("Unknown target contribution with name " + name));
    }

    protected MapRegistry getTargetRegistry(String component, String point) {
        return Framework.getRuntime()
                        .getComponentManager()
                        .<MapRegistry> getExtensionPointRegistry(component, point)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown registry for extension point '%s--%s'", component, point)));
    }

}
