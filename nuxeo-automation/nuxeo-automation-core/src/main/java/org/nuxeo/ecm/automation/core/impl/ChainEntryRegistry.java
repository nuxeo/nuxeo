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
package org.nuxeo.ecm.automation.core.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl.ChainEntry;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for chain entries
 *
 * @since 5.6
 */
public class ChainEntryRegistry extends
        ContributionFragmentRegistry<ChainEntry> {

    /**
     * Modifiable chain registry
     */
    protected final Map<String, ChainEntry> chains = new HashMap<String, OperationServiceImpl.ChainEntry>();

    /**
     * Read only cache for managed chains
     */
    protected volatile Map<String, ChainEntry> lookup;

    @Override
    public String getContributionId(ChainEntry contrib) {
        return contrib.chain.getId();
    }

    public synchronized void addContribution(ChainEntry chain, boolean replace)
            throws OperationException {
        String id = chain.chain.getId();
        if (!replace && chains.containsKey(id)) {
            throw new OperationException("Chain with id " + id
                    + " already exists");
        }
        super.addContribution(chain);
    }

    @Override
    public void contributionUpdated(String id, ChainEntry contrib,
            ChainEntry newOrigContrib) {
        chains.put(id, contrib);
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, ChainEntry origContrib) {
        if (chains.remove(id) != null) {
            lookup = null;
        }
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public ChainEntry clone(ChainEntry orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(ChainEntry src, ChainEntry dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public ChainEntry getChainEntry(String id) {
        return chains.get(id);
    }

    public synchronized void flushCompiledChains() {
        for (ChainEntry entry : chains.values()) {
            entry.cchain = null;
        }
        lookup = null;
    }

    public Map<String, ChainEntry> lookup() {
        Map<String, ChainEntry> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<String, ChainEntry>(chains);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

}
