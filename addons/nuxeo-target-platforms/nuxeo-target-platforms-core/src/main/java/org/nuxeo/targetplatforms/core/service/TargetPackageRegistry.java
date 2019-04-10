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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.targetplatforms.core.descriptors.TargetPackageDescriptor;

/**
 * Registry for target package contributions, handling merge on "enabled" attribute only.
 *
 * @since 5.7.1
 */
public class TargetPackageRegistry extends SimpleContributionRegistry<TargetPackageDescriptor> {

    @Override
    public String getContributionId(TargetPackageDescriptor contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, TargetPackageDescriptor contrib, TargetPackageDescriptor newOrigContrib) {
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
    public TargetPackageDescriptor clone(TargetPackageDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(TargetPackageDescriptor src, TargetPackageDescriptor dst) {
        // support merge only for enabled boolean
        if (src.isEnableSet() && src.isEnabled() != dst.isEnabled()) {
            dst.setEnabled(src.isEnabled());
        }
    }

    // API

    public TargetPackageDescriptor getTargetPackage(String id) {
        return currentContribs.get(id);
    }

    public List<TargetPackageDescriptor> getTargetPackages() {
        List<TargetPackageDescriptor> all = new ArrayList<>();
        all.addAll(currentContribs.values());
        return all;
    }

    public List<TargetPackageDescriptor> getTargetPackages(String targetPlatform) {
        List<TargetPackageDescriptor> tps = new ArrayList<>();
        for (TargetPackageDescriptor desc : currentContribs.values()) {
            List<String> tts = desc.getTargetPlatforms();
            if (tts != null && tts.contains(targetPlatform)) {
                tps.add(desc);
            }
        }
        return tps;
    }

}
