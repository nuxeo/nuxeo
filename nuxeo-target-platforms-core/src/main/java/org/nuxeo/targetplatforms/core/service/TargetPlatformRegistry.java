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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.targetplatforms.core.descriptors.TargetPlatformDescriptor;


/**
 * Registry for target platform contributions, handling merge on "enabled"
 * attribute only.
 *
 * @since 5.7.1
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
