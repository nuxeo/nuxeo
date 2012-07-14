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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.registries;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.routing.api.RouteModelResourceType;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for route templates
 *
 * @since 5.6
 */
public class RouteTemplateResourceRegistry extends
        SimpleContributionRegistry<RouteModelResourceType> {

    public List<URL> getRouteModelTemplateResources() throws ClientException {
        List<URL> urls = new ArrayList<URL>();
        for (RouteModelResourceType res : currentContribs.values()) {
            urls.add(res.getUrl());
        }
        return urls;
    }

    @Override
    public String getContributionId(RouteModelResourceType contrib) {
        return contrib.getId();
    }

    public RouteModelResourceType getResource(String id) {
        return getCurrentContribution(id);
    }

}
