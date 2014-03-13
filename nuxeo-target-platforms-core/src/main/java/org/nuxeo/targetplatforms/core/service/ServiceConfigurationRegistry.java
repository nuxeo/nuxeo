/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
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
 * @since 2.18
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
