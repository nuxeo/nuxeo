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
        clone.setAppendStyles(orig.getAppendStyles());
        List<String> styles = orig.getStyles();
        if (styles != null) {
            clone.setStyles(new ArrayList<String>(styles));
        }
        clone.setAppendFlavors(orig.getAppendFlavors());
        List<String> flavors = orig.getFlavors();
        if (flavors != null) {
            clone.setFlavors(new ArrayList<String>(flavors));
        }
        clone.setAppendResources(orig.getAppendResources());
        List<String> resources = orig.getResources();
        if (resources != null) {
            clone.setResources(new ArrayList<String>(resources));
        }
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
        if (newStyles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newStyles);
            boolean keepOld = src.getAppendStyles()
                    || (newStyles.isEmpty() && !src.getAppendStyles());
            if (keepOld) {
                // add back old contributions
                List<String> oldStyles = dst.getStyles();
                if (oldStyles != null) {
                    merged.addAll(0, oldStyles);
                }
            }
            dst.setStyles(merged);
        }

        List<String> newFlavors = src.getFlavors();
        if (newFlavors != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newFlavors);
            boolean keepOld = src.getAppendFlavors()
                    || (newFlavors.isEmpty() && !src.getAppendFlavors());
            if (keepOld) {
                // add back old contributions
                List<String> oldFlavors = dst.getFlavors();
                if (oldFlavors != null) {
                    merged.addAll(0, oldFlavors);
                }
            }
            dst.setFlavors(merged);
        }

        List<String> newResources = src.getResources();
        if (newResources != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newResources);
            boolean keepOld = src.getAppendResources()
                    || (newResources.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldResources = dst.getResources();
                if (oldResources != null) {
                    merged.addAll(0, oldResources);
                }
            }
            dst.setResources(merged);
        }
    }

    public ThemePage getThemePage(String id) {
        return themePageResources.get(id);
    }

    public List<ThemePage> getThemePages() {
        List<ThemePage> res = new ArrayList<ThemePage>();
        for (ThemePage page : themePageResources.values()) {
            if (page != null) {
                res.add(page);
            }
        }
        return res;
    }

    public ThemePage getConfigurationApplyingToAllThemes() {
        return themePageResources.get("*");
    }

}
