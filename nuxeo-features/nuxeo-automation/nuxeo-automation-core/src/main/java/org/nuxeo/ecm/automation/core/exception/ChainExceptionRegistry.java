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
package org.nuxeo.ecm.automation.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.7.3
 */
public class ChainExceptionRegistry extends
        ContributionFragmentRegistry<ChainException> {

    /**
     * Modifiable automation filter registry. Modifying the registry is using a
     * lock and it's thread safe. Modifications are removing the cache.
     */
    protected final Map<String, ChainException> chainExceptions = new HashMap<>();

    /**
     * Read only cache for automation filter lookup. Thread safe. Not using
     * synchronization if cache already created.
     */
    protected volatile Map<String, ChainException> lookup;

    public synchronized void addContribution(ChainException chainException,
            boolean replace) throws OperationException {
        if (!replace && chainExceptions.containsKey(chainException.getId())) {
            throw new OperationException(
                    "An exception chain is already bound to: "
                            + chainException.getId()
                            + ". Use 'replace=true' to replace an existing exception chain");
        }
        super.addContribution(chainException);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public String getContributionId(ChainException contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, ChainException contrib,
            ChainException newOrigContrib) {
        chainExceptions.put(id, contrib);
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, ChainException origContrib) {
        chainExceptions.remove(id);
        lookup = null;
    }

    @Override
    public ChainException clone(ChainException orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(ChainException src, ChainException dst) {
        throw new UnsupportedOperationException();
    }

    public Map<String, ChainException> lookup() {
        Map<String, ChainException> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<String, ChainException>(chainExceptions);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

    public ChainException getChainException(String onChainId) {
        for (ChainException chainException : lookup().values()) {
            if (onChainId.equals(chainException.getOnChainId())) {
                return chainException;
            }
        }
        return null;
    }
}
