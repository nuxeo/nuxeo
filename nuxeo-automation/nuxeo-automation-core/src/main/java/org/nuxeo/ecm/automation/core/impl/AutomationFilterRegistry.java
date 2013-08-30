/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl;

import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 5.7.3
 */
public class AutomationFilterRegistry extends
        ContributionFragmentRegistry<AutomationFilter> {

    /**
     * Modifiable exception chain registry. Modifying the registry is using a
     * lock and it's thread safe. Modifications are removing the cache.
     */
    protected final Map<String, AutomationFilter> automationFilters = new HashMap<>();

    /**
     * Read only cache for exception chain lookup. Thread safe. Not using
     * synchronization if cache already created.
     */
    protected volatile Map<String, AutomationFilter> lookup;

    public synchronized void addContribution(AutomationFilter automationFilter,
            boolean replace) throws OperationException {
        if (!replace && automationFilters.containsKey(automationFilter.getId())) {
            throw new OperationException(
                    "An automation filter is already bound to: "
                            + automationFilter.getId()
                            + ". Use 'replace=true' to replace an existing automation filter");
        }
        super.addContribution(automationFilter);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public String getContributionId(AutomationFilter contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, AutomationFilter contrib,
                                    AutomationFilter newOrigContrib) {
        automationFilters.put(id, contrib);
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, AutomationFilter origContrib) {
        automationFilters.remove(id);
        lookup = null;
    }

    @Override
    public AutomationFilter clone(AutomationFilter orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(AutomationFilter src, AutomationFilter dst) {
        throw new UnsupportedOperationException();
    }

    public Map<String, AutomationFilter> lookup() {
        Map<String, AutomationFilter> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<>(automationFilters);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

    public AutomationFilter getAutomationFilter(String id) {
        return automationFilters.get(id);
    }
}
