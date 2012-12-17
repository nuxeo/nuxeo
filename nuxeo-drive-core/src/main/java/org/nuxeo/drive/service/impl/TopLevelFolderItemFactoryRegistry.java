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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@code topLevelFolderItemFactory} contributions.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class TopLevelFolderItemFactoryRegistry extends
        ContributionFragmentRegistry<TopLevelFolderItemFactoryDescriptor> {

    protected TopLevelFolderItemFactory factory;

    @Override
    public String getContributionId(TopLevelFolderItemFactoryDescriptor contrib) {
        return contrib.factoryClass.getName();
    }

    @Override
    public void contributionUpdated(String id,
            TopLevelFolderItemFactoryDescriptor contrib,
            TopLevelFolderItemFactoryDescriptor newOrigContrib) {
        try {
            factory = newOrigContrib.getFactory();
        } catch (Exception e) {
            throw new ClientRuntimeException(
                    "Cannot update topLevelFolderItemFactory contribution.", e);
        }
    }

    @Override
    public void contributionRemoved(String id,
            TopLevelFolderItemFactoryDescriptor origContrib) {
        factory = null;
    }

    @Override
    public TopLevelFolderItemFactoryDescriptor clone(
            TopLevelFolderItemFactoryDescriptor orig) {
        TopLevelFolderItemFactoryDescriptor clone = new TopLevelFolderItemFactoryDescriptor();
        clone.factoryClass = orig.factoryClass;
        return clone;
    }

    @Override
    public void merge(TopLevelFolderItemFactoryDescriptor src,
            TopLevelFolderItemFactoryDescriptor dst) {
        // Null merge
    }

}
