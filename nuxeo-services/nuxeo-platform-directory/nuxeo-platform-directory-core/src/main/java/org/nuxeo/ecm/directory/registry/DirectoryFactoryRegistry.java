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
import java.util.List;

import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryFactoryProxy;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Tracks available factories.
 *
 * @since 5.6
 */
public class DirectoryFactoryRegistry extends
        SimpleContributionRegistry<DirectoryFactory> {

    @Override
    public String getContributionId(DirectoryFactory contrib) {
        if (contrib instanceof DirectoryFactoryProxy) {
            return ((DirectoryFactoryProxy) contrib).getComponentName();
        }
        return contrib.getName();
    }

    // API

    public List<DirectoryFactory> getFactories() {
        List<DirectoryFactory> facts = new ArrayList<DirectoryFactory>();
        if (currentContribs != null) {
            facts.addAll(currentContribs.values());
        }
        return facts;
    }

    public DirectoryFactory getFactory(String name) {
        return getCurrentContribution(name);
    }

}
