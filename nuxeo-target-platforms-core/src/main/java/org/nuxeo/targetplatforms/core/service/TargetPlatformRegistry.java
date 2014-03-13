/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.targetplatforms.core.descriptors.TargetPlatformDescriptor;


/**
 * Registry for target platform contributions, handling merge on "enabled"
 * attribute only.
 *
 * @since 2.18
 */
public class TargetPlatformRegistry extends
        SimpleContributionRegistry<TargetPlatformDescriptor> {

    public String getContributionId(TargetPlatformDescriptor contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id,
            TargetPlatformDescriptor contrib,
            TargetPlatformDescriptor newOrigContrib) {
        if (currentContribs.containsKey(id)) {
            currentContribs.remove(id);
        }
        currentContribs.put(id, contrib);
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    @Override
    public TargetPlatformDescriptor clone(TargetPlatformDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(TargetPlatformDescriptor src, TargetPlatformDescriptor dst) {
        // support merge only for enabled boolean
        if (src.isEnableSet() && src.isEnabled() != dst.isEnabled()) {
            dst.setEnabled(src.isEnabled());
        }
    }

    // API

    public TargetPlatformDescriptor getTargetPlatform(String id) {
        return currentContribs.get(id);
    }

    public List<TargetPlatformDescriptor> getTargetPlatforms() {
        List<TargetPlatformDescriptor> all = new ArrayList<>();
        all.addAll(currentContribs.values());
        return all;
    }

}
