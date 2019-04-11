/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
public class ChainExceptionRegistry extends ContributionFragmentRegistry<ChainException> {

    /**
     * Modifiable automation filter registry. Modifying the registry is using a lock and it's thread safe. Modifications
     * are removing the cache.
     */
    protected final Map<String, ChainException> chainExceptions = new HashMap<>();

    /**
     * Read only cache for automation filter lookup. Thread safe. Not using synchronization if cache already created.
     */
    protected volatile Map<String, ChainException> lookup;

    public synchronized void addContribution(ChainException chainException, boolean replace) throws OperationException {
        if (!replace && chainExceptions.containsKey(chainException.getId())) {
            throw new OperationException("An exception chain is already bound to: " + chainException.getId()
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
    public void contributionUpdated(String id, ChainException contrib, ChainException newOrigContrib) {
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
                lookup = new HashMap<>(chainExceptions);
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
