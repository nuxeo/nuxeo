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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code activeFileSystemItemFactories} contributions.
 * 
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class ActiveFileSystemItemFactoryRegistry extends
        ContributionFragmentRegistry<ActiveFileSystemItemFactoriesDescriptor> {

    private static final Log log = LogFactory.getLog(ActiveFileSystemItemFactoryRegistry.class);

    protected static final String CONTRIBUTION_ID = "activeFileSystemItemFactoriesContrib";

    protected Set<String> activeFactories = new HashSet<String>();

    @Override
    public String getContributionId(ActiveFileSystemItemFactoriesDescriptor contrib) {
        return CONTRIBUTION_ID;
    }

    @Override
    public void contributionUpdated(String id, ActiveFileSystemItemFactoriesDescriptor contrib,
            ActiveFileSystemItemFactoriesDescriptor newOrigContrib) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Updating activeFileSystemItemFactories contribution %s.", contrib));
        }
        if (contrib.isMerge()) {
            // Merge active factories
            for (ActiveFileSystemItemFactoryDescriptor factory : contrib.getFactories()) {
                if (activeFactories.contains(factory.getName()) && !factory.isEnabled()) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Removing factory %s from active factories.", factory.getName()));
                    }
                    activeFactories.remove(factory.getName());
                }
                if (!activeFactories.contains(factory.getName()) && factory.isEnabled()) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Adding factory %s to active factories.", factory.getName()));
                    }
                    activeFactories.add(factory.getName());
                }
            }
        } else {
            // No merge, reset active factories
            if (log.isTraceEnabled()) {
                log.trace(String.format("Clearing active factories as contribution %s doesn't merge.", contrib));
            }
            activeFactories.clear();
            for (ActiveFileSystemItemFactoryDescriptor factory : contrib.getFactories()) {
                if (factory.isEnabled()) {
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Adding factory %s to active factories.", factory.getName()));
                    }
                    activeFactories.add(factory.getName());
                }
            }
        }
    }

    @Override
    public void contributionRemoved(String id, ActiveFileSystemItemFactoriesDescriptor origContrib) {
        log.trace("Clearing active factories.");
        activeFactories.clear();
    }

    @Override
    public ActiveFileSystemItemFactoriesDescriptor clone(ActiveFileSystemItemFactoriesDescriptor orig) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Cloning contribution %s.", orig));
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(orig);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (ActiveFileSystemItemFactoriesDescriptor) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new ClientRuntimeException(String.format("Cannot clone contribution %s.", orig), e);
        }
    }

    @Override
    public void merge(ActiveFileSystemItemFactoriesDescriptor src, ActiveFileSystemItemFactoriesDescriptor dst) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Merging contribution %s to contribution %s.", src, dst));
        }
        // Merge
        if (src.isMerge() != dst.isMerge()) {
            dst.setMerge(src.isMerge());
        }
        // Factories
        for (ActiveFileSystemItemFactoryDescriptor factory : src.getFactories()) {
            int indexOfFactory = dst.getFactories().indexOf(factory);
            if (indexOfFactory > -1) {
                dst.getFactories().get(indexOfFactory).setEnabled(factory.isEnabled());
            } else {
                dst.getFactories().add(factory);
            }
        }
    }
}
