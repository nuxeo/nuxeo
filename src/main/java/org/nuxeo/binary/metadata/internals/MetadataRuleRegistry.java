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
package org.nuxeo.binary.metadata.internals;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.internals.MetadataRuleDescriptor} descriptors.
 *
 * @since 7.1
 */
public class MetadataRuleRegistry extends ContributionFragmentRegistry<MetadataRuleDescriptor> {

    protected final Set<MetadataRuleDescriptor> contribs = new TreeSet<>(
            new Comparator<MetadataRuleDescriptor>() {

                @Override
                public int compare(MetadataRuleDescriptor o1, MetadataRuleDescriptor o2) {
                    int order = o1.getOrder().compareTo(o2.getOrder());
                    if (order != 0) {
                        return order;
                    }
                    return o1.getId().compareTo(o2.getId());
                }

            });


    @Override
    public String getContributionId(MetadataRuleDescriptor metadataRuleDescriptor) {
        return metadataRuleDescriptor.getId();
    }

    @Override
    public void contributionUpdated(String s, MetadataRuleDescriptor metadataRuleDescriptor,
            MetadataRuleDescriptor metadataRuleDescriptor2) {
        contribs.add(metadataRuleDescriptor2);
    }

    @Override
    public synchronized void addContribution(MetadataRuleDescriptor metadataRuleDescriptor) {
        contribs.add(metadataRuleDescriptor);
    }

    @Override
    public void contributionRemoved(String s, MetadataRuleDescriptor metadataRuleDescriptor) {
        contribs.remove(metadataRuleDescriptor);
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
