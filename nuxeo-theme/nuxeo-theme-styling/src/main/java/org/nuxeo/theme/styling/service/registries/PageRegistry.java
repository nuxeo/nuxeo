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
        if (orig == null) {
            return null;
        }
        return orig.clone();
    }

    @Override
    public void merge(Page src, Page dst) {
        dst.merge(src);
    }

    public Page getPage(String id) {
        // TODO: merge with potential configurations applying to all pages
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
