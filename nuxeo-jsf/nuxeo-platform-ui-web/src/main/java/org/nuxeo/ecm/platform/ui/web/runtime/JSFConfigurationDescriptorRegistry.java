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

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for JSF configuration contributions.
 *
 * @since 7.4
 */
public class JSFConfigurationDescriptorRegistry extends SimpleContributionRegistry<JSFConfigurationDescriptor> {

    protected static final String CONTRIBUTION_ID = "JSFConfigurationsContrib";

    protected Map<String, String> properties = new HashMap<>();

    @Override
    public String getContributionId(JSFConfigurationDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String key, JSFConfigurationDescriptor contrib,
        JSFConfigurationDescriptor newOrigContrib) {
        properties.putAll(contrib.getProperties());
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

    public String getProperty(String key) {
        return properties.get(key);
    }
}
