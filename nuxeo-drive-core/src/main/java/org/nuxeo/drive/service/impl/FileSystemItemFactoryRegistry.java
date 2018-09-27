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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@code fileSystemItemFactory} contributions.
 *
 * @author Antoine Taillefer
 * @see FileSystemItemAdapterServiceImpl
 */
public class FileSystemItemFactoryRegistry extends ContributionFragmentRegistry<FileSystemItemFactoryDescriptor> {

    private static final Log log = LogFactory.getLog(FileSystemItemFactoryRegistry.class);

    protected final Map<String, FileSystemItemFactoryDescriptor> factoryDescriptors = new HashMap<>();

    @Override
    public String getContributionId(FileSystemItemFactoryDescriptor contrib) {
        String name = contrib.getName();
        if (StringUtils.isEmpty(name)) {
            throw new NuxeoException("Cannot register fileSystemItemFactory without a name.");
        }
        return name;
    }

    @Override
    public void contributionUpdated(String id, FileSystemItemFactoryDescriptor contrib,
            FileSystemItemFactoryDescriptor newOrigContrib) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Putting contribution %s with id %s in factory registry", contrib, id));
        }
        factoryDescriptors.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, FileSystemItemFactoryDescriptor origContrib) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Removing contribution with id %s from factory registry", id));
        }
        factoryDescriptors.remove(id);
    }

    @Override
    public FileSystemItemFactoryDescriptor clone(FileSystemItemFactoryDescriptor orig) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Cloning contribution with id %s", orig.getName()));
        }
        return SerializationUtils.clone(orig);
    }

    @Override
    public void merge(FileSystemItemFactoryDescriptor src, FileSystemItemFactoryDescriptor dst) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Merging contribution with id %s to contribution with id %s", src.getName(),
                    dst.getName()));
        }
        // Order
        int srcOrder = src.getOrder();
        if (srcOrder > 0 && srcOrder != dst.getOrder()) {
            dst.setOrder(srcOrder);
        }
        // Doc type
        if (!StringUtils.isEmpty(src.getDocType()) && !src.getDocType().equals(dst.getDocType())) {
            dst.setDocType(src.getDocType());
        }
        // Facet
        if (!StringUtils.isEmpty(src.getFacet()) && !src.getFacet().equals(dst.getFacet())) {
            dst.setFacet(src.getFacet());
        }
        // Class
        if (src.getFactoryClass() != null && !src.getFactoryClass().equals(dst.getFactoryClass())) {
            dst.setFactoryClass(src.getFactoryClass());
        }
        // Parameters
        if (!MapUtils.isEmpty(src.getParameters())) {
            for (String name : src.getParameters().keySet()) {
                dst.setParameter(name, src.getParameter(name));
            }
        }
    }

    protected List<FileSystemItemFactoryWrapper> getOrderedActiveFactories(Set<String> activeFactories) {
        List<FileSystemItemFactoryWrapper> factories = new ArrayList<>();
        List<FileSystemItemFactoryDescriptor> orderedFactoryDescriptors = new ArrayList<>(factoryDescriptors.values());
        Collections.sort(orderedFactoryDescriptors);
        for (FileSystemItemFactoryDescriptor factoryDesc : orderedFactoryDescriptors) {
            // Only include active factories
            if (activeFactories.contains(factoryDesc.getName())) {
                FileSystemItemFactoryWrapper factoryWrapper = new FileSystemItemFactoryWrapper(factoryDesc.getDocType(),
                        factoryDesc.getFacet(), factoryDesc.getFactory());
                factories.add(factoryWrapper);
            }
        }
        return factories;
    }

}
