/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;

/**
 * Registry for theme flavors, handling merge of registered {@link Flavor}
 * elements.
 *
 * @since 5.5
 */
public class FlavorRegistry extends ContributionFragmentRegistry<Flavor> {

    protected Map<String, Flavor> themePageFlavors = new HashMap<String, Flavor>();

    @Override
    public String getContributionId(Flavor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, Flavor contrib,
            Flavor newOrigContrib) {
        themePageFlavors.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, Flavor origContrib) {
        themePageFlavors.remove(id);
    }

    @Override
    public Flavor clone(Flavor orig) {
        Flavor clone = new Flavor();
        clone.setName(orig.getName());
        clone.setExtendsFlavor(orig.getExtendsFlavor());
        List<FlavorPresets> presets = orig.getPresets();
        if (presets != null) {
            List<FlavorPresets> newPresets = new ArrayList<FlavorPresets>();
            for (FlavorPresets item : presets) {
                newPresets.add(item.clone());
            }
            clone.setPresets(newPresets);
        }
        return clone;
    }

    @Override
    public void merge(Flavor src, Flavor dst) {
        if (src.getAppendPresets()) {
            String newExtend = dst.getExtendsFlavor();
            if (newExtend != null) {
                dst.setExtendsFlavor(newExtend);
            }

            List<FlavorPresets> newPresets = dst.getPresets();
            if (newPresets == null) {
                newPresets = new ArrayList<FlavorPresets>();
            }
            // merge
            List<FlavorPresets> presets = src.getPresets();
            if (presets != null) {
                newPresets.addAll(presets);
            }
            dst.setPresets(newPresets);
        }
    }

    public List<Flavor> getFlavorsExtending(String flavor) {
        List<Flavor> res = new ArrayList<Flavor>();
        for (String fName : themePageFlavors.keySet()) {
            Flavor f = getContribution(fName);
            if (f != null) {
                String extendsFlavor = f.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavor)
                        && extendsFlavor.equals(flavor)) {
                    res.add(f);
                }
            }
        }
        return res;
    }

}
