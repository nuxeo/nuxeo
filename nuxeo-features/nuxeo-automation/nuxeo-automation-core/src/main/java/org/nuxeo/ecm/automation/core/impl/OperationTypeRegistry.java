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
package org.nuxeo.ecm.automation.core.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class OperationTypeRegistry extends ContributionFragmentRegistry<OperationType> {

    /**
     * Modifiable operation registry. Modifying the registry is using a lock and it's thread safe. Modifications are
     * removing the cache.
     */
    protected final Map<String, OperationType> operations = new HashMap<String, OperationType>();

    /**
     * Read only cache for operation lookup. Thread safe. Not using synchronization if cache already created.
     */
    protected volatile Map<String, OperationType> lookup;

    @Override
    public String getContributionId(OperationType contrib) {
        return contrib.getId();
    }

    public synchronized void addContribution(OperationType op, boolean replace) throws OperationException {
        if (!replace && operations.containsKey(op.getId())) {
            throw new OperationException("An operation is already bound to: " + op.getId()
                    + ". Use 'replace=true' to replace an existing operation");
        }
        super.addContribution(op);
    }

    @Override
    public void contributionUpdated(String id, OperationType contrib, OperationType newOrigContrib) {
        operations.put(id, contrib);
        for (String alias : contrib.getAliases()) {
            operations.put(alias, contrib);
        }
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, OperationType origContrib) {
        operations.remove(id);
        lookup = null;
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public OperationType clone(OperationType orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(OperationType src, OperationType dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public OperationType getOperationType(Class<?> key) {
        return lookup().get(key.getAnnotation(Operation.class).id());
    }

    public Map<String, OperationType> lookup() {
        if (lookup == null) {
            synchronized (this) {
                if (lookup == null) {
                    lookup = new HashMap<String, OperationType>(operations);
                }
            }
        }
        return lookup;
    }

}
