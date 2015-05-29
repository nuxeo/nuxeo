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
import org.nuxeo.theme.styling.service.descriptors.Page;

/**
 * Registry for theme page resources, handling merge of registered {@link Page} elements.
 *
 * @since 5.5
 */
public class PageRegistry extends ContributionFragmentRegistry<Page> {

    protected Map<String, Page> pageResources = new HashMap<String, Page>();

    @Override
    public String getContributionId(Page contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, Page contrib, Page newOrigContrib) {
        pageResources.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, Page origContrib) {
        pageResources.remove(id);
    }

    @Override
    public Page clone(Page orig) {
        Page clone = new Page();
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
        List<String> bundles = orig.getResourceBundles();
        if (bundles != null) {
            clone.setResourceBundles(new ArrayList<String>(bundles));
        }
        clone.setLoaded(orig.isLoaded());
        return clone;
    }

    @Override
    public void merge(Page src, Page dst) {
        String newFlavor = src.getDefaultFlavor();
        if (newFlavor != null) {
            dst.setDefaultFlavor(newFlavor);
        }

        List<String> newStyles = src.getStyles();
        if (newStyles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newStyles);
            boolean keepOld = src.getAppendStyles() || (newStyles.isEmpty() && !src.getAppendStyles());
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
            boolean keepOld = src.getAppendFlavors() || (newFlavors.isEmpty() && !src.getAppendFlavors());
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
            boolean keepOld = src.getAppendResources() || (newResources.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldResources = dst.getResources();
                if (oldResources != null) {
                    merged.addAll(0, oldResources);
                }
            }
            dst.setResources(merged);
        }

        List<String> newBundles = src.getResourceBundles();
        if (newBundles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newBundles);
            boolean keepOld = src.getAppendResources() || (newBundles.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldBundles = dst.getResourceBundles();
                if (oldBundles != null) {
                    merged.addAll(0, oldBundles);
                }
            }
            dst.setResourceBundles(merged);
        }
    }

    public Page getPage(String id) {
        return pageResources.get(id);
    }

    /**
     * @deprecated since 7.3: use {@link #getPage(String)} instead.
     */
    @Deprecated
    public Page getThemePage(String id) {
        return getPage(id);
    }

    public List<Page> getPages() {
        List<Page> res = new ArrayList<Page>();
        for (Page page : pageResources.values()) {
            if (page != null) {
                res.add(page);
            }
        }
        return res;
    }

    /**
     * @deprecated since 7.3: use {@link #getPages()} instead.
     */
    @Deprecated
    public List<Page> getThemePages() {
        return getPages();
    }

    public Page getConfigurationApplyingToAll() {
        return pageResources.get("*");
    }

    /**
     * @deprecated since 7.3: use {@link #getConfigurationApplyingToAll()} instead.
     */
    public Page getConfigurationApplyingToAllThemes() {
        return getConfigurationApplyingToAll();
    }

}
