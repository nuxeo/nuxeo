/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    protected Map<String, ResourceBundle> bundles = new HashMap<String, ResourceBundle>();

    @Override
    public String getContributionId(ResourceBundle contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, ResourceBundle contrib, ResourceBundle newOrigContrib) {
        bundles.put(id, contrib);
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
        return new ArrayList<ResourceBundle>(bundles.values());
    }

}
