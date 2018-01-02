/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;

/**
 * Registry for theme flavors, handling merge of registered {@link FlavorDescriptor} elements.
 *
 * @since 5.5
 */
public class FlavorRegistry extends ContributionFragmentRegistry<FlavorDescriptor> {

    protected Map<String, FlavorDescriptor> themePageFlavors = new HashMap<String, FlavorDescriptor>();

    @Override
    public String getContributionId(FlavorDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, FlavorDescriptor contrib, FlavorDescriptor newOrigContrib) {
        themePageFlavors.put(id, contrib);
    }

    @Override
    public synchronized void removeContribution(FlavorDescriptor contrib) {
        removeContribution(contrib, true);
    }

    @Override
    public void contributionRemoved(String id, FlavorDescriptor origContrib) {
        themePageFlavors.remove(id);
    }

    @Override
    public FlavorDescriptor clone(FlavorDescriptor orig) {
        if (orig == null) {
            return null;
        }
        return orig.clone();
    }

    @Override
    public void merge(FlavorDescriptor src, FlavorDescriptor dst) {
        dst.merge(src);
    }

    public FlavorDescriptor getFlavor(String id) {
        return themePageFlavors.get(id);
    }

    public List<FlavorDescriptor> getFlavorsExtending(String flavor) {
        List<FlavorDescriptor> res = new ArrayList<FlavorDescriptor>();
        for (FlavorDescriptor f : themePageFlavors.values()) {
            if (f != null) {
                String extendsFlavor = f.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavor) && extendsFlavor.equals(flavor)) {
                    res.add(f);
                }
            }
        }
        return res;
    }

}
