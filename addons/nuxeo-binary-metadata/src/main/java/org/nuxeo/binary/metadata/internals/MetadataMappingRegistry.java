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
