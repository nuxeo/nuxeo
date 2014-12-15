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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code activeTopLevelFolderItemFactory} contributions.
 * 
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class ActiveTopLevelFolderItemFactoryRegistry extends
        ContributionFragmentRegistry<ActiveTopLevelFolderItemFactoryDescriptor> {

    private static final Log log = LogFactory.getLog(ActiveTopLevelFolderItemFactoryRegistry.class);

    protected static final String CONTRIBUTION_ID = "activeTopLevelFolderItemFactoriesContrib";

    protected String activeFactory;

    @Override
    public String getContributionId(ActiveTopLevelFolderItemFactoryDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String id, ActiveTopLevelFolderItemFactoryDescriptor contrib,
            ActiveTopLevelFolderItemFactoryDescriptor newOrigContrib) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Updating activeTopLevelFolderItemFactory contribution %s.", contrib));
            log.trace(String.format("Setting active factory to %s.", contrib.getName()));
        }
        activeFactory = contrib.getName();
    }

    @Override
    public void contributionRemoved(String id, ActiveTopLevelFolderItemFactoryDescriptor origContrib) {
        log.trace("Clearing active factory.");
        activeFactory = null;
    }

    @Override
    public ActiveTopLevelFolderItemFactoryDescriptor clone(ActiveTopLevelFolderItemFactoryDescriptor orig) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Cloning contribution %s.", orig));
        }
        ActiveTopLevelFolderItemFactoryDescriptor clone = new ActiveTopLevelFolderItemFactoryDescriptor();
        clone.name = orig.name;
        return clone;
    }

    @Override
    public void merge(ActiveTopLevelFolderItemFactoryDescriptor src, ActiveTopLevelFolderItemFactoryDescriptor dst) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Merging contribution %s to contribution %s.", src, dst));
        }
        if (!StringUtils.isEmpty(src.getName()) && !src.getName().equals(dst.getName())) {
            dst.setName(src.getName());
        }
    }
}
