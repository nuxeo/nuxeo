/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@code activeFileSystemItemFactories} contributions.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class ActiveFileSystemItemFactoryRegistry
        extends ContributionFragmentRegistry<ActiveFileSystemItemFactoriesDescriptor> {

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
        return SerializationUtils.clone(orig);
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
