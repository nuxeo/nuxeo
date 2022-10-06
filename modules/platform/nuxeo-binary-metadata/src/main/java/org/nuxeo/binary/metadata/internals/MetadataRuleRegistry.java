/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.internals.MetadataRuleDescriptor} descriptors.
 *
 * @since 7.1
 */
public class MetadataRuleRegistry extends SimpleContributionRegistry<MetadataRuleDescriptor> {

    protected final Set<MetadataRuleDescriptor> contribs = new TreeSet<>(new Comparator<MetadataRuleDescriptor>() {

        @Override
        public int compare(MetadataRuleDescriptor o1, MetadataRuleDescriptor o2) {
            if (o1.getOrder() != null && o2.getOrder() == null) {
                return 1;
            } else if (o1.getOrder() == null && o2.getOrder() != null) {
                return -1;
            } else if (o1.getOrder() != null && o2.getOrder() != null) {
                int order = o1.getOrder().compareTo(o2.getOrder());
                if (order != 0) {
                    return order;
                }
            }
            return o1.getId().compareTo(o2.getId());
        }

    });

    @Override
    public String getContributionId(MetadataRuleDescriptor metadataRuleDescriptor) {
        return metadataRuleDescriptor.getId();
    }

    protected void handleApplicationStarted() {
        contribs.addAll(currentContribs.values());
    }

}
