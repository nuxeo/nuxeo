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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(TopLevelFolderItemFactoryRegistry.class);

    protected static final String CONTRIBUTION_ID = "topLevelFolderItemFactoryContrib";

    protected TopLevelFolderItemFactory factory;

    @Override
    public String getContributionId(TopLevelFolderItemFactoryDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String id,
            TopLevelFolderItemFactoryDescriptor contrib,
            TopLevelFolderItemFactoryDescriptor newOrigContrib) {
        try {
            log.trace(String.format(
                    "Setting top level folder item factory to class %s",
                    contrib.getName()));
            factory = contrib.getFactory();
        } catch (Exception e) {
            throw new ClientRuntimeException(
                    "Cannot update topLevelFolderItemFactory contribution.", e);
        }
    }

    @Override
    public void contributionRemoved(String id,
            TopLevelFolderItemFactoryDescriptor origContrib) {
        log.trace("Removing top level folder item factory (setting it to null)");
        factory = null;
    }

    @Override
    public TopLevelFolderItemFactoryDescriptor clone(
            TopLevelFolderItemFactoryDescriptor orig) {
        log.trace(String.format("Cloning contribution with class name %s",
                orig.getName()));
        TopLevelFolderItemFactoryDescriptor clone = new TopLevelFolderItemFactoryDescriptor();
        clone.factoryClass = orig.factoryClass;
        return clone;
    }

    @Override
    public void merge(TopLevelFolderItemFactoryDescriptor src,
            TopLevelFolderItemFactoryDescriptor dst) {
        log.trace(String.format(
                "Merging contribution with class name %s to contribution with class name %s",
                src.getName(), dst.getName()));
        // Class
        if (src.getFactoryClass() != null
                && !src.getFactoryClass().equals(dst.getFactoryClass())) {
            dst.setFactoryClass(src.getFactoryClass());
        }
        // Parameters
        if (!MapUtils.isEmpty(src.getParameters())) {
            for (String name : src.getParameters().keySet()) {
                dst.setParameter(name, src.getparameter(name));
            }
        }
    }

}
