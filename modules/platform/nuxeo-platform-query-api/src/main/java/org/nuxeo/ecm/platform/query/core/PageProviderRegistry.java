/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
public class PageProviderRegistry extends ContributionFragmentRegistry<PageProviderDefinition> {

    private static final Log log = LogFactory.getLog(PageProviderRegistry.class);

    protected Map<String, PageProviderDefinition> providers = new HashMap<>();

    @Override
    public String getContributionId(PageProviderDefinition contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, PageProviderDefinition desc, PageProviderDefinition newOrigContrib) {
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
    public void contributionRemoved(String id, PageProviderDefinition origContrib) {
        providers.remove(id);
        log.info("Unregistering page provider with name " + id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public PageProviderDefinition clone(PageProviderDefinition orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(PageProviderDefinition src, PageProviderDefinition dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public PageProviderDefinition getPageProvider(String id) {
        return providers.get(id);
    }

}
