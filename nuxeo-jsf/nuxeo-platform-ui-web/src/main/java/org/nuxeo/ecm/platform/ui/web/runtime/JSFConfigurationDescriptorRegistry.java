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
 *      Andre Justo
 */
package org.nuxeo.ecm.platform.ui.web.runtime;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * @since 7.4
 */
public class JSFConfigurationDescriptorRegistry extends SimpleContributionRegistry<JSFConfigurationDescriptor> {

    @Override
    public String getContributionId(JSFConfigurationDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String key, JSFConfigurationDescriptor contrib,
        JSFConfigurationDescriptor newOrigContrib) {
        if (currentContribs.containsKey(key)) {
            currentContribs.remove(key);
        }
        currentContribs.put(key, contrib);
    }

    @Override
    public JSFConfigurationDescriptor clone(JSFConfigurationDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(JSFConfigurationDescriptor src, JSFConfigurationDescriptor dst) {
        dst.merge(src);
    }

    @Override
    public boolean isSupportingMerge() {
        return true;
    }

    public JSFConfigurationDescriptor lookup(String key) {
        return currentContribs.get(key);
    }
}
