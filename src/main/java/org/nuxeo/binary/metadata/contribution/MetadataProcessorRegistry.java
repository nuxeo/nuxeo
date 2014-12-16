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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.contribution.MetadataProcessorDescriptor} descriptors.
 * 
 * @since 7.1
 */
public class MetadataProcessorRegistry extends ContributionFragmentRegistry<MetadataProcessorDescriptor> {

    private static final Log log = LogFactory.getLog(MetadataProcessorRegistry.class);

    protected Map<String, MetadataProcessorDescriptor> processorDescriptorMap;

    @Override
    public String getContributionId(MetadataProcessorDescriptor metadataProcessorDescriptor) {
        return metadataProcessorDescriptor.getId();
    }

    @Override
    public void contributionUpdated(String s, MetadataProcessorDescriptor metadataProcessorDescriptor,
            MetadataProcessorDescriptor metadataProcessorDescriptor2) {
        this.processorDescriptorMap.put(metadataProcessorDescriptor.getId(), metadataProcessorDescriptor2);
    }

    @Override
    public void contributionRemoved(String s, MetadataProcessorDescriptor metadataProcessorDescriptor) {
        this.processorDescriptorMap.remove(metadataProcessorDescriptor.getId());
    }

    @Override
    public synchronized void addContribution(MetadataProcessorDescriptor metadataProcessorDescriptor) {
        processorDescriptorMap.put(metadataProcessorDescriptor.getId(), metadataProcessorDescriptor);
    }

    /**
     * Not supported.
     */
    @Override
    public MetadataProcessorDescriptor clone(MetadataProcessorDescriptor metadataProcessorDescriptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     */
    @Override
    public void merge(MetadataProcessorDescriptor metadataProcessorDescriptor,
            MetadataProcessorDescriptor metadataProcessorDescriptor2) {
        throw new UnsupportedOperationException();
    }

    public Map<String, MetadataProcessorDescriptor> getProcessorDescriptorMap() {
        return processorDescriptorMap;
    }

    public void setProcessorDescriptorMap(Map<String, MetadataProcessorDescriptor> processorDescriptorMap) {
        this.processorDescriptorMap = processorDescriptorMap;
    }
}
