/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@code topLevelFolderItemFactory} contributions.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class TopLevelFolderItemFactoryRegistry
        extends ContributionFragmentRegistry<TopLevelFolderItemFactoryDescriptor> {

    private static final Logger log = LogManager.getLogger(TopLevelFolderItemFactoryRegistry.class);

    protected Map<String, TopLevelFolderItemFactory> factories = new HashMap<>();

    @Override
    public String getContributionId(TopLevelFolderItemFactoryDescriptor contrib) {
        String name = contrib.getName();
        if (StringUtils.isEmpty(name)) {
            throw new NuxeoException("Cannot register topLevelFolderItemFactory without a name.");
        }
        return name;
    }

    @Override
    public void contributionUpdated(String id, TopLevelFolderItemFactoryDescriptor contrib,
            TopLevelFolderItemFactoryDescriptor newOrigContrib) {
        try {
            log.trace("Putting contribution with class name {} in factory registry.", contrib::getName);
            factories.put(id, contrib.getFactory());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NuxeoException("Cannot update topLevelFolderItemFactory contribution.", e);
        }
    }

    @Override
    public void contributionRemoved(String id, TopLevelFolderItemFactoryDescriptor origContrib) {
        log.trace("Removing contribution with class name {} in factory registry.", id);
        factories.remove(id);
    }

    @Override
    public TopLevelFolderItemFactoryDescriptor clone(TopLevelFolderItemFactoryDescriptor orig) {
        log.trace("Cloning contribution with class name {}.", orig::getName);
        TopLevelFolderItemFactoryDescriptor clone = new TopLevelFolderItemFactoryDescriptor();
        clone.factoryClass = orig.factoryClass;
        clone.parameters = orig.parameters;
        return clone;
    }

    @Override
    public void merge(TopLevelFolderItemFactoryDescriptor src, TopLevelFolderItemFactoryDescriptor dst) {
        log.trace("Merging contribution with class name {} to contribution with class name {}", src::getName,
                dst::getName);
        // Class
        if (src.getFactoryClass() != null && !src.getFactoryClass().equals(dst.getFactoryClass())) {
            dst.setFactoryClass(src.getFactoryClass());
        }
        // Parameters
        if (!MapUtils.isEmpty(src.getParameters())) {
            for (String name : src.getParameters().keySet()) {
                dst.setParameter(name, src.getparameter(name));
            }
        }
    }

    protected TopLevelFolderItemFactory getActiveFactory(String activeFactory) {
        return factories.get(activeFactory);
    }
}
