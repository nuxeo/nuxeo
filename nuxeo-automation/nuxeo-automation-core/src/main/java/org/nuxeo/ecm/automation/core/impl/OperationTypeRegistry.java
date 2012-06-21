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
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class OperationTypeRegistry extends
        ContributionFragmentRegistry<OperationType> {

    /**
     * Modifiable operation registry. Modifying the registry is using a lock
     * and it's thread safe. Modifications are removing the cache.
     */
    protected final Map<String, OperationType> operations = new HashMap<String, OperationType>();

    /**
     * Read only cache for operation lookup. Thread safe. Not using
     * synchronization if cache already created.
     */
    protected volatile Map<String, OperationType> lookup;

    @Override
    public String getContributionId(OperationType contrib) {
        return contrib.getId();
    }

    public synchronized void addContribution(OperationType op, boolean replace)
            throws OperationException {
        if (!replace && operations.containsKey(op.getId())) {
            throw new OperationException("An operation is already bound to: "
                    + op.getId()
                    + ". Use 'replace=true' to replace an existing operation");
        }
        super.addContribution(op);
    }

    @Override
    public void contributionUpdated(String id, OperationType contrib,
            OperationType newOrigContrib) {
        operations.put(id, contrib);
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
        return operations.get(key.getAnnotation(Operation.class).id());
    }

    public Map<String, OperationType> lookup() {
        Map<String, OperationType> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<String, OperationType>(operations);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

}
