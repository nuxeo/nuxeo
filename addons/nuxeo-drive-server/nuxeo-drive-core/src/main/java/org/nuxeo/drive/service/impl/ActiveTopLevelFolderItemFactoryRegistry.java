/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code activeTopLevelFolderItemFactory} contributions.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class ActiveTopLevelFolderItemFactoryRegistry
        extends ContributionFragmentRegistry<ActiveTopLevelFolderItemFactoryDescriptor> {

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
