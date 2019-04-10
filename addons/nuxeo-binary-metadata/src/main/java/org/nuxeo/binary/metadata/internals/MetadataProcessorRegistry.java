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

import org.nuxeo.binary.metadata.api.BinaryMetadataProcessor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for {@link org.nuxeo.binary.metadata.internals.MetadataProcessorDescriptor} descriptors.
 *
 * @since 7.1
 */
public class MetadataProcessorRegistry extends SimpleContributionRegistry<MetadataProcessorDescriptor> {

    @Override
    public String getContributionId(MetadataProcessorDescriptor metadataProcessorDescriptor) {
        return metadataProcessorDescriptor.getId();
    }

    /**
     * Not supported.
     */
    @Override
    public void merge(MetadataProcessorDescriptor metadataProcessorDescriptor,
            MetadataProcessorDescriptor metadataProcessorDescriptor2) {
        throw new UnsupportedOperationException();
    }

    public BinaryMetadataProcessor getProcessor(String processorId) {
        return currentContribs.get(processorId).processor;
    }
}
