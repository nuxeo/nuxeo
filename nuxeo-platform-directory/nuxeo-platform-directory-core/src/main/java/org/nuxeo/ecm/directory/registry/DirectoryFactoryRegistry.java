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
package org.nuxeo.ecm.directory.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryFactoryProxy;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Tracks available factories.
 *
 * @since 5.6
 */
public class DirectoryFactoryRegistry extends
        ContributionFragmentRegistry<DirectoryFactory> {

    private Map<String, DirectoryFactory> factories = new HashMap<String, DirectoryFactory>();

    @Override
    public String getContributionId(DirectoryFactory contrib) {
        if (contrib instanceof DirectoryFactoryProxy) {
            return ((DirectoryFactoryProxy) contrib).getComponentName();
        }
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, DirectoryFactory contrib,
            DirectoryFactory newOrigContrib) {
        factories.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, DirectoryFactory origContrib) {
        factories.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public DirectoryFactory clone(DirectoryFactory orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(DirectoryFactory src, DirectoryFactory dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public List<DirectoryFactory> getFactories() {
        List<DirectoryFactory> facts = new ArrayList<DirectoryFactory>();
        if (factories != null) {
            facts.addAll(factories.values());
        }
        return facts;
    }

    public DirectoryFactory getFactory(String name) {
        return factories.get(name);
    }

}
