/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core.service;

import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for resource elements. Does not handle merge.
 *
 * @since 7.3
 */
public class ResourceRegistry extends SimpleContributionRegistry<Resource> {

    @Override
    public String getContributionId(Resource contrib) {
        return contrib.getName();
    }

    // custom API

    public Resource getResource(String id) {
        return getCurrentContribution(id);
    }

}
