/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for resource elements, handling merge on referenced resources.
 *
 * @since 7.3
 */
public class ResourceBundleRegistry extends ContributionFragmentRegistry<ResourceBundle> {

    protected Map<String, ResourceBundle> bundles = new HashMap<>();

    @Override
    public String getContributionId(ResourceBundle contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ResourceBundle contrib, ResourceBundle newOrigContrib) {
        bundles.put(id, contrib);
    }

    @Override
    public synchronized void removeContribution(ResourceBundle contrib) {
        removeContribution(contrib, true);
    }

    @Override
    public void contributionRemoved(String id, ResourceBundle origContrib) {
        bundles.remove(id);
    }

    @Override
    public ResourceBundle clone(ResourceBundle orig) {
        return orig.clone();
    }

    @Override
    public void merge(ResourceBundle src, ResourceBundle dst) {
        dst.merge(src);
    }

    // custom API

    public ResourceBundle getResourceBundle(String id) {
        return bundles.get(id);
    }

    public List<ResourceBundle> getResourceBundles() {
        return new ArrayList<>(bundles.values());
    }

}
