/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping;

import java.util.Map;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 *
 * @since 5.6
 *
 */
public class PropertiesMappingContributionRegistry extends
        SimpleContributionRegistry<PropertiesMappingDescriptor> {

    @Override
    public String getContributionId(PropertiesMappingDescriptor contrib) {
        return contrib.getName();
    }

    public Map<String, String> getMappingProperties(String mappingName) {
        return currentContribs.get(mappingName).getProperties();
    }

}
