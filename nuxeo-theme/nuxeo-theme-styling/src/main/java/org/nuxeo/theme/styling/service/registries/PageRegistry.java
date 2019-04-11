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

import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;

/**
 * Registry for theme page resources, handling merge of registered {@link PageDescriptor} elements.
 *
 * @since 5.5
 */
public class PageRegistry extends ContributionFragmentRegistry<PageDescriptor> {

    protected Map<String, PageDescriptor> pageResources = new HashMap<>();

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
        List<PageDescriptor> res = new ArrayList<>();
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
        return new ArrayList<>(pageResources.keySet());
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
    @Deprecated
    public PageDescriptor getConfigurationApplyingToAllThemes() {
        return getConfigurationApplyingToAll();
    }

}
