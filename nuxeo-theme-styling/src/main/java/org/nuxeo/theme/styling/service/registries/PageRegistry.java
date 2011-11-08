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

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.ThemePage;

/**
 * Registry for theme page resources, handling merge of registered
 * {@link ThemePage} elements.
 *
 * @since 5.5
 */
public class PageRegistry extends ContributionFragmentRegistry<ThemePage> {

    protected Map<String, ThemePage> themePageResources = new HashMap<String, ThemePage>();

    @Override
    public String getContributionId(ThemePage contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ThemePage contrib,
            ThemePage newOrigContrib) {
        themePageResources.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, ThemePage origContrib) {
        themePageResources.remove(id);
    }

    @Override
    public ThemePage clone(ThemePage orig) {
        ThemePage clone = new ThemePage();
        clone.setName(orig.getName());
        clone.setDefaultFlavor(orig.getDefaultFlavor());
        clone.setStyles(orig.getStyles());
        clone.setFlavors(orig.getFlavors());
        clone.setResources(orig.getResources());
        clone.setLoaded(orig.isLoaded());
        return clone;
    }

    @Override
    public void merge(ThemePage src, ThemePage dst) {

        String newFlavor = src.getDefaultFlavor();
        if (newFlavor != null) {
            dst.setDefaultFlavor(newFlavor);
        }

        List<String> newStyles = src.getStyles();
        if (newStyles.isEmpty() || src.getAppendStyles()) {
            List<String> existingStyles = dst.getStyles();
            if (existingStyles != null) {
                newStyles.addAll(0, existingStyles);
            }
        }
        dst.setStyles(newStyles);

        List<String> newFlavors = src.getFlavors();
        if (newFlavors.isEmpty() || src.getAppendFlavors()) {
            List<String> existingFlavors = dst.getFlavors();
            if (existingFlavors != null) {
                newFlavors.addAll(0, existingFlavors);
            }
        }
        dst.setFlavors(newFlavors);

        List<String> newResources = src.getResources();
        if (newResources.isEmpty() || src.getAppendResources()) {
            List<String> existingResources = dst.getResources();
            if (existingResources != null) {
                newResources.addAll(0, existingResources);
            }
        }
        dst.setResources(newResources);
    }

    public List<ThemePage> getThemePages() {
        List<ThemePage> res = new ArrayList<ThemePage>();
        for (String themePageName : themePageResources.keySet()) {
            ThemePage item = getContribution(themePageName);
            if (item != null) {
                res.add(item);
            }
        }
        return res;
    }

}
