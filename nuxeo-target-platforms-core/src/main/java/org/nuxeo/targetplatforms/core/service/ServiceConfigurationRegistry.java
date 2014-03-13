/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
public class ServiceConfigurationRegistry extends
        SimpleContributionRegistry<ServiceConfigurationDescriptor> {

    // only one contrib, use this id
    protected static String CONF_ID = "configuration";

    public String getContributionId(ServiceConfigurationDescriptor contrib) {
        return CONF_ID;
    }

    @Override
    public void contributionUpdated(String id,
            ServiceConfigurationDescriptor contrib,
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
