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
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;

/**
 * Registry for theme page resources, handling merge of registered {@link PageDescriptor} elements.
 *
 * @since 5.5
 */
public class PageRegistry extends ContributionFragmentRegistry<PageDescriptor> {

    protected Map<String, PageDescriptor> pageResources = new HashMap<String, PageDescriptor>();

    @Override
    public String getContributionId(PageDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, PageDescriptor contrib, PageDescriptor newOrigContrib) {
        pageResources.put(id, contrib);
    }

    @Override
    public synchronized void removeContribution(PageDescriptor contrib) {
        removeContribution(contrib, true);
    }

    @Override
    public void contributionRemoved(String id, PageDescriptor origContrib) {
        pageResources.remove(id);
    }

    @Override
    public PageDescriptor clone(PageDescriptor orig) {
        if (orig == null) {
            return null;
        }
        return orig.clone();
    }

    @Override
    public void merge(PageDescriptor src, PageDescriptor dst) {
        dst.merge(src);
    }

    public PageDescriptor getPage(String id) {
        return pageResources.get(id);
    }

    /**
     * @deprecated since 7.4: use {@link #getPage(String)} instead.
     */
    @Deprecated
    public PageDescriptor getThemePage(String id) {
        return getPage(id);
    }

    public List<PageDescriptor> getPages() {
        List<PageDescriptor> res = new ArrayList<PageDescriptor>();
        for (PageDescriptor page : pageResources.values()) {
            if (page != null) {
                res.add(page);
            }
        }
        return res;
    }

    /**
     * Returns all the page names.
     *
     * @since 7.10
     */
    public List<String> getPageNames() {
        return new ArrayList<String>(pageResources.keySet());
    }

    /**
     * @deprecated since 7.4: use {@link #getPages()} instead.
     */
    @Deprecated
    public List<PageDescriptor> getThemePages() {
        return getPages();
    }

    public PageDescriptor getConfigurationApplyingToAll() {
        return pageResources.get("*");
    }

    /**
     * @deprecated since 7.4: use {@link #getConfigurationApplyingToAll()} instead.
     */
    public PageDescriptor getConfigurationApplyingToAllThemes() {
        return getConfigurationApplyingToAll();
    }

}
