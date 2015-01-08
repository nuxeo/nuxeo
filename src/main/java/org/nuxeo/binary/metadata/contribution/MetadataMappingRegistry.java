/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.contribution;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.contribution.MetadataMappingDescriptor} descriptors.
 *
 * @since 7.1
 */
public class MetadataMappingRegistry extends ContributionFragmentRegistry<MetadataMappingDescriptor> {

    protected Map<String, MetadataMappingDescriptor> mappingDescriptorMap = new HashMap<>();

    @Override
    public String getContributionId(MetadataMappingDescriptor metadataMappingDescriptor) {
        return metadataMappingDescriptor.getId();
    }

    @Override
    public void contributionUpdated(String s, MetadataMappingDescriptor metadataMappingDescriptor,
            MetadataMappingDescriptor metadataMappingDescriptor2) {
        mappingDescriptorMap.put(metadataMappingDescriptor.getId(),
                metadataMappingDescriptor2);
    }

    @Override
    public void contributionRemoved(String s, MetadataMappingDescriptor metadataMappingDescriptor) {
        mappingDescriptorMap.remove(metadataMappingDescriptor.getId());
    }

    @Override
    public synchronized void addContribution(MetadataMappingDescriptor metadataMappingDescriptor) {
        mappingDescriptorMap.put(metadataMappingDescriptor.getId(),
                metadataMappingDescriptor);
    }

    /**
     * Not supported.
     */
    @Override
    public MetadataMappingDescriptor clone(MetadataMappingDescriptor metadataMappingDescriptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     */
    @Override
    public void merge(MetadataMappingDescriptor metadataMappingDescriptor,
            MetadataMappingDescriptor metadataMappingDescriptor2) {
        throw new UnsupportedOperationException();
    }

    public Map<String, MetadataMappingDescriptor> getMappingDescriptorMap() {
        return mappingDescriptorMap;
    }

    public void setMappingDescriptorMap(Map<String,
            MetadataMappingDescriptor> mappingDescriptorMap) {
        this.mappingDescriptorMap = mappingDescriptorMap;
    }
}
