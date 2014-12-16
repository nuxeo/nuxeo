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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api.service;

import org.nuxeo.binary.metadata.contribution.MetadataMappingDescriptor;
import org.nuxeo.binary.metadata.contribution.MetadataProcessorDescriptor;
import org.nuxeo.binary.metadata.contribution.MetadataRuleDescriptor;

/**
 * Binary metadata service which registers all binary metadata contributions.
 *
 * @since 7.1
 */
public interface BinaryMetadataRegistryService {

    public void addMappingContribution(MetadataMappingDescriptor contribution);

    public void addRuleContribution(MetadataRuleDescriptor contribution);

    public void addProcessorContribution(MetadataProcessorDescriptor contribution);

    public void removeMappingContribution(MetadataMappingDescriptor contribution);

    public void removeRuleContribution(MetadataRuleDescriptor contribution);

    public void removeProcessorContribution(MetadataProcessorDescriptor contribution);

}
