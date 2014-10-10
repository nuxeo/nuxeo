/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.9.6
 */
public class PageProviderClassReplacerRegistry extends
        ContributionFragmentRegistry<PageProviderClassReplacerDefinition> {

    private static final Log log = LogFactory.getLog(PageProviderClassReplacerRegistry.class);

    protected Map<String, Class<? extends PageProvider>> replacerMap = new HashMap<>();

    @Override
    public String getContributionId(PageProviderClassReplacerDefinition contrib) {
        return contrib.getPageProviderClassName();
    }

    @Override
    public void contributionUpdated(String id, PageProviderClassReplacerDefinition desc,
                                    PageProviderClassReplacerDefinition newOrigContrib) {
        String name = desc.getPageProviderClassName();
        if (name == null) {
            log.error("Cannot register page provider class replacer without class name");
            return;
        }
        if (! desc.isEnabled()) {
            return;
        }
        log.info("Registering page provider class replacer using " + name);
        Class<? extends PageProvider> klass = getPageProviderClass(desc.getPageProviderClassName());
        for (String providerName : desc.getPageProviderNames()) {
            replacerMap.put(providerName, klass);
        }

    }

    @Override
    public void contributionRemoved(String id,
                                    PageProviderClassReplacerDefinition origContrib) {
        log.info("Unregistering page provider replacer for class " + id);
        for (String providerName : origContrib.getPageProviderNames()) {
            replacerMap.remove(providerName);
        }
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public PageProviderClassReplacerDefinition clone(PageProviderClassReplacerDefinition orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(PageProviderClassReplacerDefinition src, PageProviderClassReplacerDefinition dst) {
        throw new UnsupportedOperationException();
    }

    protected Class<? extends PageProvider> getPageProviderClass(
            final String className) {
        Class<? extends PageProvider> ret;
        try {
            ret = (Class<? extends PageProvider>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Class %s not found", className));
        }
        if (!PageProvider.class.isAssignableFrom(ret)) {
            throw new IllegalStateException(String.format(
                    "Class %s does not implement PageProvider interface",
                    className));
        }
        return ret;
    }

    // API
    public Class<? extends PageProvider> getClassForPageProvider(String name) {
        return replacerMap.get(name);
    }

    public void dumpReplacerMap() {
        if (replacerMap.isEmpty()) {
            log.info("No page provider has been superseded");
            return;
        }
        StringBuilder out = new StringBuilder();
        out.append("List of page provider names that are superseded: \n");
        for (String name: replacerMap.keySet()) {
            out.append(String.format("%s: %s\n", name, replacerMap.get(name).getName()));
        }
        log.info(out.toString());
    }
}
