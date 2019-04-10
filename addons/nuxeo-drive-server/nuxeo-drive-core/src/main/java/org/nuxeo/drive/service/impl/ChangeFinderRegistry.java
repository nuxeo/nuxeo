/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code changeFinder} contributions.
 *
 * @author Antoine Taillefer
 * @see NuxeoDriveManagerImpl
 * @since 7.3
 */
public class ChangeFinderRegistry extends ContributionFragmentRegistry<ChangeFinderDescriptor> {

    private static final Log log = LogFactory.getLog(ChangeFinderRegistry.class);

    protected static final String CONTRIBUTION_ID = "changeFinderContrib";

    protected FileSystemChangeFinder changeFinder;

    @Override
    public String getContributionId(ChangeFinderDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String id, ChangeFinderDescriptor contrib, ChangeFinderDescriptor newOrigContrib) {
        try {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Updating change finder contribution %s.", contrib));
            }
            changeFinder = contrib.getChangeFinder();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ClientRuntimeException("Cannot update changeFinder contribution.", e);
        }
    }

    @Override
    public void contributionRemoved(String id, ChangeFinderDescriptor origContrib) {
        log.trace("Clearing change finder.");
        changeFinder = null;
    }

    @Override
    public ChangeFinderDescriptor clone(ChangeFinderDescriptor orig) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Cloning contribution %s.", orig));
        }
        ChangeFinderDescriptor clone = new ChangeFinderDescriptor();
        clone.changeFinderClass = orig.changeFinderClass;
        clone.parameters = orig.parameters;
        return clone;
    }

    @Override
    public void merge(ChangeFinderDescriptor src, ChangeFinderDescriptor dst) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Merging contribution %s to contribution %s.", src, dst));
        }
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
