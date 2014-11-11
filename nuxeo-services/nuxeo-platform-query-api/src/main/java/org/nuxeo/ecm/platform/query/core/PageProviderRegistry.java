/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.query.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for page provider contributions.
 *
 * @since 5.6
 */
public class PageProviderRegistry extends
        ContributionFragmentRegistry<PageProviderDefinition> {

    private static final Log log = LogFactory.getLog(PageProviderRegistry.class);

    protected Map<String, PageProviderDefinition> providers = new HashMap<String, PageProviderDefinition>();

    @Override
    public String getContributionId(PageProviderDefinition contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, PageProviderDefinition desc,
            PageProviderDefinition newOrigContrib) {
        String name = desc.getName();
        if (name == null) {
            log.error("Cannot register page provider without a name");
            return;
        }
        boolean enabled = desc.isEnabled();
        if (enabled) {
            log.info("Registering page provider with name " + name);
            providers.put(name, desc);
        } else {
            contributionRemoved(id, desc);
        }
    }

    @Override
    public void contributionRemoved(String id,
            PageProviderDefinition origContrib) {
        providers.remove(id);
        log.info("Unregistering page provider with name " + id);
    }

    @Override
    public PageProviderDefinition clone(PageProviderDefinition orig) {
        return orig.clone();
    }

    @Override
    public void merge(PageProviderDefinition src, PageProviderDefinition dst) {
        // limited merge: only updates the enable flag
        dst.setEnabled(src.isEnabled());
    }

    // API

    public PageProviderDefinition getPageProvider(String id) {
        return providers.get(id);
    }

}
