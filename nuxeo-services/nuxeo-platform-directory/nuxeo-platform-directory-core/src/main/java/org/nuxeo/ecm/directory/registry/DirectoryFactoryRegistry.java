/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.registry;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryFactoryProxy;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Tracks available factories.
 *
 * @since 5.6
 */
public class DirectoryFactoryRegistry extends SimpleContributionRegistry<DirectoryFactory> {

    @Override
    public String getContributionId(DirectoryFactory contrib) {
        if (contrib instanceof DirectoryFactoryProxy) {
            return ((DirectoryFactoryProxy) contrib).getComponentName();
        }
        return contrib.getName();
    }

    // API

    public List<DirectoryFactory> getFactories() {
        List<DirectoryFactory> facts = new ArrayList<DirectoryFactory>();
        if (currentContribs != null) {
            facts.addAll(currentContribs.values());
        }
        return facts;
    }

    public DirectoryFactory getFactory(String name) {
        return getCurrentContribution(name);
    }

}
