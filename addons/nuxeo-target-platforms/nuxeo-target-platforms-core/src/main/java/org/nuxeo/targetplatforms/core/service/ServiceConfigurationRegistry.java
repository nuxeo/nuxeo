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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.targetplatforms.core.descriptors.ServiceConfigurationDescriptor;

/**
 * Registry for service configuration, not handling merge.
 *
 * @since 5.7.1
 */
public class ServiceConfigurationRegistry extends SimpleContributionRegistry<ServiceConfigurationDescriptor> {

    // only one contrib, use this id
    protected static String CONF_ID = "configuration";

    @Override
    public String getContributionId(ServiceConfigurationDescriptor contrib) {
        return CONF_ID;
    }

    @Override
    public void contributionUpdated(String id, ServiceConfigurationDescriptor contrib,
            ServiceConfigurationDescriptor newOrigContrib) {
        if (currentContribs.containsKey(id)) {
            currentContribs.remove(id);
        }
        currentContribs.put(id, contrib);
    }

    // API

    public ServiceConfigurationDescriptor getConfiguration() {
        return currentContribs.get(CONF_ID);
    }

}
