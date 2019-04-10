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

import java.util.Map;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.internals.MetadataMappingDescriptor} descriptors.
 *
 * @since 7.1
 */
public class MetadataMappingRegistry extends SimpleContributionRegistry<MetadataMappingDescriptor> {

    @Override
    public String getContributionId(MetadataMappingDescriptor metadataMappingDescriptor) {
        return metadataMappingDescriptor.getId();
    }

    public Map<String, MetadataMappingDescriptor> getMappingDescriptorMap() {
        return currentContribs;
    }

}
