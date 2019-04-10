/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code changeFinder} contributions.
 *
 * @author Antoine Taillefer
 * @see NuxeoDriveManagerImpl
 * @since 7.3
 */
public class ChangeFinderRegistry extends ContributionFragmentRegistry<ChangeFinderDescriptor> {

    private static final Logger log = LogManager.getLogger(ChangeFinderRegistry.class);

    protected static final String CONTRIBUTION_ID = "changeFinderContrib";

    protected FileSystemChangeFinder changeFinder;

    @Override
    public String getContributionId(ChangeFinderDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String id, ChangeFinderDescriptor contrib, ChangeFinderDescriptor newOrigContrib) {
        try {
            log.trace("Updating change finder contribution {}.", contrib);
            changeFinder = contrib.getChangeFinder();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NuxeoException("Cannot update changeFinder contribution.", e);
        }
    }

    @Override
    public void contributionRemoved(String id, ChangeFinderDescriptor origContrib) {
        log.trace("Clearing change finder.");
        changeFinder = null;
    }

    @Override
    public ChangeFinderDescriptor clone(ChangeFinderDescriptor orig) {
        log.trace("Cloning contribution {}.", orig);
        ChangeFinderDescriptor clone = new ChangeFinderDescriptor();
        clone.changeFinderClass = orig.changeFinderClass;
        clone.parameters = orig.parameters;
        return clone;
    }

    @Override
    public void merge(ChangeFinderDescriptor src, ChangeFinderDescriptor dst) {
        log.trace("Merging contribution {} to contribution {}.", src, dst);
        // Class
        if (src.getChangeFinderClass() != null && !src.getChangeFinderClass().equals(dst.getChangeFinderClass())) {
            dst.setChangeFinderClass(src.getChangeFinderClass());
        }
        // Parameters
        if (!MapUtils.isEmpty(src.getParameters())) {
            for (String name : src.getParameters().keySet()) {
                dst.setParameter(name, src.getparameter(name));
            }
        }
    }
}
