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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.contribution.MetadataRuleDescriptor} descriptors.
 * 
 * @since 7.1
 */
public class MetadataRuleRegistry extends ContributionFragmentRegistry<MetadataRuleDescriptor> {

    private static final Log log = LogFactory.getLog(MetadataRuleRegistry.class);

    public Map<String, MetadataRuleDescriptor> getMetadataRuleDescriptorMap() {
        return metadataRuleDescriptorMap;
    }

    public void setMetadataRuleDescriptorMap(Map<String, MetadataRuleDescriptor> metadataRuleDescriptorMap) {
        this.metadataRuleDescriptorMap = metadataRuleDescriptorMap;
    }

    protected Map<String, MetadataRuleDescriptor> metadataRuleDescriptorMap = new HashMap<>();

    @Override
    public String getContributionId(MetadataRuleDescriptor metadataRuleDescriptor) {
        return metadataRuleDescriptor.getId();
    }

    @Override
    public void contributionUpdated(String s, MetadataRuleDescriptor metadataRuleDescriptor,
            MetadataRuleDescriptor metadataRuleDescriptor2) {
        this.metadataRuleDescriptorMap.put(metadataRuleDescriptor.getId(), metadataRuleDescriptor2);
    }

    @Override
    public synchronized void addContribution(MetadataRuleDescriptor metadataRuleDescriptor) {
        metadataRuleDescriptorMap.put(metadataRuleDescriptor.getId(), metadataRuleDescriptor);
    }

    @Override
    public void contributionRemoved(String s, MetadataRuleDescriptor metadataRuleDescriptor) {
        this.metadataRuleDescriptorMap.remove(metadataRuleDescriptor.getId());
    }

    /**
     * Not supported.
     */
    @Override
    public MetadataRuleDescriptor clone(MetadataRuleDescriptor metadataRuleDescriptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     */
    @Override
    public void merge(MetadataRuleDescriptor metadataRuleDescriptor, MetadataRuleDescriptor metadataRuleDescriptor2) {
        throw new UnsupportedOperationException();
    }
}
